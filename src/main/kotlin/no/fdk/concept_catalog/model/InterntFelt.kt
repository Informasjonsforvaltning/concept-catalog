package no.fdk.concept_catalog.model

import com.fasterxml.jackson.annotation.JsonValue

data class InterntFelt(
    val fieldId: String,
    val type: FeltType,
    val location: FeltLokasjon,
    val label: Map<String, String>,
    val description: Map<String, String>,
    val codeListId: String?,
    val value: String?
)

enum class FeltType(private val value: String) {
    BOOLEAN("boolean"),
    TEXT_SHORT("text_short"),
    TEXT_LONG("text_long"),
    CODE_LIST("code_list"),
    USER_LIST("user_list");

    @JsonValue
    fun jsonValue(): String = value
}

enum class FeltLokasjon(private val value: String) {
    MAIN_COLUMN("main_column"),
    RIGHT_COLUMN("right_column");

    @JsonValue
    fun jsonValue(): String = value
}
