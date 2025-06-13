package no.fdk.concept_catalog.rdf

import com.fasterxml.jackson.databind.ObjectMapper
import no.fdk.concept_catalog.model.*
import no.fdk.concept_catalog.service.createPatchOperations
import no.fdk.concept_catalog.service.isValidURI
import org.apache.jena.rdf.model.Literal
import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.Resource
import org.apache.jena.vocabulary.*
import java.time.LocalDate
import java.time.format.DateTimeParseException

private val SEM_VAR_REGEX = Regex("""^(\d+)\.(\d+)\.(\d+)$""")
private val EMAIL_REGEX = Regex("""^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$""")
private val TELEPHONE_REGEX = Regex("""^\+?[0-9\s\-()]{7,15}$""")

fun Resource.extract(originalConcept: BegrepDBO, objectMapper: ObjectMapper): ConceptExtraction {
    val versjonsnr = extractVersjonsnr()
    val statusUri = extractStatusUri()
    val anbefaltTerm = extractAnbefaltTerm()
    val tillattTerm = extractTillattTerm()
    val frarådetTerm = extractFrarådetTerm()
    val definisjon = extractDefinisjon()
    val definisjonForAllmennheten = extractDefinisjonForAllmennheten()
    val definisjonForSpesialister = extractDefinisjonForSpesialister()
    val merknad = extractMerknad()
    val eksempel = extractEksempel()
    val fagområde = extractFagområde()
    val omfang = extractOmfang()
    val gyldigFom = extractGyldigFom()
    val gyldigTom = extractGyldigTom()
    val kontaktPunkt = extractKontaktPunkt()
    val seOgså = extractSeOgså()
    val erstattesAv = extractErstattesAv()
    val begrepsRelasjon = extractBegrepsRelasjon()

    val updatedConcept = originalConcept.copy(
        versjonsnr = versjonsnr.first ?: originalConcept.versjonsnr,
        statusURI = statusUri.first ?: originalConcept.statusURI,
        anbefaltTerm = anbefaltTerm.first,
        tillattTerm = tillattTerm.first,
        frarådetTerm = frarådetTerm.first,
        definisjon = definisjon.first,
        definisjonForAllmennheten = definisjonForAllmennheten.first,
        definisjonForSpesialister = definisjonForSpesialister.first,
        merknad = merknad.first,
        eksempel = eksempel.first,
        fagområde = fagområde.first,
        omfang = omfang.first,
        gyldigFom = gyldigFom.first,
        gyldigTom = gyldigTom.first,
        kontaktpunkt = kontaktPunkt.first,
        seOgså = seOgså.first,
        erstattesAv = erstattesAv.first,
        begrepsRelasjon = begrepsRelasjon.first
    )

    val operations = createPatchOperations(originalConcept, updatedConcept, objectMapper)

    val issues = mutableListOf<Issue>().apply {
        listOf(versjonsnr.second,
        statusUri.second,
        anbefaltTerm.second,
        tillattTerm.second,
        frarådetTerm.second,
        definisjon.second,
        definisjonForAllmennheten.second,
        definisjonForSpesialister.second,
        merknad.second,
        eksempel.second,
        fagområde.second,
        omfang.second,
        gyldigFom.second,
        gyldigTom.second,
        kontaktPunkt.second,
        seOgså.second,
        erstattesAv.second,
        begrepsRelasjon.second
        ).flatten().forEach { add(it) }

        if (operations.isEmpty())
            add(
                Issue (IssueType.ERROR, "No JsonPatchOperations detected in the concept")
            )
    }

    val extractResult = ExtractResult(operations, issues)

    val extractionRecord = ExtractionRecord(
        externalId = uri,
        internalId = updatedConcept.id,
        extractResult = extractResult,
    )

    return ConceptExtraction(updatedConcept, extractionRecord)
}

