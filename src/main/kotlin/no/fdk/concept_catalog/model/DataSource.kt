package no.fdk.concept_catalog.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue

@JsonIgnoreProperties(ignoreUnknown = true)
data class DataSource (
    @param:JsonProperty("dataType")
    val dataType: DataType,
    @param:JsonProperty("dataSourceType")
    val dataSourceType: DataSourceType,
    @param:JsonProperty("url")
    val url: String,
    @param:JsonProperty("acceptHeaderValue")
    val acceptHeaderValue: String,
    @param:JsonProperty("publisherId")
    val publisherId: String,
    @param:JsonProperty("description")
    val description: String
)

enum class DataType(val value: String) {
    CONCEPT("concept");

    @JsonValue
    fun jsonValue(): String = value
}

enum class DataSourceType(val value: String) {
    SKOS_AP_NO("SKOS-AP-NO"), DCAT_AP_NO("DCAT-AP-NO");

    @JsonValue
    fun jsonValue(): String = value
}
