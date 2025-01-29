package no.fdk.concept_catalog.rdf

import no.fdk.concept_catalog.model.*
import org.apache.jena.rdf.model.Literal
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.Resource
import org.apache.jena.vocabulary.*
import java.time.LocalDate
import java.time.format.DateTimeParseException

private val SEM_VAR_REGEX = Regex("""^(\d+)\.(\d+)\.(\d+)$""")
private val EMAIL_REGEX = Regex("""^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$""")
private val TELEPHONE_REGEX = Regex("""^\+?[0-9\s\-()]{7,15}$""")

fun Model.extract(): List<ExtractionRecord> {
    return this.listResourcesWithProperty(RDF.type, SKOS.Concept)
        .toList()
        .mapNotNull { it.asResourceOrNull() }
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

    this.getProperty(OWL.versionInfo)
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
                } ?: issues.add(Issue(IssueType.ERROR, "Not acceptable format for owl:versionInfo: $this"))
        }

    return ExtractResult(operations, issues)
        .takeIf { operations.isNotEmpty() || issues.isNotEmpty() }
}

private fun Resource.extractStatusUri(): ExtractResult? {
    val issues = mutableSetOf<Issue>()
    val operations = mutableSetOf<JsonPatchOperation>()

    this.getProperty(EUVOC.status)
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
                ?: issues.add(Issue(IssueType.ERROR, "Invalid URI for euvoc:status: ${this.`object`}"))
        }

    return ExtractResult(operations, issues)
        .takeIf { (operations.isNotEmpty() || issues.isNotEmpty()) }
}