private fun Resource.extractVersjonsnr(): Pair<SemVer?, List<Issue>> {
    val issues = mutableListOf<Issue>()

    val versionInfo = OWL.versionInfo

    val literal = getProperty(versionInfo)
        ?.`object`
        ?.asLiteralOrNull()
        ?.string
        ?.takeIf { it.isNotBlank() }

    if (literal == null) return null to issues

    val semVer = literal.let {
        SEM_VAR_REGEX.matchEntire(it)?.destructured?.let { (major, minor, patch) ->
            SemVer(major.toInt(), minor.toInt(), patch.toInt())
        }
    }

    if (semVer == null) {
        issues.add(Issue(IssueType.ERROR, "${versionInfo.localName}: Not acceptable format '$literal'"))
    }

    return semVer to issues
}

private fun Resource.extractStatusUri(): Pair<String?, List<Issue>> {
    return extractUri(EUVOC.status)
}

private fun Resource.extractAnbefaltTerm(): Pair<Term?, List<Issue>> {
    val prefLabel = SKOS.prefLabel
    val (localizedStrings, localizedStringsIssues) = extractLocalizedStrings(prefLabel)

    val issues = localizedStringsIssues.toMutableList()

    val term = if (localizedStrings.isNotEmpty()) {
        Term(localizedStrings)
    } else {
        issues += Issue(IssueType.ERROR, "${prefLabel.localName}: Required property")
        null
    }

    return term to issues
}

private fun Resource.extractTillattTerm(): Pair<Map<String, List<String>>, List<Issue>> {
    return extractLocalizedStringsAsGrouping(SKOS.altLabel)
}

private fun Resource.extractFrarådetTerm(): Pair<Map<String, List<String>>, List<Issue>> {
    return extractLocalizedStringsAsGrouping(SKOS.hiddenLabel)
}

private fun Resource.extractDefinisjon(): Pair<Definisjon?, List<Issue>> {
    return listProperties(EUVOC.xlDefinition)
        .asSequence()
        .mapNotNull { it.`object`.asResourceOrNull() }
        .firstOrNull { !it.hasProperty(DCTerms.audience) }
        ?.extractDefinition()
        ?: Pair(null, emptyList())
}

private fun Resource.extractDefinisjonForAllmennheten(): Pair<Definisjon?, List<Issue>> {
    return listProperties(EUVOC.xlDefinition)
        .asSequence()
        .mapNotNull { it.`object`.asResourceOrNull() }
        .firstOrNull { it.hasProperty(DCTerms.audience, AUDIENCE_TYPE.public) }
        ?.extractDefinition()
        ?: Pair(null, emptyList())
}

private fun Resource.extractDefinisjonForSpesialister(): Pair<Definisjon?, List<Issue>> {
    return listProperties(EUVOC.xlDefinition)
        .asSequence()
        .mapNotNull { it.`object`.asResourceOrNull() }
        .firstOrNull { it.hasProperty(DCTerms.audience, AUDIENCE_TYPE.specialist) }
        ?.extractDefinition()
        ?: Pair(null, emptyList())
}

