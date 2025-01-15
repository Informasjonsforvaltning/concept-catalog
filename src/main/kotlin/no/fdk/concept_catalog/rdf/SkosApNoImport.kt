package no.fdk.concept_catalog.rdf

import no.fdk.concept_catalog.model.*
import no.fdk.concept_catalog.service.isValidURI
import org.apache.jena.rdf.model.*
import org.apache.jena.vocabulary.*
import java.time.LocalDate
import java.time.format.DateTimeParseException

private val SEM_VAR_REGEX = Regex("""^(\d+)\.(\d+)\.(\d+)$""")
private val EMAIL_REGEX = Regex("""^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$""")
private val TELEPHONE_REGEX = Regex("""^\+?[0-9\s\-()]{7,15}$""")

fun Model.extractBegreper(catalogId: String): List<Begrep> {
    return this.listResourcesWithProperty(RDF.type, SKOS.Concept)
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
                fagområdeKoder = it.extractFagområdeKoder(),
                omfang = it.extractOmfang(),
                gyldigFom = it.extractGyldigFom(),
                gyldigTom = it.extractGyldigTom(),
                seOgså = it.extractSeOgså(),
                erstattesAv = it.extractErstattesAv(),
                kontaktpunkt = it.extractKontaktPunkt()
            )
        }
}

private fun Resource.extractVersjonr(): SemVer? {
    return this.getProperty(OWL.versionInfo)
        ?.let { it.`object`.asLiteralOrNull()?.string }
        ?.takeIf { it.isNotBlank() && SEM_VAR_REGEX.matches(it) }
        ?.let {
            SEM_VAR_REGEX.matchEntire(it)?.destructured?.let { (major, minor, patch) ->
                SemVer(major.toInt(), minor.toInt(), patch.toInt())
            }
        }
}

private fun Resource.extractStatusUri(): String? {
    return this.getProperty(EUVOC.status)
        ?.let { it.`object`.asUriResourceOrNull()?.uri }
}

private fun Resource.extractAnbefaltTerm(): Term? {
    return this.extractLocalizedStrings(SKOS.prefLabel)
        ?.let { Term(it) }
}

private fun Resource.extractTillattTerm(): Map<String, List<String>>? {
    return this.extractLocalizedStringsAsGrouping(SKOS.altLabel)
}

private fun Resource.extractFrarådetTerm(): Map<String, List<String>>? {
    return this.extractLocalizedStringsAsGrouping(SKOS.hiddenLabel)
}

private fun Resource.extractDefinisjon(): Definisjon? {
    return this.listProperties(EUVOC.xlDefinition)
        .toList()
        .mapNotNull { it.`object`.asResourceOrNull() }
        .filterNot { it.hasProperty(DCTerms.audience) }
        .firstNotNullOfOrNull { it.extractDefinition() }
}

private fun Resource.extractDefinisjonForAllmennheten(): Definisjon? {
    return this.listProperties(EUVOC.xlDefinition)
        .toList()
        .mapNotNull { it.`object`.asResourceOrNull() }
        .filter {
            it.getProperty(DCTerms.audience)
                ?.`object`
                ?.asUriResourceOrNull()
                ?.hasURI(AUDIENCE_TYPE.public.uri) == true
        }
        .firstNotNullOfOrNull { it.extractDefinition() }
}

private fun Resource.extractDefinisjonForSpesialister(): Definisjon? {
    return this.listProperties(EUVOC.xlDefinition)
        .toList()
        .mapNotNull { it.`object`.asResourceOrNull() }
        .filter {
            it.getProperty(DCTerms.audience)
                ?.`object`
                ?.asUriResourceOrNull()
                ?.hasURI(AUDIENCE_TYPE.specialist.uri) == true
        }
        .firstNotNullOfOrNull { it.extractDefinition() }
}

private fun Resource.extractMerknad(): Map<String, String>? {
    return this.extractLocalizedStrings(SKOS.scopeNote)
}

private fun Resource.extractEksempel(): Map<String, String>? {
    return this.extractLocalizedStrings(SKOS.example)
}

private fun Resource.extractFagområde(): Map<String, List<String>>? {
    return this.extractLocalizedStringsAsGrouping(DCTerms.subject)
}

