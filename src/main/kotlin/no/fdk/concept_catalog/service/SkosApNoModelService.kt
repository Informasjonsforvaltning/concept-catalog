package no.fdk.concept_catalog.service

import no.fdk.concept_catalog.configuration.ApplicationProperties
import no.fdk.concept_catalog.model.Begrep
import no.fdk.concept_catalog.model.BegrepsRelasjon
import no.fdk.concept_catalog.model.Definisjon
import no.fdk.concept_catalog.model.ForholdTilKildeEnum
import no.fdk.concept_catalog.model.URITekst
import no.fdk.concept_catalog.rdf.EUVOC
import no.fdk.concept_catalog.rdf.SCHEMA
import no.fdk.concept_catalog.rdf.SKOSNO
import no.fdk.concept_catalog.rdf.XKOS
import org.apache.jena.datatypes.xsd.impl.XSDDateType
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.Resource
import org.apache.jena.vocabulary.DCAT
import org.apache.jena.vocabulary.DCTerms
import org.apache.jena.vocabulary.RDF
import org.apache.jena.vocabulary.RDFS
import org.apache.jena.vocabulary.SKOS
import org.apache.jena.vocabulary.SKOSXL
import org.apache.jena.vocabulary.VCARD4
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate
import java.time.ZoneId

private val logger = LoggerFactory.getLogger(SkosApNoModelService::class.java)