private fun Resource.extractAnbefaltTerm(): ExtractResult? {
    val issues = mutableSetOf<Issue>()
    val operations = mutableSetOf<JsonPatchOperation>()

    val localizedStrings: Map<String, String>? = this.listProperties(SKOS.prefLabel)
        .toList()
        .mapNotNull { it.`object`.asLiteralOrNull() }
        .filter {
            if (it.string.isNullOrBlank())
                return@filter false

            if (it.language.isNullOrBlank()) {
                issues.add(Issue(IssueType.WARNING, "Missing language tag for skos:prefLabel: ${it.string}"))

                return@filter false
            }

            true
        }
        .associate { it.language to it.string }
        .takeIf { it.isNotEmpty() }

    if (localizedStrings.isNullOrEmpty()) {
        issues.add(Issue(IssueType.ERROR, "Missing skos:prefLabel"))
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

    val localizedStrings: Map<String, List<String>>? = this.listProperties(SKOS.altLabel)
        .toList()
        .mapNotNull { it.`object`.asLiteralOrNull() }
        .filter {
            if (it.string.isNullOrBlank())
                return@filter false

            if (it.language.isNullOrBlank()) {
                issues.add(Issue(IssueType.WARNING, "Missing language tag for skos:altLabel: ${it.string}"))

                return@filter false
            }

            true
        }
        .groupBy { it.language }
        .mapValues { (_, literals) -> literals.map { it.string } }
        .takeIf { it.isNotEmpty() }

    if (!localizedStrings.isNullOrEmpty()) {
        operations.add(
            JsonPatchOperation(
                op = OpEnum.ADD,
                path = "/tillattTerm",
                value = localizedStrings
            )
        )
    }

    return ExtractResult(operations, issues)
        .takeIf { operations.isNotEmpty() || issues.isNotEmpty() }
}

private fun Resource.extractFrarådetTerm(): ExtractResult? {
    val issues = mutableSetOf<Issue>()
    val operations = mutableSetOf<JsonPatchOperation>()

    val localizedStrings: Map<String, List<String>>? = this.listProperties(SKOS.hiddenLabel)
        .toList()
        .mapNotNull { it.`object`.asLiteralOrNull() }
        .filter {
            if (it.string.isNullOrBlank())
                return@filter false

            if (it.language.isNullOrBlank()) {
                issues.add(Issue(IssueType.WARNING, "Missing language tag for skos:hiddenLabel: ${it.string}"))

                return@filter false
            }

            true
        }
        .groupBy { it.language }
        .mapValues { (_, literals) -> literals.map { it.string } }
        .takeIf { it.isNotEmpty() }

    if (!localizedStrings.isNullOrEmpty()) {
        operations.add(
            JsonPatchOperation(
                op = OpEnum.ADD,
                path = "/frarådetTerm",
                value = localizedStrings
            )
        )
    }

    return ExtractResult(operations, issues)
        .takeIf { operations.isNotEmpty() || issues.isNotEmpty() }
}

private fun Resource.extractDefinisjon(): ExtractResult? {
    return this.listProperties(EUVOC.xlDefinition)
        .toList()
        .mapNotNull { it.`object`.asResourceOrNull() }
        .filterNot { it.hasProperty(DCTerms.audience) }
        .firstNotNullOfOrNull { it.extractDefinition("/definisjon") }
}

private fun Resource.extractDefinisjonForAllmennheten(): ExtractResult? {
    return this.listProperties(EUVOC.xlDefinition)
        .toList()
        .mapNotNull { it.`object`.asResourceOrNull() }
        .filter { it.hasProperty(DCTerms.audience, AUDIENCE_TYPE.public) }
        .firstNotNullOfOrNull { it.extractDefinition("/definisjonForAllmennheten") }
}

private fun Resource.extractDefinisjonForSpesialister(): ExtractResult? {
    return this.listProperties(EUVOC.xlDefinition)
        .toList()
        .mapNotNull { it.`object`.asResourceOrNull() }
        .filter { it.hasProperty(DCTerms.audience, AUDIENCE_TYPE.specialist) }
        .firstNotNullOfOrNull { it.extractDefinition("/definisjonForSpesialister") }
}

private fun Resource.extractDefinition(path: String): ExtractResult? {
    val issues = mutableSetOf<Issue>()
    val operations = mutableSetOf<JsonPatchOperation>()

    val relationshipWithSource: ForholdTilKildeEnum? = when {
        this.hasProperty(SKOSNO.relationshipWithSource, RELATIONSHIP.selfComposed)
            -> ForholdTilKildeEnum.EGENDEFINERT

        this.hasProperty(
            SKOSNO.relationshipWithSource,
            RELATIONSHIP.directFromSource
        ) -> ForholdTilKildeEnum.BASERTPAAKILDE

        this.hasProperty(
            SKOSNO.relationshipWithSource,
            RELATIONSHIP.derivedFromSource
        ) -> ForholdTilKildeEnum.SITATFRAKILDE

        else -> {
            issues.add(
                Issue(IssueType.WARNING, "[euvoc:xlDefinition] Invalid type for skosno:relationshipWithSource")
            )

            null
        }
    }

    val source: List<URITekst>? = this.listProperties(DCTerms.source)
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

    val sourceDescription: Kildebeskrivelse? = relationshipWithSource?.let {
        Kildebeskrivelse(forholdTilKilde = relationshipWithSource, kilde = source)
    }

    val localizedStrings: Map<String, String>? = this.listProperties(RDF.value)
        .toList()
        .mapNotNull { it.`object`.asLiteralOrNull() }
        .filter {
            if (it.string.isNullOrBlank())
                return@filter false

            if (it.language.isNullOrBlank()) {
                issues.add(
                    Issue(
                        IssueType.WARNING,
                        "[euvoc:xlDefinition] Missing language tag for rdf:value: ${it.string}"
                    )
                )

                return@filter false
            }

            true
        }
        .associate { it.language to it.string }
        .takeIf { it.isNotEmpty() }

    if (localizedStrings.isNullOrEmpty()) {
        issues.add(Issue(IssueType.ERROR, "[euvoc:xlDefinition] Missing rdf:value"))
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

    this.listProperties(SKOS.scopeNote)
        .toList()
        .mapNotNull { it.`object`.asLiteralOrNull() }
        .filter {
            if (it.string.isNullOrBlank())
                return@filter false

            if (it.language.isNullOrBlank()) {
                issues.add(Issue(IssueType.WARNING, "Missing language tag for skos:scopeNote: ${it.string}"))

                return@filter false
            }

            true
        }
        .associate { it.language to it.string }
        .takeIf { it.isNotEmpty() }
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

    this.listProperties(SKOS.example)
        .toList()
        .mapNotNull { it.`object`.asLiteralOrNull() }
        .filter {
            if (it.string.isNullOrBlank())
                return@filter false

            if (it.language.isNullOrBlank()) {
                issues.add(Issue(IssueType.WARNING, "Missing language tag for skos:example: ${it.string}"))

                return@filter false
            }

            true
        }
        .associate { it.language to it.string }
        .takeIf { it.isNotEmpty() }
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

    this.listProperties(DCTerms.subject)
        .toList()
        .mapNotNull { it.`object`.asLiteralOrNull() }
        .filter {
            if (it.string.isNullOrBlank())
                return@filter false

            if (it.language.isNullOrBlank()) {
                issues.add(Issue(IssueType.WARNING, "Missing language tag for dct:subject: ${it.string}"))

                return@filter false
            }

            true
        }
        .groupBy { it.language }
        .mapValues { (_, literals) -> literals.map { it.string } }
        .takeIf { it.isNotEmpty() }
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

    val text = this.listProperties(SKOSNO.valueRange)
        .toList()
        .firstOrNull { it.`object`.isLiteral }
        ?.let { it.`object`.asLiteralOrNull()?.string }

    val uri = this.listProperties(SKOSNO.valueRange)
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

    this.getProperty(EUVOC.startDate)
        ?.let { it.`object`.asLiteralOrNull()?.string }
        ?.takeIf { it.isNotBlank() }
        ?.apply {
            if (!isValidDate(this)) {
                issues.add(
                    Issue(IssueType.ERROR, "Invalid date for euvoc:startDate: $this")
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

    this.getProperty(EUVOC.endDate)
        ?.let { it.`object`.asLiteralOrNull()?.string }
        ?.takeIf { it.isNotBlank() }
        ?.apply {
            if (!isValidDate(this)) {
                issues.add(
                    Issue(IssueType.ERROR, "Invalid date for euvoc:endDate: $this")
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

private fun isValidDate(dateString: String): Boolean =
    try {
        LocalDate.parse(dateString)
        true
    } catch (e: DateTimeParseException) {
        false
    }

private fun Resource.extractKontaktPunkt(): ExtractResult? {
    val issues = mutableSetOf<Issue>()
    val operations = mutableSetOf<JsonPatchOperation>()

    this.getProperty(DCAT.contactPoint)
        ?.`object`?.asResourceOrNull()
        ?.apply {
            val email = this.getProperty(VCARD4.hasEmail)
                ?.`object`?.asUriResourceOrNull()?.toString()
                ?.removePrefix("mailto:")
                ?.takeIf {
                    val validEmail = EMAIL_REGEX.matches(it)

                    if (!validEmail) {
                        issues.add(
                            Issue(IssueType.WARNING, "[dcat:contactPoint] Invalid email for vcard:hasEmail: $it")
                        )
                    }

                    validEmail
                }

            val telephone = this.getProperty(VCARD4.hasTelephone)
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
                                "[dcat:contactPoint] Invalid telephone for vcard:hasTelephone: $it"
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

private fun RDFNode.asLiteralOrNull(): Literal? {
    return if (this.isLiteral) asLiteral() else null
}

private fun RDFNode.asResourceOrNull(): Resource? {
    return if (this.isResource) asResource() else null
}

private fun RDFNode.asUriResourceOrNull(): Resource? {
    return if (this.isURIResource) asResource() else null
}