private fun Resource.extractDefinition(): Pair<Definisjon?, List<Issue>> {
    val issues = mutableListOf<Issue>()

    val relationshipWithSource = SKOSNO.relationshipWithSource
    val xlDefinition = EUVOC.xlDefinition

    val relationship = when {
        hasProperty(relationshipWithSource, RELATIONSHIP.selfComposed)
            -> ForholdTilKildeEnum.EGENDEFINERT

        hasProperty(
            relationshipWithSource,
            RELATIONSHIP.directFromSource
        ) -> ForholdTilKildeEnum.BASERTPAAKILDE

        hasProperty(
            relationshipWithSource,
            RELATIONSHIP.derivedFromSource
        ) -> ForholdTilKildeEnum.SITATFRAKILDE

        else -> {
            issues.add(
                Issue(
                    IssueType.WARNING,
                    "${xlDefinition.localName}: Invalid type for '${relationshipWithSource.localName}'"
                )
            )

            null
        }
    }

    val source = listProperties(DCTerms.source)
        .toList()
        .mapNotNull { statement ->
            statement.`object`.let {
                when {
                    it.isLiteral -> it.asLiteralOrNull()?.string
                        ?.let { text -> URITekst(tekst = text) }

                    it.isURIResource -> it.asUriResourceOrNull()?.uri
                        ?.let { uri -> URITekst(uri = uri) }

                    else -> null
                }
            }
        }

    val sourceDescription: Kildebeskrivelse? = relationship?.let {
        Kildebeskrivelse(forholdTilKilde = relationship, kilde = source)
    }

    val value = RDF.value

    val (localizedStrings, localizedStringsIssues) = extractLocalizedStrings(value)
    issues += localizedStringsIssues

    if (localizedStrings.isEmpty()) {
        issues.add(Issue(IssueType.ERROR, "${xlDefinition.localName}: Missing '${value.localName}'"))
        return null to issues
    }

    return Definisjon(tekst = localizedStrings, kildebeskrivelse = sourceDescription) to issues
}

private fun Resource.extractMerknad(): Pair<Map<String, String>, List<Issue>> {
    return extractLocalizedStrings(SKOS.scopeNote)
}

private fun Resource.extractEksempel(): Pair<Map<String, String>, List<Issue>> {
    return extractLocalizedStrings(SKOS.example)
}

private fun Resource.extractFagområde(): Pair<Map<String, List<String>>, List<Issue>> {
    return extractLocalizedStringsAsGrouping(DCTerms.subject)
}

private fun Resource.extractOmfang(): Pair<URITekst?, List<Issue>> {
    val issues = mutableListOf<Issue>()

    val literal = listProperties(SKOSNO.valueRange)
        .asSequence()
        .firstOrNull { it.`object`.isLiteral }
        ?.let { it.`object`.asLiteralOrNull()?.string }

    val uri = listProperties(SKOSNO.valueRange)
        .asSequence()
        .firstOrNull { it.`object`.isURIResource }
        ?.let { it.`object`.asUriResourceOrNull()?.uri }

    if (literal == null && uri == null) return null to issues

    return URITekst(uri = uri, tekst = literal) to issues
}

private fun Resource.extractGyldigFom(): Pair<LocalDate?, List<Issue>> {
    return extractDate(EUVOC.startDate)
}

private fun Resource.extractGyldigTom(): Pair<LocalDate?, List<Issue>> {
    return extractDate(EUVOC.endDate)
}

private fun Resource.extractKontaktPunkt(): Pair<Kontaktpunkt?, List<Issue>> {
    val issues = mutableListOf<Issue>()

    val contactPoint = DCAT.contactPoint

    getProperty(contactPoint)
        ?.`object`?.asResourceOrNull()
        ?.apply {
            val hasEmail = VCARD4.hasEmail

            val email = this.getProperty(hasEmail)
                ?.`object`?.asUriResourceOrNull()?.toString()
                ?.removePrefix("mailto:")
                ?.takeIf {
                    val validEmail = EMAIL_REGEX.matches(it)

                    if (!validEmail) {
                        issues.add(
                            Issue(
                                IssueType.WARNING,
                                "${contactPoint.localName}: Invalid email for ${hasEmail.localName}: $it"
                            )
                        )
                    }

                    validEmail
                }

            val hasTelephone = VCARD4.hasTelephone

            val telephone = this.getProperty(hasTelephone)
                ?.let {
                    it.`object`.asUriResourceOrNull()?.toString() ?: it.`object`.asResourceOrNull()
                        ?.getProperty(VCARD4.hasValue)
                        ?.`object`
                        ?.asUriResourceOrNull()?.toString()
                }
                ?.removePrefix("tel:")
                ?.takeIf {
                    val validTelephone = TELEPHONE_REGEX.matches(it)

                    if (!validTelephone) {
                        issues.add(
                            Issue(
                                IssueType.WARNING,
                                "${contactPoint.localName}: Invalid telephone for ${hasTelephone.localName}: $it"
                            )
                        )
                    }

                    validTelephone
                }

            if (email != null || telephone != null) {
                return Kontaktpunkt(harEpost = email, harTelefon = telephone) to issues
            }
        }

    return null to issues
}

