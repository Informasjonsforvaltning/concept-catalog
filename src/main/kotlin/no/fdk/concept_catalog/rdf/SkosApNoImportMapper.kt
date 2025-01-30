package no.fdk.concept_catalog.rdf

import no.fdk.concept_catalog.model.*
import org.apache.jena.rdf.model.*
import org.apache.jena.vocabulary.*
import java.time.LocalDate
import java.time.format.DateTimeParseException

private val SEM_VAR_REGEX = Regex("""^(\d+)\.(\d+)\.(\d+)$""")
private val EMAIL_REGEX = Regex("""^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$""")
private val TELEPHONE_REGEX = Regex("""^\+?[0-9\s\-()]{7,15}$""")

fun Model.extract(): List<ExtractionRecord> {
    return this.listResourcesWithProperty(RDF.type, SKOS.Concept)
        .toList()
        .mapNotNull { it.asUriResourceOrNull() }
        .map {
            val extractResult = sequenceOf(
                it.extractAnbefaltTerm(),
                it.extractTillattTerm(),
                it.extractFrarådetTerm(),
                it.extractVersjonr(),
                it.extractStatusUri(),
                it.extractDefinisjon(),
                it.extractDefinisjonForAllmennheten(),
                it.extractDefinisjonForSpesialister(),
                it.extractMerknad(),
                it.extractEksempel(),
                it.extractFagområde(),
                it.extractOmfang(),
                it.extractGyldigFom(),
                it.extractGyldigTom(),
                it.extractKontaktPunkt()
            ).merge()

            ExtractionRecord(externalId = it.uri, extractResult = extractResult)
        }
}

private fun Resource.extractVersjonr(): ExtractResult? {
    val issues = mutableSetOf<Issue>()
    val operations = mutableSetOf<JsonPatchOperation>()

    val versionInfo = OWL.versionInfo

    getProperty(versionInfo)
        ?.`object`?.asLiteralOrNull()?.string
        ?.takeIf { it.isNotBlank() }
        ?.apply {
            SEM_VAR_REGEX.matchEntire(this)?.destructured
                ?.let { (major, minor, patch) ->
                    operations.add(
                        JsonPatchOperation(
                            op = OpEnum.ADD,
                            path = "/versjonsnr",
                            value = SemVer(major.toInt(), minor.toInt(), patch.toInt())
                        )
                    )
                } ?: issues.add(Issue(IssueType.ERROR, "Not acceptable format for ${versionInfo.localName}: $this"))
        }

    return ExtractResult(operations, issues)
        .takeIf { operations.isNotEmpty() || issues.isNotEmpty() }
}

private fun Resource.extractStatusUri(): ExtractResult? {
    val issues = mutableSetOf<Issue>()
    val operations = mutableSetOf<JsonPatchOperation>()

    val status = EUVOC.status

    getProperty(status)
        ?.apply {
            this.`object`
                .takeIf { it.isURIResource }
                ?.asUriResourceOrNull()
                ?.uri
                ?.let { uri ->
                    operations.add(
                        JsonPatchOperation(
                            op = OpEnum.ADD,
                            path = "/statusURI",
                            value = uri
                        )
                    )
                }
                ?: issues.add(Issue(IssueType.ERROR, "Invalid URI for ${status.localName}: ${this.`object`}"))
        }

    return ExtractResult(operations, issues)
        .takeIf { (operations.isNotEmpty() || issues.isNotEmpty()) }
}

private fun Resource.extractAnbefaltTerm(): ExtractResult? {
    val issues = mutableSetOf<Issue>()
    val operations = mutableSetOf<JsonPatchOperation>()

    val prefLabel = SKOS.prefLabel

    val localizedStrings: Map<String, String>? = extractLocalizedStrings(prefLabel, issues)

    if (localizedStrings.isNullOrEmpty()) {
        issues.add(Issue(IssueType.ERROR, "Missing ${prefLabel.localName}"))
    } else {
        operations.add(
            JsonPatchOperation(
                op = OpEnum.ADD,
                path = "/anbefaltTerm",
                value = Term(localizedStrings)
            )
        )
    }

    return ExtractResult(operations, issues)
        .takeIf { operations.isNotEmpty() || issues.isNotEmpty() }
}

private fun Resource.extractTillattTerm(): ExtractResult? {
    val issues = mutableSetOf<Issue>()
    val operations = mutableSetOf<JsonPatchOperation>()

    extractLocalizedStringsAsGrouping(SKOS.altLabel, issues)
        ?.apply {
            operations.add(
                JsonPatchOperation(
                    op = OpEnum.ADD,
                    path = "/tillattTerm",
                    value = this
                )
            )
        }

    return ExtractResult(operations, issues)
        .takeIf { operations.isNotEmpty() || issues.isNotEmpty() }
}


