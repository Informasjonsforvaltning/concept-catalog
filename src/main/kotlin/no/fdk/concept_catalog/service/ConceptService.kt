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
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import kotlin.math.ceil
import kotlin.math.roundToLong

private val logger = LoggerFactory.getLogger(ConceptService::class.java)

@Service
class ConceptService(
    private val conceptRepository: ConceptRepository,
    private val conceptSearchService: ConceptSearchService,
    private val currentConceptRepository: CurrentConceptRepository,
    private val applicationProperties: ApplicationProperties,
    private val conceptPublisher: ConceptPublisher,
    private val historyService: HistoryService,
    private val mapper: ObjectMapper
) {

    fun updateCurrentConceptForOriginalId(originalId: String) {
        val allVersions = conceptRepository.findByOriginaltBegrep(originalId).map { it.toDBO() }
        val newCurrent = allVersions.maxByOrNull { it.versjonsnr }

        if (newCurrent == null && currentConceptRepository.existsById(originalId)) {
            currentConceptRepository.deleteById(originalId)
        } else if (newCurrent != null) {
            val latestArchivedId = allVersions.filter { it.isArchived == true }
                .maxByOrNull { it.versjonsnr }
                ?.id
            currentConceptRepository.save(CurrentConcept(newCurrent, latestArchivedId))
        }
    }

    @Transactional
    fun deleteConcept(concept: BegrepDBO) {
        conceptRepository.deleteById(concept.id)
            .also { logger.debug("deleted concept ${concept.id}") }

        updateCurrentConceptForOriginalId(concept.originaltBegrep)
    }

    fun getConceptById(id: String): Begrep? =
        conceptRepository.findById(id).orElse(null)?.toDBO()?.toDTO()

    fun getConceptDBO(id: String): BegrepDBO? =
        conceptRepository.findById(id).orElse(null)?.toDBO()

    @Transactional
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

    @Transactional
    fun createRevisionOfConcept(revisionValues: Begrep, concept: BegrepDBO, user: User, jwt: Jwt): Begrep {
        val newRevision = concept.createNewRevision().updateLastChangedAndByWhom(user)
        val operations =
            createPatchOperations(newRevision, newRevision.addUpdatableFieldsFromDTO(revisionValues), mapper)
        return createRevisionOfConcept(operations, concept, user, jwt)
    }

    @Transactional
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

    @Transactional
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

    @Transactional
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
            concept.isArchived == true -> {
                val badRequest = ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to patch archived concepts")
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

            patched.erPublisert == true || patched.publiseringsTidspunkt != null -> {
                val badRequest = ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unable to publish concepts as part of normal update"
                )
                logger.error("aborting update of ${concept.id}", badRequest)
                throw badRequest
            }

            patched.isArchived == true -> {
                val badRequest = ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unable to archive concepts as part of normal update"
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
            val entities = conceptsAndOperations.keys.map { it.toEntity() }
            return conceptRepository.saveAll(entities)
                .map { it.toDBO() }
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
            concept.erPublisert != true -> false
            concept.versjonsnr == null -> true
            !concept.isValid() -> true
            published?.versjonsnr == null -> false
            else -> published.versjonsnr >= concept.versjonsnr
        }
    }

    fun getConceptsForOrganization(orgNr: String, status: Status?): List<Begrep> =
        if (status == null) conceptRepository.findByAnsvarligVirksomhetId(orgNr).map { it.toDBO().toDTO() }
        else conceptRepository.findByAnsvarligVirksomhetIdAndStatus(orgNr, status.value).map { it.toDBO().toDTO() }

    fun getAllPublisherIds(): List<String> =
        conceptRepository.findDistinctAnsvarligVirksomhetIds()

    fun getLastPublished(originaltBegrep: String?): Begrep? =
        if (originaltBegrep == null) null
        else {
            conceptRepository.findByOriginaltBegrep(originaltBegrep)
                .map { it.toDBO() }
                .filter { it.erPublisert == true }
                .maxByOrNull { concept -> concept.versjonsnr }
                ?.toDTO()
        }

    fun getLastPublishedForOrganization(orgNr: String): List<Begrep> =
        conceptRepository.findByAnsvarligVirksomhetId(orgNr)
            .map { it.toDBO() }
            .filter { it.erPublisert == true }
            .sortedByDescending { concept -> concept.versjonsnr }
            .distinctBy { concept -> concept.originaltBegrep }
            .map { it.toDTO() }

    fun getLatestVersion(originalId: String): BegrepDBO? =
        conceptRepository.findByOriginaltBegrep(originalId)
            .map { it.toDBO() }
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

    fun publishNewCollectionIfFirstSavedConcept(publisherId: String?) {
        val begrepCount = publisherId?.let {
            conceptRepository.countByAnsvarligVirksomhetId(it)
        }

        if (begrepCount == 0L) {
            logger.info("Adding first entry for $publisherId in harvest admin...")
            conceptPublisher.createNewDataSource(publisherId)
        }
    }

    fun findRevisions(concept: BegrepDBO): List<Begrep> =
        conceptRepository.findByOriginaltBegrep(concept.originaltBegrep)
            .map { it.toDBO().toDTO() }

    @Transactional
    fun publish(concept: BegrepDBO): Begrep {
        val published = concept.copy(
            erPublisert = true,
            isArchived = true,
            versjonsnr = getVersionOrMinimum(concept),
            publiseringsTidspunkt = Instant.now()
        )

        when {
            concept.erPublisert == true -> {
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

        conceptPublisher.triggerHarvest(concept.ansvarligVirksomhet.id)

        val savedDBO = conceptRepository.save(published.toEntity()).toDBO()
        updateRelationsToNonInternal(savedDBO)
        updateCurrentConceptForOriginalId(savedDBO.originaltBegrep)
        return savedDBO.toDTO()
    }

    private fun updateRelationsToNonInternal(concept: BegrepDBO) {
        val collectionURI = getCollectionUri(applicationProperties.collectionBaseUri, concept.ansvarligVirksomhet.id)
        val conceptURI = getConceptUri(collectionURI, concept.originaltBegrep)
        conceptRepository.findByAnsvarligVirksomhetId(concept.ansvarligVirksomhet.id)
            .map { it.toDBO() }
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
            .map { it.toEntity() }
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
        conceptRepository.findByOriginaltBegrep(originaltBegrep)
            .map { it.toDBO() }
            .maxByOrNull { it.versjonsnr }
            ?.let { it.id == id }
            ?: true

    fun findIdOfUnarchivedRevision(concept: BegrepDBO): String? =
        when {
            concept.isArchived != true -> null
            else -> conceptRepository.findByOriginaltBegrepAndIsArchived(
                originaltBegrep = concept.originaltBegrep,
                isArchived = false
            ).map { it.toDBO() }
                .maxByOrNull { it.opprettet?.epochSecond ?: 0 }?.id
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
