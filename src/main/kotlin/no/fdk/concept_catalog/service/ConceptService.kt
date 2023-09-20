package no.fdk.concept_catalog.service

import com.fasterxml.jackson.databind.ObjectMapper
import no.fdk.concept_catalog.configuration.ApplicationProperties
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
import org.springframework.web.client.HttpClientErrorException.BadRequest
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.util.UriComponentsBuilder
import java.time.Instant
import kotlin.math.ceil
import kotlin.math.roundToInt

private val logger = LoggerFactory.getLogger(ConceptService::class.java)

@Service
class ConceptService(
    private val conceptRepository: ConceptRepository,
    private val conceptSearchService: ConceptSearchService,
    private val mongoOperations: MongoOperations,
    private val applicationProperties: ApplicationProperties,
    private val conceptPublisher: ConceptPublisher,
    private val historyService: HistoryService,
    private val mapper: ObjectMapper
) {

    fun deleteConcept(concept: BegrepDBO) =
        conceptRepository.delete(concept)

    fun getConceptById(id: String): Begrep? =
        conceptRepository.findByIdOrNull(id)?.withHighestVersionDTO()

    fun getConceptDBO(id: String): BegrepDBO? =
        conceptRepository.findByIdOrNull(id)

    fun createConcept(concept: Begrep, user: User, jwt: Jwt): Begrep {
        val newDefaultConcept: BegrepDBO = createNewConcept(concept.ansvarligVirksomhet, user)
            .also { publishNewCollectionIfFirstSavedConcept(concept.ansvarligVirksomhet.id) }
            .updateLastChangedAndByWhom(user)

        val newConcept: BegrepDBO = newDefaultConcept.addUpdatableFieldsFromDTO(concept)

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
        val operations = createPatchOperations(newRevision, newWithUpdatedValues, mapper)

        return saveConceptsAndUpdateHistory(mapOf(Pair(newWithUpdatedValues, operations)), user, jwt).first()
    }

    fun createConcepts(concepts: List<Begrep>, user: User, jwt: Jwt) {
        concepts.map { it.ansvarligVirksomhet.id }
            .distinct()
            .forEach { publishNewCollectionIfFirstSavedConcept(it) }

        val validationResultsMap = mutableMapOf<BegrepDBO, ValidationResults>()
        val newConceptsAndOperations = concepts
            .map { it to createNewConcept(it.ansvarligVirksomhet, user).updateLastChangedAndByWhom(user) }
            .associate { it.second.addUpdatableFieldsFromDTO(it.first) to it.second }
            .mapValues { createPatchOperations(it.key, it.value, mapper) }
            .onEach {
                val validation = it.key.validateSchema()
                if (!validation.isValid) {
                    validationResultsMap[it.key] = validation.results()
                }
            }

        if (validationResultsMap.isNotEmpty()) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                validationResultsMap.entries.mapIndexed { index, entry ->
                    "Begrep ${index}"
                        .plus(entry.key.anbefaltTerm?.navn?.let { " - $it" } ?: "")
                        .plus("\n")
                        .plus(entry.value.toString())
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

    private fun saveConceptsAndUpdateHistory(conceptsAndOperations: Map<BegrepDBO, List<JsonPatchOperation>>, user: User, jwt: Jwt) =
        if (applicationProperties.namespace == "staging") {
            val locations = conceptsAndOperations.map { historyService.updateHistory(it.key, it.value, user, jwt) }
            try {
                conceptRepository.saveAll(conceptsAndOperations.keys).map { it.withHighestVersionDTO() }
            } catch (ex: Exception) {
                locations.filterNotNull().forEach { historyService.removeHistoryUpdate(it, jwt) }
                throw ex
            }
        } else conceptRepository.saveAll(conceptsAndOperations.keys).map { it.withHighestVersionDTO() }

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

    fun searchConcepts(orgNumber: String, search: SearchOperation): Paginated =
        conceptSearchService.searchConcepts(orgNumber, search)
            .map { it.withHighestVersionDTO() }
            .filter { if(search.filters.onlyCurrentVersions) it.isCurrentVersion() else true }
            .toList()
            .paginate(search.pagination)

    private fun List<Begrep>.paginate(pagination: Pagination): Paginated {
        val currentPage = if (pagination.page > 0) pagination.page else 0
        val pageSize = if (pagination.size > 0) pagination.size else 10
        val totalElements = size
        val totalPages = ceil(totalElements.toDouble() / pageSize).roundToInt()
        val nextPage = currentPage.inc()

        val fromIndex = currentPage.times(pageSize)
        val toIndex = nextPage.times(pageSize)

        val hits = when {
            currentPage >= totalPages -> emptyList()
            nextPage == totalPages -> subList(fromIndex, totalElements)
            else -> subList(fromIndex, toIndex)
        }

        return Paginated(
            hits = hits,
            page = PageMeta(
                currentPage = currentPage,
                size = pageSize,
                totalElements = totalElements,
                totalPages = totalPages
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

    private fun Begrep.isCurrentVersion(): Boolean =
        erSistPublisert || isUnpublished()

    private fun Begrep.isUnpublished(): Boolean =
        id == originaltBegrep && !erPublisert

}
