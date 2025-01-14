package no.fdk.concept_catalog.rdf

import no.fdk.concept_catalog.model.*
import org.apache.jena.rdf.model.*
import org.apache.jena.vocabulary.DCTerms
import org.apache.jena.vocabulary.RDF
import org.apache.jena.vocabulary.SKOS

fun Model.extractBegreper(catalogId: String): List<Begrep> {
    val begreper = this.listResourcesWithProperty(RDF.type, SKOS.Concept)
        .toList()
        .mapNotNull { it.asResourceOrNull() }
        .map {
            Begrep(
                ansvarligVirksomhet = Virksomhet(id = catalogId),
                statusURI = it.extractStatusUri(),
                anbefaltTerm = it.extractAnbefaltTerm(),
                tillattTerm = it.extractTillattTerm(),
                frarådetTerm = it.extractFrarådetTerm(),
                definisjon = it.extractDefinisjon(),
                definisjonForAllmennheten = it.extractDefinisjonForAllmennheten(),
                definisjonForSpesialister = it.extractDefinisjonForSpesialister()
            )
        }

    return begreper
}

private fun Resource.extractStatusUri(): String? {
    return this.getProperty(EUVOC.status)
        ?.let { statement ->
            statement.`object`.asResourceOrNull()?.uri
        }
}

private fun Resource.extractAnbefaltTerm(): Term? {
    return this.listProperties(SKOS.prefLabel)
        .toList()
        .mapNotNull { it.`object`.asLiteralOrNull() }
        .filter { it.language.isNotBlank() and it.string.isNotBlank() }
        .associate { it.language to it.string }
        .takeIf { it.isNotEmpty() }
        ?.let { Term(it) }
}

private fun Resource.extractTillattTerm(): Map<String, List<String>>? {
    return extractTerm(SKOS.altLabel)
}

private fun Resource.extractFrarådetTerm(): Map<String, List<String>>? {
    return extractTerm(SKOS.hiddenLabel)
}

private fun Resource.extractTerm(property: Property): Map<String, List<String>>? {
    return this.listProperties(property)
        .toList()
        .mapNotNull { it.`object`.asLiteralOrNull() }
        .filter { it.language.isNotBlank() and it.string.isNotBlank() }
        .groupBy { it.language }
        .mapValues { (_, literals) -> literals.map { it.string } }
        .takeIf { it.isNotEmpty() }
}

private fun Resource.extractDefinisjon(): Definisjon? {
    return this.listProperties(EUVOC.xlDefinition)
        .toList()
        .mapNotNull { it.`object`.asResourceOrNull() }
        .filterNot { it.hasProperty(DCTerms.audience) }
        .firstNotNullOfOrNull { resource -> extractDefinition(resource) }
}

private fun Resource.extractDefinisjonForAllmennheten(): Definisjon? {
    return this.listProperties(EUVOC.xlDefinition)
        .toList()
        .mapNotNull { it.`object`.asResourceOrNull() }
        .filter {
            it.getProperty(DCTerms.audience)
                ?.`object`
                ?.asResourceOrNull()
                ?.hasURI(AUDIENCE_TYPE.public.uri) == true
        }
        .firstNotNullOfOrNull { resource -> extractDefinition(resource) }
}

private fun Resource.extractDefinisjonForSpesialister(): Definisjon? {
    return this.listProperties(EUVOC.xlDefinition)
        .toList()
        .mapNotNull { it.`object`.asResourceOrNull() }
        .filter {
            it.getProperty(DCTerms.audience)
                ?.`object`
                ?.asResourceOrNull()
                ?.hasURI(AUDIENCE_TYPE.specialist.uri) == true
        }
        .firstNotNullOfOrNull { resource -> extractDefinition(resource) }
}

private fun extractDefinition(resource: Resource): Definisjon? {
    val relationshipWithSource: ForholdTilKildeEnum? = resource.getProperty(SKOSNO.relationshipWithSource)
        ?.let { statement ->
            statement.`object`.asResourceOrNull()?.let {
                when {
                    it.hasURI(RELATIONSHIP.selfComposed.uri) -> ForholdTilKildeEnum.EGENDEFINERT
                    it.hasURI(RELATIONSHIP.directFromSource.uri) -> ForholdTilKildeEnum.BASERTPAAKILDE
                    it.hasURI(RELATIONSHIP.derivedFromSource.uri) -> ForholdTilKildeEnum.SITATFRAKILDE
                    else -> null
                }
            }
        }

    val source: List<URITekst>? = resource.listProperties(DCTerms.source)
        .toList()
        .mapNotNull { statement ->
            statement.`object`.let { obj ->
                when {
                    obj.isLiteral -> URITekst(tekst = obj.asLiteralOrNull()?.string)
                    obj.isResource -> URITekst(uri = obj.asResourceOrNull()?.uri)
                    else -> null
                }
            }
        }
        .takeIf { it.isNotEmpty() }

    val sourceDescription: Kildebeskrivelse? = relationshipWithSource?.let {
        Kildebeskrivelse(forholdTilKilde = relationshipWithSource, kilde = source)
    }

    val value: Map<String, String>? = resource.listProperties(RDF.value)
        .toList()
        .mapNotNull { it.`object`.asLiteralOrNull() }
        .filter { it.language.isNotBlank() and it.string.isNotBlank() }
        .associate { it.language to it.string }
        .takeIf { it.isNotEmpty() }

    return value?.let { Definisjon(tekst = it, kildebeskrivelse = sourceDescription) }
}

fun RDFNode.asLiteralOrNull(): Literal? {
    return if (this.isLiteral) this.asLiteral() else null
}

fun RDFNode.asResourceOrNull(): Resource? {
    return if (this.isResource) this.asResource() else null
}