@Service
class SkosApNoModelService(
    private val conceptService: ConceptService,
    private val applicationProperties: ApplicationProperties
) {

    private val NB = "nb"
    private val ASSOCIATIVE = "assosiativ"
    private val PARTITIVE = "partitiv"
    private val GENERIC = "generisk"
    private val OMFATTER = "omfatter"
    private val ERDELAV = "erDelAv"
    private val UNDERORDNET = "underordnet"
    private val OVERORDNET = "overordnet"

    private fun enhetsregisteretUri(orgId: String): String = "https://data.brreg.no/enhetsregisteret/api/enheter/$orgId"

    private fun subjectsURI(orgId: String): String = "${applicationProperties.adminServiceUri}/$orgId/concepts/subjects#"

    private fun Model.safeCreateResource(uri: String?) =
        if (uri != null) createResource(escapeURI(uri))
        else createResource()

    fun buildModelForPublishersCollection(publisherId: String): Model {
        val model = ModelFactory.createDefaultModel()
        model.addConceptNamespaces()
        model.addConceptsToCollection(publisherId)
        return model
    }

    fun buildModelForAllCollections(): Model {
        val model = ModelFactory.createDefaultModel()
        model.addConceptNamespaces()

        conceptService
                .getAllPublisherIds()
                .forEach { model.addConceptsToCollection(it) }

        return model
    }

    fun buildModelForConcept(collectionId: String, id: String): Model {
        val concept = conceptService.getLastPublished(id)
        val model = ModelFactory.createDefaultModel()

        if (concept?.ansvarligVirksomhet?.id == collectionId) {
            model.addConceptToModel(concept, conceptService.getLastPublishedForOrganization(collectionId).mapNotNull { it.id })
        } else throw ResponseStatusException(HttpStatus.NOT_FOUND)

        return model
    }

    private fun Model.createCollectionResource(publisherId: String): Resource {
        val uri = getCollectionUri(publisherId)
        return createResource(uri)
            .addProperty(RDF.type, SKOS.Collection)
            .addProperty(DCTerms.identifier, createLiteral(uri))
            .addProperty(RDFS.label, "Concept collection belonging to $publisherId")
            .addProperty(DCTerms.publisher, safeCreateResource(enhetsregisteretUri(publisherId)))
    }

    private fun Model.addConceptsToCollection(publisherId: String) {
        val collectionResource = createCollectionResource(publisherId)

        val publishedConcepts = conceptService.getLastPublishedForOrganization(publisherId)
        val publishedConceptIds = publishedConcepts.mapNotNull { it.id }

        publishedConcepts.forEach { collectionResource.addConceptToCollection(it, publishedConceptIds) }
    }

    private fun Resource.safeAddPublisher(publisherId: String?): Resource {
        if (publisherId != null) {
            addProperty(DCTerms.publisher, model.safeCreateResource(enhetsregisteretUri(publisherId)))
        }
        return this
    }

    private fun Resource.safeAddModified(date: LocalDate?): Resource {
        if (date != null) {
            addProperty(DCTerms.modified, model.createTypedLiteral(localDateToXSDDateTime(date), XSDDateType.XSDdate))
        }
        return this
    }

    private fun Resource.addConceptToCollection(concept: Begrep, publishedIds: List<String>) {
        if (concept.originaltBegrep == null) logger.error("Concept has no original id, will not serialize.", Exception("Concept has no original id, will not serialize."))
        else {
            val conceptURI = getConceptUri(uri, concept.originaltBegrep)
            val conceptResource = model.createResource(conceptURI)
                .addProperty(RDF.type, SKOS.Concept)
                .addProperty(DCTerms.identifier, model.createLiteral(conceptURI))
                .safeAddPublisher(concept.ansvarligVirksomhet.id)
                .safeAddModified(concept.endringslogelement?.endringstidspunkt?.atZone(ZoneId.systemDefault())?.toLocalDate())

            conceptResource.addPropertiesToConcept(concept, uri, publishedIds)
            addProperty(SKOS.member, conceptResource)
        }
    }

    private fun Model.addConceptToModel(concept: Begrep, publishedIds: List<String>) {
        if (concept.originaltBegrep == null) logger.error("Concept has no original id, will not serialize.", Exception("Concept has no original id, will not serialize."))
        else {
            val collectionURI = getCollectionUri(concept.ansvarligVirksomhet.id)
            val conceptURI = getConceptUri(collectionURI, concept.originaltBegrep)
            val conceptResource = createResource(conceptURI)
                .addProperty(RDF.type, SKOS.Concept)
                .addProperty(DCTerms.identifier, createLiteral(conceptURI))
                .safeAddPublisher(concept.ansvarligVirksomhet.id)
                .safeAddModified(concept.endringslogelement?.endringstidspunkt?.atZone(ZoneId.systemDefault())?.toLocalDate())

            conceptResource.addPropertiesToConcept(concept, collectionURI, publishedIds)
        }
    }

    private fun Resource.addPropertiesToConcept(concept: Begrep, collectionURI: String, publishedIds: List<String>) {
        addPrefLabelToConcept(concept)
        addDefinitionToConcept(concept)
        addPublicDefinitionToConcept(concept)
        addSpecialistDefinitionToConcept(concept)
        addAltLabelToConcept(concept)
        addHiddenLabelToConcept(concept)
        addExampleToConcept(concept)
        addSubjectToConcept(concept)
        addContactPointToConcept(concept)
        addSeeAlsoReferencesToConcept(concept, collectionURI, publishedIds)
        addValidityPeriodToConcept(concept)
        addBegrepsRelasjonToConcept(concept, collectionURI, publishedIds)
        addReplacedByReferencesToConcept(concept, collectionURI, publishedIds)
        addStatusToConcept(concept)
        addScopeNoteToConcept(concept)
        addScopeToConcept(concept)
    }

    private fun getCollectionUri(publisherId: String): String {
        return "${applicationProperties.collectionBaseUri}/collections/$publisherId"
    }

    private fun getConceptUri(collectionUri: String, conceptId: String): String {
        return "$collectionUri/concepts/$conceptId"
    }

    private fun Resource.addPrefLabelToConcept(concept: Begrep) {
        concept.anbefaltTerm?.navn
            ?.filterValues { it.toString().isNotBlank() }
            ?.takeIf { it.isNotEmpty() }
            ?.let {
                val prefLabelResource = model.createResource(SKOSXL.Label)
                it.forEach { (key, value) -> prefLabelResource.addProperty(SKOSXL.literalForm, value.toString(), key) }
                addProperty(SKOSXL.prefLabel, prefLabelResource)
            }
    }

    private fun Resource.addPublicDefinitionToConcept(concept: Begrep) {
        val definitionResource = model.createDefinitionResource(concept.definisjonForAllmennheten)
        if (definitionResource != null) {
            definitionResource.addProperty(DCTerms.audience, SKOSNO.allmennheten)

            addProperty(SKOSNO.definisjon, definitionResource)
        }
    }


    private fun Resource.addSpecialistDefinitionToConcept(concept: Begrep) {
        val definitionResource = model.createDefinitionResource(concept.definisjonForSpesialister)
        if (definitionResource != null) {
            definitionResource.addProperty(DCTerms.audience, SKOSNO.fagspesialist)

            addProperty(SKOSNO.definisjon, definitionResource)
        }
    }


    private fun Resource.addDefinitionToConcept(concept: Begrep) {
        val definitionResource = model.createDefinitionResource(concept.definisjon)
        if (definitionResource != null) {
            addProperty(SKOSNO.definisjon, definitionResource)
        }
    }

    private fun Model.createDefinitionResource(definition: Definisjon?): Resource? =
        definition?.tekst
            ?.filterValues { it.isNotBlank() }
            ?.takeIf { it.isNotEmpty() }
            ?.let {
                val definitionResource = createResource()
                    .addProperty(RDF.type, SKOSNO.Definisjon)

                it.forEach { (key, value) -> definitionResource.addProperty(RDFS.label, value, key) }

                definitionResource.addSourceDescriptionToDefinition(definition)
                definitionResource
            }

    private fun Resource.addScopeToConcept(concept: Begrep) {
        concept.omfang
            ?.takeIf { !it.tekst.isNullOrBlank() || it.uri.isValidURI() }
            ?.let { source -> addURIOrText(SKOSNO.valueRange, source) }
    }

    private fun Resource.addStatusToConcept(concept: Begrep) {
        concept.statusURI
            ?.takeIf { it.isValidURI() }
            ?.let { addProperty(EUVOC.status, model.safeCreateResource(it)) }
    }

    private fun Resource.addScopeNoteToConcept(concept: Begrep) {
        concept.merknad
            ?.filterValues { it.isNotBlank() }
            ?.forEach { (key, value) -> addProperty(SKOS.scopeNote, value, key) }
    }

    private fun Resource.addSourceDescriptionToDefinition(definition: Definisjon) {
        definition.kildebeskrivelse
            ?.takeIf { !it.kilde.isNullOrEmpty() || it.forholdTilKilde == ForholdTilKildeEnum.EGENDEFINERT }
            ?.let {
                when (it.forholdTilKilde) {
                    ForholdTilKildeEnum.EGENDEFINERT -> addProperty(SKOSNO.forholdTilKilde, SKOSNO.egendefinert)
                    ForholdTilKildeEnum.BASERTPAAKILDE -> addProperty(SKOSNO.forholdTilKilde, SKOSNO.basertPåKilde)
                    ForholdTilKildeEnum.SITATFRAKILDE -> addProperty(SKOSNO.forholdTilKilde, SKOSNO.sitatFraKilde)
                    else -> {}
                }

                if (!it.kilde.isNullOrEmpty()) {
                    it.kilde
                        .filter { sourceEntry -> !sourceEntry.tekst.isNullOrBlank() || sourceEntry.uri.isValidURI() }
                        .forEach { sourceEntry -> addURIText(DCTerms.source, sourceEntry) }
                }
            }
    }

    private fun Resource.addURIOrText(predicate: Property, uriText: URITekst) {
        if (uriText.uri.isValidURI()) {
            addProperty(predicate, model.safeCreateResource(uriText.uri))
        }
        else if (!uriText.tekst.isNullOrBlank()) {
            addProperty(predicate, uriText.tekst, NB)
        }
    }

    private fun Resource.addURIText(predicate: Property, uriText: URITekst) {
        if (uriText.uri.isValidURI() || !uriText.tekst.isNullOrBlank()) {
            val sourceResource = model.createResource()
            if (!uriText.tekst.isNullOrBlank()) {
                sourceResource.addProperty(RDFS.label, uriText.tekst, NB)
            }
            if (uriText.uri.isValidURI()) {
                sourceResource.addProperty(RDFS.seeAlso, model.safeCreateResource(uriText.uri))
            }
            addProperty(predicate, sourceResource)
        }
    }

    private fun Resource.addSKOSXLLabel(predicate: Property, labelText: String, language: String) {
        val labelResource = model.createResource()
            .addProperty(RDF.type, SKOSXL.Label)
            .addProperty(SKOSXL.literalForm, labelText, language)

        addProperty(predicate, labelResource)
    }

    private fun Resource.addAltLabelToConcept(concept: Begrep) {
        concept.tillattTerm
            ?.filterValues { it.isNotEmpty() }
            ?.takeIf { it.isNotEmpty() }
            ?.forEach { (key, entry) ->
                entry.forEach { value ->
                    addSKOSXLLabel(SKOSXL.altLabel, value, key)
                }
            }
    }

    private fun Resource.addHiddenLabelToConcept(concept: Begrep) {
        concept.frarådetTerm
            ?.filterValues { it.isNotEmpty() }
            ?.takeIf { it.isNotEmpty() }
            ?.forEach { (key, entry) ->
                entry.forEach { value ->
                    addSKOSXLLabel(SKOSXL.hiddenLabel, value, key)
                }
            }
    }

    private fun Resource.addExampleToConcept(concept: Begrep) {
        concept.eksempel
            ?.filterValues { it.isNotBlank() }
            ?.forEach { (key, value) -> addProperty(SKOS.example, value, key) }
    }

    private fun Resource.addSubjectToConcept(concept: Begrep) {
        concept.fagområde
            ?.filterValues { it.isNotEmpty() }
            ?.forEach { (key, entry) ->
                entry.forEach { value ->
                    addProperty(DCTerms.subject, value, key)
                }
            }
        concept.fagområdeKoder
            ?.filter { !it.isNullOrEmpty() }
            ?.forEach { kode ->
                addProperty(
                    DCTerms.subject,
                    model.safeCreateResource("${subjectsURI(concept.ansvarligVirksomhet.id)}$kode")
                )
            }
    }

    private fun Resource.addContactPointToConcept(concept: Begrep) {
        concept.kontaktpunkt
            ?.let {
                val contactPointResource = model.createResource(VCARD4.Organization)

                val email = it.harEpost
                val phone = it.harTelefon

                if (email?.isNotBlank() == true) {
                    contactPointResource.addProperty(VCARD4.hasEmail, model.emailResource(email))
                }

                if (phone?.isNotBlank() == true) {
                    contactPointResource.addProperty(VCARD4.hasTelephone, model.telephoneResource(phone))
                }

                addProperty(DCAT.contactPoint, contactPointResource)
            }
    }

    private fun Resource.addBegrepsRelasjonToConcept(concept: Begrep, collectionURI: String, publishedIds: List<String>) {
        concept.begrepsRelasjon
            ?.forEach { addRelationResource(it, it.relatertBegrep) }

        concept.internBegrepsRelasjon
            ?.filter { publishedIds.contains(it.relatertBegrep) }
            ?.forEach { addRelationResource(it, it.internalRelationURI(collectionURI)) }
    }



    private fun Resource.addRelationResource(relation: BegrepsRelasjon, relationURI: String?) {
        val relationResource = model.createResource()

        if (relation.relasjon == ASSOCIATIVE) {
            relationResource.addProperty(RDF.type, SKOSNO.AssosiativRelasjon)

            relation.beskrivelse
                ?.filterValues { description -> description.isNotBlank() }
                ?.takeIf { description -> description.isNotEmpty() }
                ?.forEach { (key, value) -> relationResource.addProperty(SKOSNO.relationRole, value, key) }

            if (relationURI?.isNotBlank() == true) {
                relationResource.addProperty(SKOS.related, model.safeCreateResource(relationURI))
            }
            addProperty(SKOSNO.assosiativRelasjon, relationResource)
        }
        if (relation.relasjon == PARTITIVE) {
            relationResource.addProperty(RDF.type, SKOSNO.PartitivRelasjon)

            val relationType = relation.relasjonsType

            relation.inndelingskriterium
                ?.filterValues { criteria -> criteria.isNotBlank() }
                ?.takeIf { criteria -> criteria.isNotEmpty() }
                ?.forEach { (key, value) -> relationResource.addProperty(DCTerms.description, value, key) }

            if (relationType == OMFATTER) {
                relationResource.addProperty(DCTerms.hasPart, model.safeCreateResource(relationURI))
            }
            if (relationType == ERDELAV) {
                relationResource.addProperty(DCTerms.isPartOf, model.safeCreateResource(relationURI))
            }
            addProperty(SKOSNO.partitivRelasjon, relationResource)
        }
        if (relation.relasjon == GENERIC) {
            relationResource.addProperty(RDF.type, SKOSNO.GeneriskRelasjon)

            val relationType = relation.relasjonsType

            relation.inndelingskriterium
                ?.filterValues { criteria -> criteria.isNotBlank() }
                ?.takeIf { criteria -> criteria.isNotEmpty() }
                ?.forEach { (key, value) -> relationResource.addProperty(SKOSNO.inndelingskriterium, value, key) }

            if (relationType == OVERORDNET) {
                relationResource.addProperty(XKOS.specializes, model.safeCreateResource(relationURI))
            }
            if (relationType == UNDERORDNET) {
                relationResource.addProperty(XKOS.generalizes, model.safeCreateResource(relationURI))
            }
            addProperty(SKOSNO.generiskRelasjon, relationResource)
        }
    }

    private fun Resource.addSeeAlsoReferencesToConcept(concept: Begrep, collectionURI: String, publishedIds: List<String>) {
        concept.seOgså
            ?.filter { it.isNotBlank() }
            ?.forEach { addProperty(RDFS.seeAlso, model.safeCreateResource(it)) }

        concept.internSeOgså
            ?.filter { publishedIds.contains(it) }
            ?.map { getConceptUri(collectionURI, it) }
            ?.forEach { addProperty(RDFS.seeAlso, model.safeCreateResource(it)) }
    }

    private fun Resource.addReplacedByReferencesToConcept(concept: Begrep, collectionURI: String, publishedIds: List<String>) {
        concept.erstattesAv
            ?.filter { it.isNotBlank() }
            ?.forEach {
                addProperty(DCTerms.isReplacedBy, model.safeCreateResource(it))
                model.getResource(it).addProperty(DCTerms.replaces, model.safeCreateResource(uri))
            }
        concept.internErstattesAv
            ?.filter { publishedIds.contains(it) }
            ?.map {getConceptUri(collectionURI, it)}
            ?.forEach{
                addProperty(DCTerms.isReplacedBy, model.safeCreateResource(it))
                model.getResource(it).addProperty(DCTerms.replaces, model.safeCreateResource(uri))
            }
    }

    private fun Resource.addValidityPeriodToConcept(concept: Begrep) {
        val validFromIncluding = concept.gyldigFom
        val validToIncluding = concept.gyldigTom

        when {
            validFromIncluding != null && validToIncluding != null && validFromIncluding.isBefore(validToIncluding) -> {
                val periodOfTimeResource = model.createResource(DCTerms.PeriodOfTime)
                model.createTypedLiteral(localDateToXSDDateTime(validFromIncluding), XSDDateType.XSDdate)
                    .let { start -> periodOfTimeResource.addProperty(SCHEMA.startDate, start) }
                model.createTypedLiteral(localDateToXSDDateTime(validToIncluding), XSDDateType.XSDdate)
                    .let { end -> periodOfTimeResource.addProperty(SCHEMA.endDate, end) }
                addProperty(DCTerms.temporal, periodOfTimeResource)
            }
            validFromIncluding != null -> {
                val periodOfTimeResource = model.createResource(DCTerms.PeriodOfTime)
                model.createTypedLiteral(localDateToXSDDateTime(validFromIncluding), XSDDateType.XSDdate)
                    .let { start -> periodOfTimeResource.addProperty(SCHEMA.startDate, start) }
                addProperty(DCTerms.temporal, periodOfTimeResource)
            }
            validToIncluding != null -> {
                val periodOfTimeResource = model.createResource(DCTerms.PeriodOfTime)
                model.createTypedLiteral(localDateToXSDDateTime(validToIncluding), XSDDateType.XSDdate)
                    .let { end -> periodOfTimeResource.addProperty(SCHEMA.endDate, end) }
                addProperty(DCTerms.temporal, periodOfTimeResource)
            }
        }
    }

    private fun Model.addConceptNamespaces() {
        setNsPrefix("adms", "http://www.w3.org/ns/adms#")
        setNsPrefix("dcat", "http://www.w3.org/ns/dcat#")
        setNsPrefix("dct", "http://purl.org/dc/terms/")
        setNsPrefix("foaf", "http://xmlns.com/foaf/0.1/")
        setNsPrefix("owl", "http://www.w3.org/2002/07/owl#")
        setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#")
        setNsPrefix("schema", "http://schema.org/")
        setNsPrefix("skos", "http://www.w3.org/2004/02/skos/core#")
        setNsPrefix("spdx", "http://spdx.org/rdf/terms#")
        setNsPrefix("xsd", "http://www.w3.org/2001/XMLSchema#")
        setNsPrefix("vcard", "http://www.w3.org/2006/vcard/ns#")
        setNsPrefix("dqv", "http://www.w3.org/ns/dqv#")
        setNsPrefix("iso", "http://iso.org/25012/2008/dataquality/")
        setNsPrefix("oa", "http://www.w3.org/ns/prov#")
        setNsPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#")
        setNsPrefix("skosxl", "http://www.w3.org/2008/05/skos-xl#")

        setNsPrefix("skosno", SKOSNO.uri)
        setNsPrefix("xkos", XKOS.uri)
    }

    private fun Model.emailResource(email: String): Resource =
        safeCreateResource("mailto:$email")

    fun Model.telephoneResource(telephone: String): Resource =
        telephone.trim { it <= ' ' }
            .filterIndexed { index, c ->
                when {
                    index == 0 && c == '+' -> true // global-number-digits
                    c in '0'..'9' -> true // digit
                    else -> false // skip visual-separator and other content
                }
            }
            .let { safeCreateResource("tel:$it") }

    private fun BegrepsRelasjon.internalRelationURI(collectionURI: String): String? =
        relatertBegrep?.let { getConceptUri(collectionURI, it) }
}
