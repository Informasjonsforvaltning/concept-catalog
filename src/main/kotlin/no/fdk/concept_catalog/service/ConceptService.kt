package no.fdk.concept_catalog.service

import com.fasterxml.jackson.databind.ObjectMapper
import no.fdk.concept_catalog.configuration.ApplicationProperties
import no.fdk.concept_catalog.elastic.CurrentConceptRepository
import no.fdk.concept_catalog.model.*
import no.fdk.concept_catalog.repository.ConceptRepository
import no.fdk.concept_catalog.validation.isValid
import no.fdk.concept_catalog.validation.validateSchema
import org.apache.jena.riot.Lang
import org.openapi4j.core.validation.ValidationResults
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.util.UriComponentsBuilder
import java.time.Instant
import kotlin.math.ceil
import kotlin.math.roundToLong

private val logger = LoggerFactory.getLogger(ConceptService::class.java)

@Service
class ConceptService(
    private val conceptRepository: ConceptRepository,
    private val conceptSearchService: ConceptSearchService,
    private val currentConceptRepository: CurrentConceptRepository,
    private val mongoOperations: MongoOperations,
    private val applicationProperties: ApplicationProperties,
    private val conceptPublisher: ConceptPublisher,
    private val historyService: HistoryService,
    private val mapper: ObjectMapper
) {

    fun updateCurrentConceptForOriginalId(originalId: String) {
        val allVersions = conceptRepository.getByOriginaltBegrep(originalId)
        val newCurrent = allVersions.maxByOrNull { it.versjonsnr }

        if (newCurrent == null && currentConceptRepository.existsById(originalId)) {
            currentConceptRepository.deleteById(originalId)
        } else if (newCurrent != null) {
            val latestPublishedId = allVersions.filter { it.erPublisert }
                .maxByOrNull { it.versjonsnr }
                ?.id
            currentConceptRepository.save(CurrentConcept(newCurrent, latestPublishedId))
        }
    }

    fun updateCurrentConceptsForExtractions(extractions: List<ConceptExtraction>) {
        extractions
            .mapNotNull { it.concept.originaltBegrep }
            .distinct()
            .onEach { updateCurrentConceptForOriginalId(it) }

    }

    fun deleteConcept(concept: BegrepDBO) {
        conceptRepository.delete(concept)
            .also { logger.debug("deleted concept ${concept.id}") }

        updateCurrentConceptForOriginalId(concept.originaltBegrep)
    }

    fun getConceptById(id: String): Begrep? =
        conceptRepository.findByIdOrNull(id)?.toDTO()

    fun getConceptDBO(id: String): BegrepDBO? =
        conceptRepository.findByIdOrNull(id)

    fun createConcept(concept: Begrep, user: User, jwt: Jwt): Begrep {
        val newDefaultConcept: BegrepDBO = createNewConcept(concept.ansvarligVirksomhet, user)
            .also { publishNewCollectionIfFirstSavedConcept(concept.ansvarligVirksomhet.id) }
            .updateLastChangedAndByWhom(user)

        val newConcept: BegrepDBO = newDefaultConcept.addUpdatableFieldsFromDTO(concept)

        if (!newConcept.validateMinimumVersion()) {
            val badRequest = ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid version ${newConcept.versjonsnr}. Version must be minimum 0.1.0"
            )
            logger.error("aborting create", badRequest)
            throw badRequest
        }
        val validation = newConcept.validateSchema()
        if (!validation.isValid) {
            val badRequest = ResponseStatusException(HttpStatus.BAD_REQUEST, validation.results().toString())
            logger.error("invalid concept, aborting create", badRequest)
            throw badRequest
        }

        val operations = createPatchOperations(newDefaultConcept, newConcept, mapper)

        return saveConceptsAndUpdateHistory(mapOf(Pair(newConcept, operations)), user, jwt)
            .first()
            .also { logger.debug("new concept ${it.id} successfully created") }
    }

    fun getAllCollections(): List<Begrepssamling> =
        getAllPublisherIds()
            .map { getCollectionForPublisher(it) }

    fun getCollectionsForOrganizations(publishers: Set<String>): List<Begrepssamling> =
        publishers
            .map { getCollectionForPublisher(it) }
            .filter { it.antallBegrep > 0 }

    private fun getCollectionForPublisher(publisherId: String): Begrepssamling =
        Begrepssamling(
            id = publisherId,
            antallBegrep = getConceptsForOrganization(publisherId, null)
                .distinctBy { it.originaltBegrep }
                .size
        )

    fun createRevisionOfConcept(revisionValues: Begrep, concept: BegrepDBO, user: User, jwt: Jwt): Begrep {
        val newRevision = concept.createNewRevision().updateLastChangedAndByWhom(user)
        val operations =
            createPatchOperations(newRevision, newRevision.addUpdatableFieldsFromDTO(revisionValues), mapper)
        return createRevisionOfConcept(operations, concept, user, jwt)
    }

    fun createRevisionOfConcept(
        operations: List<JsonPatchOperation>,
        concept: BegrepDBO,
        user: User,
        jwt: Jwt
    ): Begrep {
        if (!concept.isHighestVersion()) {
            val badRequest = ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid revision target, ${concept.id} is not highest version of the concept"
            )
            logger.error("revision of ${concept.id} aborted", badRequest)
            throw badRequest
        }

        val newWithUpdatedValues = patchAndValidateConcept(
            concept.createNewRevision().updateLastChangedAndByWhom(user),
            operations,
            user
        )

        if (!newWithUpdatedValues.validateVersionUpgrade(concept.versjonsnr)) {
            val badRequest = ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid version ${newWithUpdatedValues.versjonsnr}. Version must be greater than ${concept.versjonsnr}"
            )
            logger.error("revision of ${concept.id} aborted", badRequest)
            throw badRequest
        }

        return saveConceptsAndUpdateHistory(mapOf(Pair(newWithUpdatedValues, operations)), user, jwt)
            .first()
            .also { logger.debug("new revision ${it.id} successfully created") }
    }

    fun createConcepts(concepts: List<Begrep>, user: User, jwt: Jwt) {
        concepts.map { it.ansvarligVirksomhet.id }
            .distinct()
            .forEach { publishNewCollectionIfFirstSavedConcept(it) }

        val invalidVersionsList = mutableListOf<BegrepDBO>()
        val validationResultsMap = mutableMapOf<BegrepDBO, ValidationResults>()
        val newConceptsAndOperations = concepts
            .map { it to createNewConcept(it.ansvarligVirksomhet, user).updateLastChangedAndByWhom(user) }
            .associate { it.second.addUpdatableFieldsFromDTO(it.first) to it.second }
            .mapValues { createPatchOperations(it.key, it.value, mapper) }
            .onEach {
                if (!it.key.validateMinimumVersion()) {
                    invalidVersionsList.add(it.key)
                }

                val validation = it.key.validateSchema()
                if (!validation.isValid) {
                    validationResultsMap[it.key] = validation.results()
                }
            }

        if (validationResultsMap.isNotEmpty() || invalidVersionsList.isNotEmpty()) {
            val badRequest = ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                validationResultsMap.entries.mapIndexed { index, entry ->
                    "Concept ${index}"
                        .plus(entry.key.anbefaltTerm?.navn?.let { " - $it" } ?: "")
                        .plus("\n")
                        .plus(entry.value.toString())
                        .plus("\n\n")
                }.joinToString("\n") +
                        invalidVersionsList.mapIndexed { index, entry ->
                            "Concept ${index}"
                                .plus(entry.anbefaltTerm?.navn?.let { " - $it" } ?: "")
                                .plus("\n")
                                .plus("Invalid version ${entry.versjonsnr}. Version must be minimum 0.1.0")
                                .plus("\n\n")
                        }.joinToString("\n")
            )
            logger.error("validation of some concepts failed, aborting create", badRequest)
            throw badRequest
        }

        saveConceptsAndUpdateHistory(newConceptsAndOperations, user, jwt)
            .also { logger.debug("created ${it.size} new concepts for ${it.first().ansvarligVirksomhet.id}") }
    }

    fun createConcepts(concepts: String, lang: Lang, user: User, jwt: Jwt) {
        /*
        TODO: Read, convert and process begreper
         */
    }

    fun updateConcept(concept: BegrepDBO, operations: List<JsonPatchOperation>, user: User, jwt: Jwt): Begrep {
        val patched = patchAndValidateConcept(concept, operations, user)
        return saveConceptsAndUpdateHistory(mapOf(Pair(patched, operations)), user, jwt)
            .first()
            .also { logger.debug("concept ${it.id} successfully updated") }
    }

    private fun patchAndValidateConcept(
        concept: BegrepDBO,
        operations: List<JsonPatchOperation>,
        user: User
    ): BegrepDBO {
        val patched = try {
            concept
                .addUpdatableFieldsFromDTO(
                    patchOriginal(
                        concept.toDTO(),
                        operations,
                        mapper
                    )
                )
                .updateLastChangedAndByWhom(user)
        } catch (ex: Exception) {
            logger.error("failed to patch concept ${concept.id}", ex)
            throw ex
        }

        val validation = patched.validateSchema()

        when {
            concept.erPublisert -> {
                val badRequest = ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to patch published concepts")
                logger.error("aborting update of ${concept.id}", badRequest)
                throw badRequest
            }

            !validation.isValid -> {
                val badRequest = ResponseStatusException(HttpStatus.BAD_REQUEST, validation.results().toString())
                logger.error("aborting update of ${concept.id}, update failed validation", badRequest)
                throw badRequest
            }

            isPublishedAndNotValid(patched.toDTO()) -> {
                val badRequestException = ResponseStatusException(HttpStatus.BAD_REQUEST)
                logger.error(
                    "Concept ${patched.id} has not passed validation for published concepts and has not been saved.",
                    badRequestException
                )
                throw badRequestException
            }

            patched.erPublisert || patched.publiseringsTidspunkt != null -> {
                val badRequest = ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unable to publish concepts as part of normal update"
                )
                logger.error("aborting update of ${concept.id}", badRequest)
                throw badRequest
            }
        }

        return patched
    }

    private fun saveConceptsAndUpdateHistory(
        conceptsAndOperations: Map<BegrepDBO, List<JsonPatchOperation>>,
        user: User,
        jwt: Jwt
    ): List<Begrep> {
        val locations = conceptsAndOperations.map { historyService.updateHistory(it.key, it.value, user, jwt) }
        try {
            return conceptRepository.saveAll(conceptsAndOperations.keys)
                .onEach { updateCurrentConceptForOriginalId(it.originaltBegrep) }
                .map { it.toDTO() }
        } catch (ex: Exception) {
            logger.error("save failed, removing history update", ex)
            locations.filterNotNull().forEach { historyService.removeHistoryUpdate(it, jwt) }
            throw ex
        }
    }

    fun isPublishedAndNotValid(concept: Begrep): Boolean {
        val published = getLastPublished(concept.originaltBegrep)
        return when {
            !concept.erPublisert -> false
            concept.versjonsnr == null -> true
            !concept.isValid() -> true
            published?.versjonsnr == null -> false
            else -> published.versjonsnr >= concept.versjonsnr
        }
    }

    fun getConceptsForOrganization(orgNr: String, status: Status?): List<Begrep> =
        if (status == null) conceptRepository.getBegrepByAnsvarligVirksomhetId(orgNr).map { it.toDTO() }
        else conceptRepository.getBegrepByAnsvarligVirksomhetIdAndStatus(orgNr, status).map { it.toDTO() }

    fun getAllPublisherIds(): List<String> {
        return mongoOperations
            .query(Begrep::class.java)
            .distinct("ansvarligVirksomhet.id")
            .`as`(String::class.java)
            .all()
    }

    fun getLastPublished(originaltBegrep: String?): Begrep? =
        if (originaltBegrep == null) null
        else {
            conceptRepository.getByOriginaltBegrep(originaltBegrep)
                .filter { it.erPublisert }
                .maxByOrNull { concept -> concept.versjonsnr }
                ?.toDTO()
        }

    fun getLastPublishedForOrganization(orgNr: String): List<Begrep> =
        conceptRepository.getBegrepByAnsvarligVirksomhetId(orgNr)
            .filter { it.erPublisert }
            .sortedByDescending { concept -> concept.versjonsnr }
            .distinctBy { concept -> concept.originaltBegrep }
            .map { it.toDTO() }

    fun getLatestVersion(originalId: String): BegrepDBO? =
        conceptRepository.getByOriginaltBegrep(originalId)
            .maxByOrNull { it.versjonsnr }

    fun searchConcepts(orgNumber: String, search: SearchOperation): Paginated {
        val hits = conceptSearchService.searchCurrentConcepts(orgNumber, search)
        return hits.map { it.content }
            .toList()
            .map { it.toDTO() }
            .asPaginatedWrapDTO(hits.totalHits, search.pagination)
    }

    fun suggestConcepts(orgNumber: String, published: Boolean?, query: String): List<Suggestion> =
        conceptSearchService.suggestConcepts(orgNumber, published, query)
            .map { it.content }
            .map { it.toSuggestion() }
            .toList()

    fun countCurrentConcepts(orgNumber: String): Long =
        conceptSearchService.countCurrentConcepts(orgNumber)

    private fun CurrentConcept.toSuggestion(): Suggestion =
        Suggestion(
            id = idOfThisVersion,
            originaltBegrep = originaltBegrep,
            erPublisert = erPublisert,
            anbefaltTerm = anbefaltTerm,
            definisjon = definisjon?.copy(kildebeskrivelse = null)
        )

    private fun List<Begrep>.asPaginatedWrapDTO(totalHits: Long, pagination: Pagination): Paginated {
        return Paginated(
            hits = this,
            page = PageMeta(
                currentPage = pagination.getPage(),
                size = size,
                totalElements = totalHits,
                totalPages = ceil(totalHits.toDouble() / pagination.getSize()).roundToLong()
            )
        )
    }

    private fun publishNewCollectionIfFirstSavedConcept(publisherId: String?) {
        val begrepCount = publisherId?.let {
            conceptRepository.countBegrepByAnsvarligVirksomhetId(it)
        }

        if (begrepCount == 0L) {
            logger.info("Adding first entry for $publisherId in harvest admin...")
            val harvestUrl = UriComponentsBuilder
                .fromUriString(applicationProperties.collectionBaseUri)
                .replacePath("/collections/$publisherId")
                .build().toUriString()
            conceptPublisher.sendNewDataSource(publisherId, harvestUrl)
        }
    }

    fun findRevisions(concept: BegrepDBO): List<Begrep> =
        conceptRepository.getByOriginaltBegrep(concept.originaltBegrep)
            .map { it.toDTO() }

    fun publish(concept: BegrepDBO): Begrep {
        val published = concept.copy(
            erPublisert = true,
            versjonsnr = getVersionOrMinimum(concept),
            publiseringsTidspunkt = Instant.now()
        )

        when {
            concept.erPublisert -> {
                val badRequest =
                    ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to publish already published concepts")
                logger.error("aborting publish of ${concept.id}", badRequest)
                throw badRequest
            }

            isPublishedAndNotValid(published.toDTO()) -> {
                val badRequestException = ResponseStatusException(HttpStatus.BAD_REQUEST)
                logger.error(
                    "Concept ${concept.id} has not passed validation and has not been published.",
                    badRequestException
                )
                throw badRequestException
            }
        }

        conceptPublisher.send(concept.ansvarligVirksomhet.id)

        return conceptRepository.save(published)
            .also { updateRelationsToNonInternal(it) }
            .toDTO()
    }

    private fun updateRelationsToNonInternal(concept: BegrepDBO) {
        val collectionURI = getCollectionUri(applicationProperties.collectionBaseUri, concept.ansvarligVirksomhet.id)
        val conceptURI = getConceptUri(collectionURI, concept.originaltBegrep)
        conceptRepository.getBegrepByAnsvarligVirksomhetId(concept.ansvarligVirksomhet.id)
            .filter {
                it.internSeOgså?.contains(concept.id) == true ||
                        it.internErstattesAv?.contains(concept.id) == true ||
                        it.internBegrepsRelasjon?.map { relation -> relation.relasjon }?.contains(concept.id) == true
            }
            .map {
                if (it.internSeOgså?.contains(concept.id) == true) {
                    it.copy(
                        seOgså = it.seOgså?.plus(conceptURI) ?: listOf(conceptURI),
                        internSeOgså = it.internSeOgså.minus(concept.id)
                    )
                } else {
                    it
                }
            }
            .map {
                if (it.internErstattesAv?.contains(concept.id) == true) {
                    it.copy(
                        erstattesAv = it.erstattesAv?.plus(conceptURI) ?: listOf(conceptURI),
                        internErstattesAv = it.internErstattesAv.minus(concept.id)
                    )
                } else {
                    it
                }
            }
            .map {
                val external = mutableListOf<BegrepsRelasjon>()
                val internal = mutableListOf<BegrepsRelasjon>()
                it.internBegrepsRelasjon?.forEach { relation ->
                    if (relation.relatertBegrep == concept.id) external.add(relation.copy(relatertBegrep = conceptURI))
                    else internal.add(relation)
                }

                if (internal.size != (it.internBegrepsRelasjon ?: 0)) {
                    it.copy(
                        begrepsRelasjon = it.begrepsRelasjon?.plus(external) ?: external,
                        internBegrepsRelasjon = internal
                    )
                } else {
                    it
                }
            }
            .run { conceptRepository.saveAll(this) }
    }

    private fun getVersionOrMinimum(concept: BegrepDBO): SemVer {
        return if (concept.versjonsnr.major == 0) {
            SemVer(major = 1, minor = 0, patch = 0)
        } else {
            concept.versjonsnr
        }
    }

    private fun BegrepDBO.isHighestVersion(): Boolean =
        conceptRepository.getByOriginaltBegrep(originaltBegrep)
            .maxByOrNull { it.versjonsnr }
            ?.let { it.id == id }
            ?: true

    fun findIdOfUnpublishedRevision(concept: BegrepDBO): String? =
        when {
            !concept.erPublisert -> null
            else -> conceptRepository.getByOriginaltBegrepAndErPublisert(
                originaltBegrep = concept.originaltBegrep,
                erPublisert = false
            ).maxByOrNull { it.opprettet?.epochSecond ?: 0 }?.id
        }

    fun BegrepDBO.validateMinimumVersion(): Boolean =
        when {
            versjonsnr < SemVer(0, 1, 0) -> false
            else -> true
        }

    fun BegrepDBO.validateVersionUpgrade(currentVersion: SemVer?): Boolean =
        when {
            currentVersion != null && versjonsnr <= currentVersion -> false
            else -> true
        }
}