private fun Resource.extractSeOgså(): Pair<List<String>, List<Issue>> {
    return extractUris(RDFS.seeAlso)
}

private fun Resource.extractErstattesAv(): Pair<List<String>, List<Issue>> {
    return extractUris(DCTerms.isReplacedBy)
}

private fun Resource.extractBegrepsRelasjon(): Pair<List<BegrepsRelasjon>, List<Issue>> {
    val issues = mutableListOf<Issue>()

    val associativeConceptRelations = this.listProperties(SKOSNO.isFromConceptIn)
        .toList()
        .mapNotNull { it.`object`.asResourceOrNull() }
        .filter { it.hasProperty(RDF.type, SKOSNO.AssociativeConceptRelation) }
        .mapNotNull {
            val (localizedStrings, localizedStringsIssues) = it.extractLocalizedStrings(SKOSNO.relationRole)
            issues += localizedStringsIssues

            val toConcept = it.getProperty(SKOSNO.hasToConcept)
                ?.`object`
                ?.asUriResourceOrNull()
                ?.toString()

            BegrepsRelasjon(relasjon = "assosiativ", beskrivelse = localizedStrings, relatertBegrep = toConcept)
                .takeIf { localizedStrings.isNotEmpty() && toConcept != null }
        }

    val partitiveConceptRelations = this.listProperties(SKOSNO.hasPartitiveConceptRelation)
        .toList()
        .mapNotNull { it.`object`.asResourceOrNull() }
        .filter { it.hasProperty(RDF.type, SKOSNO.PartitiveConceptRelation) }
        .mapNotNull {
            val (localizedStrings, localizedStringsIssues) = it.extractLocalizedStrings(DCTerms.description)
            issues += localizedStringsIssues

            when {
                it.hasProperty(SKOSNO.hasPartitiveConcept) -> {
                    val partitiveConcept = it.getProperty(SKOSNO.hasPartitiveConcept)
                        ?.`object`
                        ?.asUriResourceOrNull()
                        ?.toString()

                    BegrepsRelasjon(
                        relasjon = "partitiv",
                        relasjonsType = "omfatter",
                        inndelingskriterium = localizedStrings,
                        relatertBegrep = partitiveConcept
                    ).takeIf { partitiveConcept != null }
                }

                it.hasProperty(SKOSNO.hasComprehensiveConcept) -> {
                    val comprehensiveConcept = it.getProperty(SKOSNO.hasComprehensiveConcept)
                        ?.`object`
                        ?.asUriResourceOrNull()
                        ?.toString()

                    BegrepsRelasjon(
                        relasjon = "partitiv",
                        relasjonsType = "erDelAv",
                        inndelingskriterium = localizedStrings,
                        relatertBegrep = comprehensiveConcept
                    ).takeIf { comprehensiveConcept != null }
                }

                else -> null
            }
        }

    val genericConceptRelations = this.listProperties(SKOSNO.hasGenericConceptRelation)
        .toList()
        .mapNotNull { it.`object`.asResourceOrNull() }
        .filter { it.hasProperty(RDF.type, SKOSNO.GenericConceptRelation) }
        .mapNotNull {
            val (localizedStrings, localizedStringsIssues) = it.extractLocalizedStrings(DCTerms.description)
            issues += localizedStringsIssues

            when {
                it.hasProperty(SKOSNO.hasGenericConcept) -> {
                    val genericConcept = it.getProperty(SKOSNO.hasGenericConcept)
                        ?.`object`
                        ?.asUriResourceOrNull()
                        ?.toString()

                    BegrepsRelasjon(
                        relasjon = "generisk",
                        relasjonsType = "overordnet",
                        inndelingskriterium = localizedStrings,
                        relatertBegrep = genericConcept
                    ).takeIf { genericConcept != null }
                }

                it.hasProperty(SKOSNO.hasSpecificConcept) -> {
                    val specificConcept = it.getProperty(SKOSNO.hasSpecificConcept)
                        ?.`object`
                        ?.asUriResourceOrNull()
                        ?.toString()

                    BegrepsRelasjon(
                        relasjon = "generisk",
                        relasjonsType = "underordnet",
                        inndelingskriterium = localizedStrings,
                        relatertBegrep = specificConcept
                    ).takeIf { specificConcept != null }
                }

                else -> null
            }
        }

    return listOf(associativeConceptRelations, partitiveConceptRelations, genericConceptRelations)
        .flatten()
        .let { it to issues }
}

