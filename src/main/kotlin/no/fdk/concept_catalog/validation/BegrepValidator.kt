package no.fdk.concept_catalog.validation

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import no.fdk.concept_catalog.model.Begrep
import no.fdk.concept_catalog.model.BegrepDBO
import no.fdk.concept_catalog.model.Status
import no.fdk.concept_catalog.model.Virksomhet
import org.openapi4j.parser.OpenApi3Parser
import org.openapi4j.parser.model.v3.OpenApi3
import org.openapi4j.parser.model.v3.Schema
import org.openapi4j.schema.validator.ValidationData
import org.openapi4j.schema.validator.v3.SchemaValidator
import org.springframework.core.io.ClassPathResource
import java.io.StringReader
import java.time.LocalDate

private val mapper = ObjectMapper()
    .registerModule(JavaTimeModule())
    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    .setSerializationInclusion(JsonInclude.Include.NON_NULL)

private val openApi: OpenApi3? = OpenApi3Parser()
    .parse(ClassPathResource("specification/specification.yaml").url, true)

fun BegrepDBO.validateSchema() : ValidationData<Void> {
    val json = mapper.writeValueAsString(this)
    val schema = openApi!!.components.getSchema("Begrep")
    val validator = SchemaValidator(null, flattenSchema(schema).toNode())
    val validation = ValidationData<Void>()
    validator.validate(mapper.readTree(StringReader(json)), validation)
    return validation
}

fun Begrep.isValid(): Boolean = when {
    versjonsnr == null -> false
    versjonsnr.major == 0 -> false
    status == null -> false
    status == Status.UTKAST -> false
    anbefaltTerm == null -> false
    anbefaltTerm.navn.isNullOrEmpty() -> false
    !isValidTranslationsMap(anbefaltTerm.navn) -> false
    definisjon == null -> false
    definisjon.tekst.isNullOrEmpty() -> false
    !isValidTranslationsMap(definisjon.tekst) -> false
    ansvarligVirksomhet == null -> false
    !ansvarligVirksomhet.isValid() -> false
    !isValidValidityPeriod(gyldigFom, gyldigTom) -> false
    else -> true
}

private fun flattenSchema(schema: Schema): Schema {
    val copy = schema.copy().getFlatSchema(openApi?.context)
    copy.properties?.forEach { copy.properties[it.key] = flattenSchema(it.value) }
    copy.oneOfSchemas?.forEachIndexed { index, schema -> copy.oneOfSchemas[index] = flattenSchema(schema) }
    copy.allOfSchemas?.forEachIndexed { index, schema -> copy.allOfSchemas[index] = flattenSchema(schema) }
    copy.anyOfSchemas?.forEachIndexed { index, schema -> copy.anyOfSchemas[index] = flattenSchema(schema) }
    copy.additionalProperties = copy.additionalProperties?.let{ flattenSchema(it) }
    copy.itemsSchema = copy.itemsSchema?.let{ flattenSchema(it) }
    copy.notSchema = copy.notSchema?.let{ flattenSchema(it) }

    return copy
}

private fun Virksomhet.isValid(): Boolean = when {
    id.isNullOrBlank() -> false
    !id.isOrganizationNumber() -> false
    else -> true
}

private fun isValidTranslationsMap(translations: Map<String, Any>): Boolean = when {
    !translations.values.stream().anyMatch { it is String && it.isNotBlank() } -> false
    else -> true
}

private fun isValidValidityPeriod(validFrom: LocalDate?, validTo: LocalDate?): Boolean = when {
    validFrom != null && validTo != null && validFrom.isAfter(validTo) -> false
    else -> true
}

fun String.isOrganizationNumber(): Boolean {
    val regex = Regex("""^[0-9]{9}$""")
    return regex.containsMatchIn(this)
}
