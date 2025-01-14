package no.fdk.concept_catalog.rdf

import no.fdk.concept_catalog.model.*
import no.fdk.concept_catalog.service.isValidURI
import org.apache.jena.rdf.model.*
import org.apache.jena.vocabulary.DCTerms
import org.apache.jena.vocabulary.OWL
import org.apache.jena.vocabulary.RDF
import org.apache.jena.vocabulary.SKOS

private val SEM_VAR_REGEX = Regex("""^(\d+)\.(\d+)\.(\d+)$""")

fun Model.extractBegreper(catalogId: String): List<Begrep> {
    val begreper = this.listResourcesWithProperty(RDF.type, SKOS.Concept)
        .toList()
        .mapNotNull { it.asResourceOrNull() }
        .map {
            Begrep(
                ansvarligVirksomhet = Virksomhet(id = catalogId),
                versjonsnr = it.extractVersjonr(),
                statusURI = it.extractStatusUri(),
                anbefaltTerm = it.extractAnbefaltTerm(),
                tillattTerm = it.extractTillattTerm(),
                frarådetTerm = it.extractFrarådetTerm(),
                definisjon = it.extractDefinisjon(),
                definisjonForAllmennheten = it.extractDefinisjonForAllmennheten(),
                definisjonForSpesialister = it.extractDefinisjonForSpesialister(),
                merknad = it.extractMerknad(),
                eksempel = it.extractEksempel(),
                fagområde = it.extractFagområde(),
                fagområdeKoder = it.extractFagområdeKoder()
            )
        }

    return begreper
}

fun Resource.extractVersjonr(): SemVer? {
    return this.getProperty(OWL.versionInfo)
        ?.let { it.`object`.asLiteralOrNull()?.string }
        ?.takeIf { it.isNotBlank() && SEM_VAR_REGEX.matches(it) }
        ?.let {
            SEM_VAR_REGEX.matchEntire(it)?.destructured?.let { (major, minor, patch) ->
                SemVer(major.toInt(), minor.toInt(), patch.toInt())
            }
        }
}

fun Resource.extractStatusUri(): String? {
    return this.getProperty(EUVOC.status)
        ?.let { it.`object`.asResourceUriOrNull()?.uri }
}

fun Resource.extractAnbefaltTerm(): Term? {
    return extractLocalizedStrings(SKOS.prefLabel)
        ?.let { Term(it) }
}

fun Resource.extractTillattTerm(): Map<String, List<String>>? {
    return extractLocalizedStringsAsGrouping(SKOS.altLabel)
}

fun Resource.extractFrarådetTerm(): Map<String, List<String>>? {
    return extractLocalizedStringsAsGrouping(SKOS.hiddenLabel)
}

fun Resource.extractDefinisjon(): Definisjon? {
    return this.listProperties(EUVOC.xlDefinition)
        .toList()
        .mapNotNull { it.`object`.asResourceOrNull() }
        .filterNot { it.hasProperty(DCTerms.audience) }
        .firstNotNullOfOrNull { it.extractDefinition() }
}

fun Resource.extractDefinisjonForAllmennheten(): Definisjon? {
    return this.listProperties(EUVOC.xlDefinition)
        .toList()
        .mapNotNull { it.`object`.asResourceOrNull() }
        .filter {
            it.getProperty(DCTerms.audience)
                ?.`object`
                ?.asResourceUriOrNull()
                ?.hasURI(AUDIENCE_TYPE.public.uri) == true
        }
        .firstNotNullOfOrNull { it.extractDefinition() }
}

fun Resource.extractDefinisjonForSpesialister(): Definisjon? {
    return this.listProperties(EUVOC.xlDefinition)
        .toList()
        .mapNotNull { it.`object`.asResourceOrNull() }
        .filter {
            it.getProperty(DCTerms.audience)
                ?.`object`
                ?.asResourceUriOrNull()
                ?.hasURI(AUDIENCE_TYPE.specialist.uri) == true
        }
        .firstNotNullOfOrNull { it.extractDefinition() }
}

fun Resource.extractMerknad(): Map<String, String>? {
    return extractLocalizedStrings(SKOS.scopeNote)
}

fun Resource.extractEksempel(): Map<String, String>? {
    return extractLocalizedStrings(SKOS.example)
}

fun Resource.extractFagområde(): Map<String, List<String>>? {
    return extractLocalizedStringsAsGrouping(DCTerms.subject)
}

fun Resource.extractFagområdeKoder(): List<String>? {
    return this.listProperties(DCTerms.subject)
        .toList()
        .mapNotNull { it.`object`.asResourceUriOrNull() }
        .map { it.toString() }
        .takeIf { it.isNotEmpty() }
}

private fun Resource.extractDefinition(): Definisjon? {
    val relationshipWithSource: ForholdTilKildeEnum? = this.getProperty(SKOSNO.relationshipWithSource)
        ?.let { statement ->
            statement.`object`.asResourceUriOrNull()?.let {
                when {
                    it.hasURI(RELATIONSHIP.selfComposed.uri) -> ForholdTilKildeEnum.EGENDEFINERT
                    it.hasURI(RELATIONSHIP.directFromSource.uri) -> ForholdTilKildeEnum.BASERTPAAKILDE
                    it.hasURI(RELATIONSHIP.derivedFromSource.uri) -> ForholdTilKildeEnum.SITATFRAKILDE
                    else -> null
                }
            }
        }

    val source: List<URITekst>? = this.listProperties(DCTerms.source)
        .toList()
        .mapNotNull { statement ->
            statement.`object`.let { obj ->
                when {
                    obj.isLiteral -> URITekst(tekst = obj.asLiteralOrNull()?.string)
                    obj.isURIResource -> URITekst(uri = obj.asResourceUriOrNull()?.uri)
                    else -> null
                }
            }
        }
        .takeIf { it.isNotEmpty() }

    val sourceDescription: Kildebeskrivelse? = relationshipWithSource?.let {
        Kildebeskrivelse(forholdTilKilde = relationshipWithSource, kilde = source)
    }

    val value: Map<String, String>? = this.extractLocalizedStrings(RDF.value)

    return value?.let { Definisjon(tekst = it, kildebeskrivelse = sourceDescription) }
}

private fun Resource.extractLocalizedStringsAsGrouping(property: Property): Map<String, List<String>>? {
    return this.listProperties(property)
        .toList()
        .mapNotNull { it.`object`.asLiteralOrNull() }
        .filter { it.language.isNotBlank() && it.string.isNotBlank() }
        .groupBy { it.language }
        .mapValues { (_, literals) -> literals.map { it.string } }
        .takeIf { it.isNotEmpty() }
}

private fun Resource.extractLocalizedStrings(property: Property): Map<String, String>? {
    return this.listProperties(property)
        .toList()
        .mapNotNull { it.`object`.asLiteralOrNull() }
        .filter { it.language.isNotBlank() && it.string.isNotBlank() }
        .associate { it.language to it.string }
        .takeIf { it.isNotEmpty() }
}

private fun RDFNode.asLiteralOrNull(): Literal? {
    return if (this.isLiteral) this.asLiteral() else null
}

private fun RDFNode.asResourceOrNull(): Resource? {
    return if (this.isResource) this.asResource() else null
}

private fun RDFNode.asResourceUriOrNull(): Resource? {
    return if (this.isURIResource && this.asResource().uri.isValidURI()) this.asResource() else null
}