private fun Resource.extractFagområdeKoder(): List<String>? {
    return this.listProperties(DCTerms.subject)
        .toList()
        .mapNotNull { it.`object`.asUriResourceOrNull() }
        .map { it.toString() }
        .takeIf { it.isNotEmpty() }
}

private fun Resource.extractOmfang(): URITekst? {
    val text = this.listProperties(SKOSNO.valueRange)
        .toList()
        .firstOrNull { it.`object`.isLiteral }
        ?.let { it.`object`.asLiteralOrNull()?.string }

    val uri = this.listProperties(SKOSNO.valueRange)
        .toList()
        .firstOrNull { it.`object`.isURIResource }
        ?.let { it.`object`.asUriResourceOrNull()?.uri }

    return URITekst(uri, text)
        .takeIf { text != null || uri != null }
}

private fun Resource.extractGyldigFom(): LocalDate? {
    return this.extractDate(EUVOC.startDate)
}

private fun Resource.extractGyldigTom(): LocalDate? {
    return this.extractDate(EUVOC.endDate)
}

private fun Resource.extractSeOgså(): List<String>? {
    return this.extractUri(RDFS.seeAlso)
}

private fun Resource.extractErstattesAv(): List<String>? {
    return this.extractUri(DCTerms.isReplacedBy)
}

private fun Resource.extractKontaktPunkt(): Kontaktpunkt? {
    return this.getProperty(DCAT.contactPoint)
        ?.`object`?.asResourceOrNull()
        ?.let { vcard ->
            val email = vcard.getProperty(VCARD4.hasEmail)
                ?.`object`?.asUriResourceOrNull()
                ?.toString()
                ?.removePrefix("mailto:")
                ?.takeIf { EMAIL_REGEX.matches(it) }

            val telephone = vcard.getProperty(VCARD4.hasTelephone)
                ?.let {
                    if (it.`object`.isURIResource) {
                        it.`object`.asUriResourceOrNull()
                            ?.toString()
                            ?.removePrefix("tel:")
                    } else {
                        it.`object`.asResourceOrNull()
                            ?.getProperty(VCARD4.hasValue)
                            ?.`object`
                            ?.asUriResourceOrNull()
                            ?.toString()
                    }
                }
                ?.toString()
                ?.removePrefix("tel:")
                ?.takeIf { TELEPHONE_REGEX.matches(it) }

            Kontaktpunkt(harEpost = email, harTelefon = telephone)
                .takeIf { email != null || telephone != null }
        }
}

private fun Resource.extractLocalizedStrings(property: Property): Map<String, String>? {
    return this.listProperties(property)
        .toList()
        .mapNotNull { it.`object`.asLiteralOrNull() }
        .filter { it.language.isNotBlank() && it.string.isNotBlank() }
        .associate { it.language to it.string }
        .takeIf { it.isNotEmpty() }
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

private fun Resource.extractDefinition(): Definisjon? {
    val relationshipWithSource: ForholdTilKildeEnum? = this.getProperty(SKOSNO.relationshipWithSource)
        ?.let { statement ->
            statement.`object`.asUriResourceOrNull()?.let {
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
            statement.`object`.let {
                when {
                    it.isLiteral -> URITekst(tekst = it.asLiteralOrNull()?.string)
                    it.isURIResource -> URITekst(uri = it.asUriResourceOrNull()?.uri)
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

private fun Resource.extractDate(property: Property): LocalDate? {
    return this.getProperty(property)
        ?.let { it.`object`.asLiteralOrNull()?.string }
        ?.takeIf { isValidDate(it) }
        ?.let { LocalDate.parse(it) }
}

private fun isValidDate(dateString: String): Boolean {
    return try {
        LocalDate.parse(dateString)
        true
    } catch (e: DateTimeParseException) {
        false
    }
}

private fun Resource.extractUri(property: Property): List<String>? {
    return this.listProperties(property)
        .toList()
        .mapNotNull { it.`object`.asUriResourceOrNull()?.uri }
        .takeIf { it.isNotEmpty() }
}

private fun RDFNode.asLiteralOrNull(): Literal? {
    return if (this.isLiteral) this.asLiteral() else null
}

private fun RDFNode.asResourceOrNull(): Resource? {
    return if (this.isResource) this.asResource() else null
}

private fun RDFNode.asUriResourceOrNull(): Resource? {
    return if (this.isURIResource && this.asResource().uri.isValidURI()) this.asResource() else null
}
