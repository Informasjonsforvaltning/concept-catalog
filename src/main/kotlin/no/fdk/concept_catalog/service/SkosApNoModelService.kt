package no.fdk.concept_catalog.service

import no.fdk.concept_catalog.configuration.ApplicationProperties
import no.fdk.concept_catalog.model.Begrep
import no.fdk.concept_catalog.model.ForholdTilKildeEnum
import no.norge.data.skos_ap_no.concept.builder.Conceptcollection.CollectionBuilder
import no.norge.data.skos_ap_no.concept.builder.Conceptcollection.Concept.ConceptBuilder
import no.norge.data.skos_ap_no.concept.builder.Conceptcollection.Concept.Sourcedescription.Definition.DefinitionBuilder
import no.norge.data.skos_ap_no.concept.builder.ModelBuilder
import no.norge.data.skos_ap_no.concept.builder.generic.SourceType
import org.apache.jena.rdf.model.Model
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.ZoneId

private val logger = LoggerFactory.getLogger(SkosApNoModelService::class.java)

@Service
class SkosApNoModelService(
    private val conceptService: ConceptService,
    private val applicationProperties: ApplicationProperties
) {

    private val NB = "nb"

    fun buildModelForPublishersCollection(publisherId: String): Model {
        logger.debug("Building concept collection model for publisher: {}", publisherId)

        val modelBuilder = instantiateModelBuilder()

        addConceptsToCollection(modelBuilder, publisherId)

        return modelBuilder.build()
    }

    fun buildModelForAllCollections(): Model {
        logger.debug("Building concept collection models for all publishers")

        val modelBuilder = instantiateModelBuilder()

        conceptService
                .getAllPublisherIds()
                .forEach { addConceptsToCollection(modelBuilder, it) }

        return modelBuilder.build()
    }

    fun buildModelForConcept(collectionId: String, id: String): Model {
        logger.debug("Building model for concept: {}", id)
        val concept = conceptService.getLastPublished(id)
        val modelBuilder = instantiateModelBuilder()

        if (concept?.ansvarligVirksomhet?.id == collectionId) {
            addConceptToModel(modelBuilder, concept)
        } else throw ResponseStatusException(HttpStatus.NOT_FOUND)

        return modelBuilder.build()
    }

    private fun instantiateModelBuilder(): ModelBuilder {
        return ModelBuilder.builder()
    }

    private fun instantiateCollectionBuilder(modelBuilder: ModelBuilder, publisherId: String): CollectionBuilder {
        return modelBuilder
                .collectionBuilder(getCollectionUri(publisherId))
                .publisher(publisherId)
                .name("Concept collection belonging to $publisherId")
    }

    private fun addConceptsToCollection(modelBuilder: ModelBuilder, publisherId: String) {
        val collectionBuilder = instantiateCollectionBuilder(modelBuilder, publisherId)

        conceptService
                .getLastPublishedForOrganization(publisherId)
                .forEach { addConceptToCollection(collectionBuilder, it) }
    }

    private fun addConceptToCollection(collectionBuilder: CollectionBuilder, concept: Begrep) {
        if (concept.originaltBegrep == null) logger.error("Concept has no original id, will not serialize.", Exception("Concept has no original id, will not serialize."))
        else {
            val conceptURI = getConceptUri(collectionBuilder, concept.originaltBegrep)
            val conceptBuilder = collectionBuilder
                .conceptBuilder(conceptURI)
                .identifier(conceptURI)
                .publisher(concept.ansvarligVirksomhet?.id)
                .modified(concept.endringslogelement?.endringstidspunkt?.atZone(ZoneId.systemDefault())?.toLocalDate())

            addPropertiesToConcept(conceptBuilder, concept)

            conceptBuilder.build()
        }
    }

    private fun addConceptToModel(modelBuilder: ModelBuilder, concept: Begrep) {
        if (concept.originaltBegrep == null || concept.ansvarligVirksomhet?.id == null) logger.error("Concept has no original id, will not serialize.", Exception("Concept has no original id, will not serialize."))
        else {
            val conceptURI = "${getCollectionUri(concept.ansvarligVirksomhet.id)}/${concept.originaltBegrep}"
            val conceptBuilder = modelBuilder
                .conceptBuilder(conceptURI)
                .identifier(conceptURI)
                .publisher(concept.ansvarligVirksomhet.id)
                .modified(concept.endringslogelement?.endringstidspunkt?.atZone(ZoneId.systemDefault())?.toLocalDate())

            addPropertiesToConcept(conceptBuilder, concept)

            conceptBuilder.buildSingleConcept()
        }
    }

    private fun addPropertiesToConcept(conceptBuilder: ConceptBuilder, concept: Begrep) {
        addPrefLabelToConcept(conceptBuilder, concept)
        addDefinitionToConcept(conceptBuilder, concept)
        addAltLabelToConcept(conceptBuilder, concept)
        addHiddenLabelToConcept(conceptBuilder, concept)
        addExampleToConcept(conceptBuilder, concept)
        addSubjectToConcept(conceptBuilder, concept)
        addDomainOfUseToConcept(conceptBuilder, concept)
        addContactPointToConcept(conceptBuilder, concept)
        addSeeAlsoReferencesToConcept(conceptBuilder, concept)
        addValidityPeriodToConcept(conceptBuilder, concept)
    }

    private fun getCollectionUri(publisherId: String): String {
        return "${applicationProperties.collectionBaseUri}/collections/$publisherId"
    }

    private fun getConceptUri(collectionBuilder: CollectionBuilder, conceptId: String): String {
        return "${getCollectionUri(collectionBuilder.publisher)}/$conceptId"
    }

    private fun addPrefLabelToConcept(conceptBuilder: ConceptBuilder, concept: Begrep) {
        concept.anbefaltTerm?.navn
            ?.filterValues { it.toString().isNotBlank() }
            ?.takeIf { it.isNotEmpty() }
            ?.let {
                val prefLabelBuilder = conceptBuilder.prefLabelBuilder()

                it.forEach { (key, value) -> prefLabelBuilder.label(value.toString(), key) }

                prefLabelBuilder.build()
            }
    }


    private fun addDefinitionToConcept(conceptBuilder: ConceptBuilder, concept: Begrep) {
        concept.definisjon?.tekst
            ?.filterValues { it.isNotBlank() }
            ?.takeIf { it.isNotEmpty() }
            ?.let {
                val definitionBuilder = conceptBuilder.definitionBuilder()

                it.forEach { (key, value) -> definitionBuilder.text(value, key) }

                addScopeToDefinition(definitionBuilder, concept)
                addScopeNoteToDefinition(definitionBuilder, concept)
                addSourceDescriptionToDefinition(definitionBuilder, concept)

                definitionBuilder.build()
            }
    }

    private fun addScopeToDefinition(definitionBuilder: DefinitionBuilder, concept: Begrep) {
        concept.omfang
            ?.takeIf { it.tekst?.isNotBlank() == true || it.uri?.isNotBlank() == true }
            ?.let { definitionBuilder.scopeBuilder().label(it.tekst, NB).seeAlso(it.uri).build() }
    }

    private fun addScopeNoteToDefinition(definitionBuilder: DefinitionBuilder, concept: Begrep) {
        concept.merknad
            ?.filterValues { it.toString().isNotBlank() }
            ?.forEach { (key, entry) ->
                entry.forEach { value -> definitionBuilder.scopeNote(value, key) }
            }
    }

    private fun addSourceDescriptionToDefinition(definitionBuilder: DefinitionBuilder, concept: Begrep) {
        concept.kildebeskrivelse
            ?.takeIf { !it.kilde.isNullOrEmpty() || it.forholdTilKilde == ForholdTilKildeEnum.EGENDEFINERT }
            ?.let {
                val sourceDescriptionBuilder = definitionBuilder.sourcedescriptionBuilder()

                it.forholdTilKilde.let { type ->
                    if (type == ForholdTilKildeEnum.EGENDEFINERT) {
                        sourceDescriptionBuilder.sourcetype(SourceType.Source.Userdefined)
                    }
                    if (type == ForholdTilKildeEnum.BASERTPAAKILDE) {
                        sourceDescriptionBuilder.sourcetype(SourceType.Source.BasedOn)
                    }
                    if (type == ForholdTilKildeEnum.SITATFRAKILDE) {
                        sourceDescriptionBuilder.sourcetype(SourceType.Source.QuoteFrom)
                    }
                }

                if (!it.kilde.isNullOrEmpty()) {
                    val sourceBuilder = sourceDescriptionBuilder.sourceBuilder()
                    it.kilde.forEach { source -> sourceBuilder.label(source.tekst, NB).seeAlso(source.uri) }
                    sourceBuilder.build()
                }

                sourceDescriptionBuilder.build()
            }
    }

    private fun addAltLabelToConcept(conceptBuilder: ConceptBuilder, concept: Begrep) {
        concept.tillattTerm
            ?.filterValues { it.isNotEmpty() }
            ?.takeIf { it.isNotEmpty() }
            ?.let {
                it.forEach { (key, entry) ->
                    entry.forEach { value -> conceptBuilder
                        .altLabelBuilder()
                        .label(value, key)
                        .build()
                    }
                }
            }
    }

    private fun addHiddenLabelToConcept(conceptBuilder: ConceptBuilder, concept: Begrep) {
        concept.frarådetTerm
            ?.filterValues { it.isNotEmpty() }
            ?.takeIf { it.isNotEmpty() }
            ?.let {
                it.forEach { (key, entry) ->
                    entry.forEach { value -> conceptBuilder
                        .hiddenLabelBuilder()
                        .label(value, key)
                        .build()
                    }
                }
            }
    }

    private fun addExampleToConcept(conceptBuilder: ConceptBuilder, concept: Begrep) {
        concept.eksempel
            ?.filterValues { it.toString().isNotBlank() }
            ?.forEach { (key, entry) ->
                entry.forEach { value -> conceptBuilder.example(value, key) }
            }
    }

    private fun addSubjectToConcept(conceptBuilder: ConceptBuilder, concept: Begrep) {
        concept.fagområde
            ?.filterValues { it.isNotBlank() }
            ?.forEach { (key, value) -> conceptBuilder.subject(value, key) }
    }

    private fun addDomainOfUseToConcept(conceptBuilder: ConceptBuilder, concept: Begrep) {
        concept.bruksområde
            ?.filterValues { it.isNotEmpty() }
            ?.forEach { (key, entry) -> entry.forEach { value -> conceptBuilder.domainOfUse(value, key) } }
    }

    private fun addContactPointToConcept(conceptBuilder: ConceptBuilder, concept: Begrep) {
        concept.kontaktpunkt
            ?.let {
                val contactPointBuilder = conceptBuilder.contactPointBuilder()

                val email = it.harEpost
                val phone = it.harTelefon

                if (email?.isNotBlank() == true) {
                    contactPointBuilder.email(email)
                }

                if (phone?.isNotBlank() == true) {
                    contactPointBuilder.telephone(phone)
                }

                contactPointBuilder.build()
            }
    }

    private fun addSeeAlsoReferencesToConcept(conceptBuilder: ConceptBuilder, concept: Begrep) {
        concept.seOgså
            ?.filter { it.isNotBlank() }
            ?.forEach { conceptBuilder.seeAlso(it) }
    }

    private fun addValidityPeriodToConcept(conceptBuilder: ConceptBuilder, concept: Begrep) {
        val validFromIncluding = concept.gyldigFom
        val validToIncluding = concept.gyldigTom

        if (validFromIncluding != null && validToIncluding != null) {
            if (validFromIncluding.isBefore(validToIncluding)) {
                conceptBuilder
                    .periodOfTimeBuilder()
                    .validFromIncluding(validFromIncluding)
                    .validToIncluding(validToIncluding)
                    .build()
            }
        } else {
            if (validFromIncluding != null) {
                conceptBuilder
                    .periodOfTimeBuilder()
                    .validFromIncluding(validFromIncluding)
                    .build()
            }
            if (validToIncluding != null) {
                conceptBuilder
                    .periodOfTimeBuilder()
                    .validToIncluding(validToIncluding)
                    .build()
            }
        }
    }
}