private fun Resource.extractFrarådetTerm(): ExtractResult? {
    val issues = mutableSetOf<Issue>()
    val operations = mutableSetOf<JsonPatchOperation>()

    extractLocalizedStringsAsGrouping(SKOS.hiddenLabel, issues)
        ?.apply {
            operations.add(
                JsonPatchOperation(
                    op = OpEnum.ADD,
                    path = "/frarådetTerm",
                    value = this
                )
            )
        }

    return ExtractResult(operations, issues)
        .takeIf { operations.isNotEmpty() || issues.isNotEmpty() }
}

private fun Resource.extractDefinisjon(): ExtractResult? {
    return listProperties(EUVOC.xlDefinition)
        .toList()
        .mapNotNull { it.`object`.asResourceOrNull() }
        .filterNot { it.hasProperty(DCTerms.audience) }
        .firstNotNullOfOrNull { it.extractDefinition("/definisjon") }
}

private fun Resource.extractDefinisjonForAllmennheten(): ExtractResult? {
    return listProperties(EUVOC.xlDefinition)
        .toList()
        .mapNotNull { it.`object`.asResourceOrNull() }
        .filter { it.hasProperty(DCTerms.audience, AUDIENCE_TYPE.public) }
        .firstNotNullOfOrNull { it.extractDefinition("/definisjonForAllmennheten") }
}

private fun Resource.extractDefinisjonForSpesialister(): ExtractResult? {
    return listProperties(EUVOC.xlDefinition)
        .toList()
        .mapNotNull { it.`object`.asResourceOrNull() }
        .filter { it.hasProperty(DCTerms.audience, AUDIENCE_TYPE.specialist) }
        .firstNotNullOfOrNull { it.extractDefinition("/definisjonForSpesialister") }
}

private fun Resource.extractDefinition(path: String): ExtractResult? {
    val issues = mutableSetOf<Issue>()
    val operations = mutableSetOf<JsonPatchOperation>()

    val relationshipWithSource = SKOSNO.relationshipWithSource
    val xlDefinition = EUVOC.xlDefinition

    val relationship: ForholdTilKildeEnum? = when {
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
                    "[${xlDefinition.localName}] Invalid type for ${relationshipWithSource.localName}"
                )
            )

            null
        }
    }

    val source: List<URITekst>? = listProperties(DCTerms.source)
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
        .takeIf { it.isNotEmpty() }

    val sourceDescription: Kildebeskrivelse? = relationship?.let {
        Kildebeskrivelse(forholdTilKilde = relationship, kilde = source)
    }

    val value = RDF.value

    val localizedStrings: Map<String, String>? = extractLocalizedStrings(value, issues)

    if (localizedStrings.isNullOrEmpty()) {
        issues.add(Issue(IssueType.ERROR, "[${xlDefinition.localName}] Missing ${value.localName}"))
    } else {
        operations.add(
            JsonPatchOperation(
                op = OpEnum.ADD,
                path = path,
                value = Definisjon(tekst = localizedStrings, kildebeskrivelse = sourceDescription)
            )
        )
    }

    return ExtractResult(operations, issues)
        .takeIf { operations.isNotEmpty() || issues.isNotEmpty() }
}

private fun Resource.extractMerknad(): ExtractResult? {
    val issues = mutableSetOf<Issue>()
    val operations = mutableSetOf<JsonPatchOperation>()

    extractLocalizedStrings(SKOS.scopeNote, issues)
        ?.apply {
            operations.add(
                JsonPatchOperation(
                    op = OpEnum.ADD,
                    path = "/merknad",
                    value = this
                )
            )
        }

    return ExtractResult(operations, issues)
        .takeIf { operations.isNotEmpty() || issues.isNotEmpty() }
}

private fun Resource.extractEksempel(): ExtractResult? {
    val issues = mutableSetOf<Issue>()
    val operations = mutableSetOf<JsonPatchOperation>()

    extractLocalizedStrings(SKOS.example, issues)
        ?.apply {
            operations.add(
                JsonPatchOperation(
                    op = OpEnum.ADD,
                    path = "/eksempel",
                    value = this
                )
            )
        }

    return ExtractResult(operations, issues)
        .takeIf { operations.isNotEmpty() || issues.isNotEmpty() }
}

private fun Resource.extractFagområde(): ExtractResult? {
    val issues = mutableSetOf<Issue>()
    val operations = mutableSetOf<JsonPatchOperation>()

    extractLocalizedStringsAsGrouping(DCTerms.subject, issues)
        ?.apply {
            operations.add(
                JsonPatchOperation(
                    op = OpEnum.ADD,
                    path = "/fagområde",
                    value = this
                )
            )
        }

    return ExtractResult(operations, issues)
        .takeIf { operations.isNotEmpty() || issues.isNotEmpty() }
}

