package no.fdk.concept_catalog.rdf

import no.fdk.concept_catalog.model.*
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
                eksempel = it.extractEksempel()
            )
        }

    return begreper
}

fun Resource.extractVersjonr(): SemVer? {
    return this.getProperty(OWL.versionInfo)
        ?.let { it.`object`.asLiteralOrNull()?.string }
        ?.takeIf { it.isNotBlank() and SEM_VAR_REGEX.matches(it) }
        ?.let {
            SEM_VAR_REGEX.matchEntire(it)?.destructured?.let { (major, minor, patch) ->
                SemVer(major.toInt(), minor.toInt(), patch.toInt())
            }
        }
}

fun Resource.extractStatusUri(): String? {
    return this.getProperty(EUVOC.status)
        ?.let { it.`object`.asResourceOrNull()?.uri }
}

fun Resource.extractAnbefaltTerm(): Term? {
    return extractLocalizesStrings(SKOS.prefLabel)
        ?.let { Term(it) }
}

fun Resource.extractTillattTerm(): Map<String, List<String>>? {
    return extractTerm(SKOS.altLabel)
}

fun Resource.extractFrarådetTerm(): Map<String, List<String>>? {
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
                ?.asResourceOrNull()
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
                ?.asResourceOrNull()
                ?.hasURI(AUDIENCE_TYPE.specialist.uri) == true
        }
        .firstNotNullOfOrNull { it.extractDefinition() }
}

fun Resource.extractMerknad(): Map<String, String>? {
    return extractLocalizesStrings(SKOS.scopeNote)
}

fun Resource.extractEksempel(): Map<String, String>? {
    return extractLocalizesStrings(SKOS.example)
}

private fun Resource.extractDefinition(): Definisjon? {
    val relationshipWithSource: ForholdTilKildeEnum? = this.getProperty(SKOSNO.relationshipWithSource)
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

    val source: List<URITekst>? = this.listProperties(DCTerms.source)
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

    val value: Map<String, String>? = this.extractLocalizesStrings(RDF.value)

    return value?.let { Definisjon(tekst = it, kildebeskrivelse = sourceDescription) }
}

private fun Resource.extractLocalizesStrings(property: Property): Map<String, String>? {
    return this.listProperties(property)
        .toList()
        .mapNotNull { it.`object`.asLiteralOrNull() }
        .filter { it.language.isNotBlank() and it.string.isNotBlank() }
        .associate { it.language to it.string }
        .takeIf { it.isNotEmpty() }
}

private fun RDFNode.asLiteralOrNull(): Literal? {
    return if (this.isLiteral) this.asLiteral() else null
}

private fun RDFNode.asResourceOrNull(): Resource? {
    return if (this.isResource) this.asResource() else null
}