private fun Resource.extractLocalizedStrings(property: Property): Pair<Map<String, String>, List<Issue>> {
    val issues = mutableListOf<Issue>()

    val literals = listProperties(property)
        .toList()
        .mapNotNull { it.`object`.asLiteralOrNull() }
        .filter {
            if (it.string.isNullOrBlank())
                return@filter false

            if (it.language.isNullOrBlank()) {
                issues.add(Issue(IssueType.ERROR, "${property.localName}: Missing language tag '${it.string}'"))

                return@filter false
            }

            true
        }
        .associate { it.language to it.string }

    return literals to issues
}

private fun Resource.extractLocalizedStringsAsGrouping(property: Property): Pair<Map<String, List<String>>, List<Issue>> {
    val issues = mutableListOf<Issue>()

    val literals = listProperties(property)
        .toList()
        .mapNotNull { it.`object`.asLiteralOrNull() }
        .filter {
            if (it.string.isNullOrBlank())
                return@filter false

            if (it.language.isNullOrBlank()) {
                issues.add(Issue(IssueType.ERROR, "Missing language tag for ${property.localName}: ${it.string}"))

                return@filter false
            }

            true
        }
        .groupBy { it.language }
        .mapValues { (_, literals) -> literals.map { it.string } }

    return literals to issues
}

private fun Resource.extractUri(property: Property): Pair<String?, List<Issue>> {
    val issues = mutableListOf<Issue>()

    val uri = getProperty(property)
        ?.`object`
        ?.asUriResourceOrNull()
        ?.uri

    if (uri != null && !uri.isValidURI()) {
        issues.add(Issue(IssueType.ERROR, "${property.localName}: Invalid URI '$uri'"))
    }

    return uri to issues
}

private fun Resource.extractUris(property: Property): Pair<List<String>, List<Issue>> {
    val issues = mutableListOf<Issue>()

    val uris = listProperties(property)
        .toList()
        .mapNotNull { it.`object`.asUriResourceOrNull()?.uri }
        .filter {
            if (!uri.isValidURI()) {
                issues.add(Issue(IssueType.ERROR, "${property.localName}: Invalid URI '$uri'"))

                return@filter false
            }

            true
        }

    return uris to issues
}

private fun Resource.extractDate(property: Property): Pair<LocalDate?, List<Issue>> {
    val issues = mutableListOf<Issue>()

    val date = getProperty(property)
        ?.`object`
        ?.asLiteralOrNull()
        ?.string
        ?.takeIf { it.isNotBlank() }

    date?.let {
        return try {
            LocalDate.parse(it) to issues
        } catch (e: DateTimeParseException) {
            issues.add(Issue(IssueType.ERROR, "${property.localName}: Invalid date format '$it'"))
            null to issues
        }
    }

    return null to issues
}

private fun RDFNode.asResourceOrNull(): Resource? {
    return if (isResource) asResource() else null
}

private fun RDFNode.asUriResourceOrNull(): Resource? {
    return if (isURIResource) asResource() else null
}

private fun RDFNode.asLiteralOrNull(): Literal? {
    return if (isLiteral) asLiteral() else null
}