private fun Resource.extractOmfang(): ExtractResult? {
    val issues = mutableSetOf<Issue>()
    val operations = mutableSetOf<JsonPatchOperation>()

    val text = listProperties(SKOSNO.valueRange)
        .toList()
        .firstOrNull { it.`object`.isLiteral }
        ?.let { it.`object`.asLiteralOrNull()?.string }

    val uri = listProperties(SKOSNO.valueRange)
        .toList()
        .firstOrNull { it.`object`.isURIResource }
        ?.let { it.`object`.asUriResourceOrNull()?.uri }

    if (text != null || uri != null) {
        operations.add(
            JsonPatchOperation(
                op = OpEnum.ADD,
                path = "/omfang",
                value = URITekst(uri, text)
            )
        )
    }

    return ExtractResult(operations, issues)
        .takeIf { operations.isNotEmpty() || issues.isNotEmpty() }
}

private fun Resource.extractGyldigFom(): ExtractResult? {
    val issues = mutableSetOf<Issue>()
    val operations = mutableSetOf<JsonPatchOperation>()

    val startDate = EUVOC.startDate

    getProperty(startDate)
        ?.let { it.`object`.asLiteralOrNull()?.string }
        ?.takeIf { it.isNotBlank() }
        ?.apply {
            if (!isValidDate(this)) {
                issues.add(
                    Issue(IssueType.ERROR, "Invalid date for ${startDate.localName}: $this")
                )
            } else {
                operations.add(
                    JsonPatchOperation(
                        op = OpEnum.ADD,
                        path = "/gyldigFom",
                        value = LocalDate.parse(this)
                    )
                )
            }
        }

    return ExtractResult(operations, issues)
        .takeIf { operations.isNotEmpty() || issues.isNotEmpty() }
}

private fun Resource.extractGyldigTom(): ExtractResult? {
    val issues = mutableSetOf<Issue>()
    val operations = mutableSetOf<JsonPatchOperation>()

    val endDate = EUVOC.endDate

    getProperty(endDate)
        ?.let { it.`object`.asLiteralOrNull()?.string }
        ?.takeIf { it.isNotBlank() }
        ?.apply {
            if (!isValidDate(this)) {
                issues.add(
                    Issue(IssueType.ERROR, "Invalid date for ${endDate.localName}: $this")
                )
            } else {
                operations.add(
                    JsonPatchOperation(
                        op = OpEnum.ADD,
                        path = "/gyldigTom",
                        value = LocalDate.parse(this)
                    )
                )
            }
        }

    return ExtractResult(operations, issues)
        .takeIf { operations.isNotEmpty() || issues.isNotEmpty() }
}

private fun Resource.extractKontaktPunkt(): ExtractResult? {
    val issues = mutableSetOf<Issue>()
    val operations = mutableSetOf<JsonPatchOperation>()

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
                                "[${contactPoint.localName}] Invalid email for ${hasEmail.localName}: $it"
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
                                "[${contactPoint.localName}] Invalid telephone for ${hasTelephone.localName}: $it"
                            )
                        )
                    }

                    validTelephone
                }

            if (email != null || telephone != null) {
                operations.add(
                    JsonPatchOperation(
                        op = OpEnum.ADD,
                        path = "/kontaktpunkt",
                        value = Kontaktpunkt(harEpost = email, harTelefon = telephone)
                    )
                )
            }
        }

    return ExtractResult(operations, issues)
        .takeIf { operations.isNotEmpty() || issues.isNotEmpty() }
}

private fun Resource.extractLocalizedStrings(property: Property, issues: MutableSet<Issue>): Map<String, String>? {
    return listProperties(property)
        .toList()
        .mapNotNull { it.`object`.asLiteralOrNull() }
        .filter {
            if (it.string.isNullOrBlank())
                return@filter false

            if (it.language.isNullOrBlank()) {
                issues.add(Issue(IssueType.WARNING, "Missing language tag for ${property.localName}: ${it.string}"))

                return@filter false
            }

            true
        }
        .associate { it.language to it.string }
        .takeIf { it.isNotEmpty() }
}

private fun Resource.extractLocalizedStringsAsGrouping(
    property: Property,
    issues: MutableSet<Issue>
): Map<String, List<String>>? {
    return listProperties(property)
        .toList()
        .mapNotNull { it.`object`.asLiteralOrNull() }
        .filter {
            if (it.string.isNullOrBlank())
                return@filter false

            if (it.language.isNullOrBlank()) {
                issues.add(Issue(IssueType.WARNING, "Missing language tag for ${property.localName}: ${it.string}"))

                return@filter false
            }

            true
        }
        .groupBy { it.language }
        .mapValues { (_, literals) -> literals.map { it.string } }
        .takeIf { it.isNotEmpty() }
}

private fun isValidDate(dateString: String): Boolean =
    try {
        LocalDate.parse(dateString)
        true
    } catch (e: DateTimeParseException) {
        false
    }

private fun RDFNode.asLiteralOrNull(): Literal? {
    return if (isLiteral) asLiteral() else null
}

private fun RDFNode.asResourceOrNull(): Resource? {
    return if (isResource) asResource() else null
}

private fun RDFNode.asUriResourceOrNull(): Resource? {
    return if (isURIResource) asResource() else null
}
