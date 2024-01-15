package no.fdk.concept_catalog.service

import com.fasterxml.jackson.databind.ObjectMapper
import no.fdk.concept_catalog.configuration.ApplicationProperties
import no.fdk.concept_catalog.elastic.CurrentConceptRepository
import no.fdk.concept_catalog.model.*
import no.fdk.concept_catalog.repository.ConceptRepository
import no.fdk.concept_catalog.validation.isValid
import no.fdk.concept_catalog.validation.validateSchema
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

    fun deleteConcept(concept: BegrepDBO) {
        if (concept.id == concept.originaltBegrep) {
            currentConceptRepository.delete(CurrentConcept(concept))
        }
        conceptRepository.delete(concept)
    }

    fun getConceptById(id: String): Begrep? =
        conceptRepository.findByIdOrNull(id)?.withHighestVersionDTO()

    fun getConceptDBO(id: String): BegrepDBO? =
        conceptRepository.findByIdOrNull(id)

    fun createConcept(concept: Begrep, user: User, jwt: Jwt): Begrep {
        val newDefaultConcept: BegrepDBO = createNewConcept(concept.ansvarligVirksomhet, user)
            .also { publishNewCollectionIfFirstSavedConcept(concept.ansvarligVirksomhet.id) }
            .updateLastChangedAndByWhom(user)

        val newConcept: BegrepDBO = newDefaultConcept.addUpdatableFieldsFromDTO(concept)

        if(!newConcept.validateMinimumVersion()) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid version ${newConcept.versjonsnr}. Version must be minimum 0.1.0"
            )
        }
        val validation = newConcept.validateSchema()
        if (!validation.isValid) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, validation.results().toString())
        }

        val operations = createPatchOperations(newDefaultConcept, newConcept, mapper)

        return saveConceptsAndUpdateHistory(mapOf(Pair(newConcept, operations)), user, jwt).first()
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
        val newRevision = concept.createNewRevision(user).updateLastChangedAndByWhom(user)
        val newWithUpdatedValues = newRevision.addUpdatableFieldsFromDTO(revisionValues)

        if(!newWithUpdatedValues.validateVersionUpgrade(concept.versjonsnr)) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid version ${newWithUpdatedValues.versjonsnr}. Version must be greater than ${concept.versjonsnr}"
            )
        }

        val operations = createPatchOperations(newRevision, newWithUpdatedValues, mapper)

        return saveConceptsAndUpdateHistory(mapOf(Pair(newWithUpdatedValues, operations)), user, jwt).first()
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
                if(!it.key.validateMinimumVersion()) {
                    invalidVersionsList.add(it.key)
                }

                val validation = it.key.validateSchema()
                if (!validation.isValid) {
                    validationResultsMap[it.key] = validation.results()
                }
            }

        if (validationResultsMap.isNotEmpty() || invalidVersionsList.isNotEmpty()) {
            throw ResponseStatusException(
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
        }

        saveConceptsAndUpdateHistory(newConceptsAndOperations, user, jwt)
    }

    fun updateConcept(concept: BegrepDBO, operations: List<JsonPatchOperation>, user: User, jwt: Jwt): Begrep {
        val patched = patchOriginal(concept.copy(endringslogelement = null), operations, mapper)
            .copy(
                id = concept.id,
                originaltBegrep = concept.originaltBegrep,
                ansvarligVirksomhet = concept.ansvarligVirksomhet
            )
            .updateLastChangedAndByWhom(user)

        val validation = patched.validateSchema()

        when {
            concept.erPublisert -> throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Unable to patch published concepts"
            )
            !validation.isValid -> throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                validation.results().toString()
            )
            isPublishedAndNotValid(patched.withHighestVersionDTO()) -> {
                val badRequestException = ResponseStatusException(HttpStatus.BAD_REQUEST)
                logger.error(
                    "Concept ${patched.id} has not passed validation for published concepts and has not been saved.",
                    badRequestException
                )
                throw badRequestException
            }
            patched.erPublisert || patched.publiseringsTidspunkt != null -> throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Unable to publish concepts as part of normal update"
            )
        }
        return saveConceptsAndUpdateHistory(mapOf(Pair(patched, operations)), user, jwt).first()
    }

    private fun saveConceptsAndUpdateHistory(
        conceptsAndOperations: Map<BegrepDBO, List<JsonPatchOperation>>,
        user: User,
        jwt: Jwt
    ): List<Begrep> {
        val locations = conceptsAndOperations.map { historyService.updateHistory(it.key, it.value, user, jwt) }
        try {
            conceptsAndOperations.keys
                .filter { it.id == it.originaltBegrep }
                .map { CurrentConcept(it) }
                .run { currentConceptRepository.saveAll(this) }
            return conceptRepository.saveAll(conceptsAndOperations.keys).map { it.withHighestVersionDTO() }
        } catch (ex: Exception) {
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
        if (status == null) conceptRepository.getBegrepByAnsvarligVirksomhetId(orgNr).map { it.withHighestVersionDTO() }
        else conceptRepository.getBegrepByAnsvarligVirksomhetIdAndStatus(orgNr, status).map { it.withHighestVersionDTO() }

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
                ?.let { it.toDTO(it.versjonsnr, it.id, findIdOfUnpublishedRevision(it)) }
        }

    fun getLastPublishedForOrganization(orgNr: String): List<Begrep> =
        conceptRepository.getBegrepByAnsvarligVirksomhetId(orgNr)
            .filter { it.erPublisert }
            .sortedByDescending {concept -> concept.versjonsnr }
            .distinctBy {concept -> concept.originaltBegrep }
            .map { it.toDTO(it.versjonsnr, it.id, findIdOfUnpublishedRevision(it)) }

    fun getLatestVersion(originalId: String): BegrepDBO? =
        conceptRepository.getByOriginaltBegrep(originalId)
            .maxByOrNull { it.versjonsnr }

    fun searchConcepts(orgNumber: String, search: SearchOperation): Paginated {
        val hits = conceptSearchService.searchCurrentConcepts(orgNumber, search)

        return hits.map { it.content }
            .map { it.toDBO() }
            .map { it.withHighestVersionDTO() }
            .toList()
            .asPaginatedWrapDTO(hits.totalHits, search.pagination)
    }

    fun suggestConcepts(orgNumber: String, published: Boolean?, query: String): List<Suggestion> =
        conceptSearchService.suggestConcepts(orgNumber, published, query)
                .map { it.content }
                .map { it.toSuggestion() }
                .toList()

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

    private fun BegrepDBO.withHighestVersionDTO(): Begrep =
        getLastPublished(originaltBegrep)
            ?.let { toDTO(it.versjonsnr, it.id, findIdOfUnpublishedRevision(this)) }
            ?: toDTO(null, null, findIdOfUnpublishedRevision(this))

    fun findRevisions(concept: BegrepDBO): List<Begrep> =
        conceptRepository.getByOriginaltBegrep(concept.originaltBegrep)
            .map { it.withHighestVersionDTO() }

    fun publish(concept: BegrepDBO): Begrep {
        val published = concept.copy(
            erPublisert = true,
            versjonsnr = getVersionOrMinimum(concept),
            publiseringsTidspunkt = Instant.now()
        )

        when {
            concept.erPublisert -> throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Unable to publish already published concepts"
            )
            isPublishedAndNotValid(published.withHighestVersionDTO()) -> {
                val badRequestException = ResponseStatusException(HttpStatus.BAD_REQUEST)
                logger.error(
                    "Concept ${concept.id} has not passed validation and has not been published.",
                    badRequestException
                )
                throw badRequestException
            }
        }

        conceptPublisher.send(concept.ansvarligVirksomhet.id)

        currentConceptRepository.save(CurrentConcept(published))
        return conceptRepository.save(published)
            .withHighestVersionDTO()
    }

    private fun getVersionOrMinimum(concept: BegrepDBO): SemVer {
        return if (concept.versjonsnr.major == 0) {
            SemVer(major = 1, minor = 0, patch = 0)
        } else {
            concept.versjonsnr
        }
    }

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
            versjonsnr < SemVer(0,1,0) -> false
            else -> true
        }
    fun BegrepDBO.validateVersionUpgrade(currentVersion: SemVer?): Boolean =
        when {
            currentVersion != null && versjonsnr <= currentVersion -> false
            else -> true
        }
}
