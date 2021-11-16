package no.fdk.concept_catalog.model

import com.fasterxml.jackson.annotation.JsonValue

enum class Status(val value: String) {
    UTKAST("utkast"),
    GODKJENT("godkjent"),
    HOERING("høring"),
    PUBLISERT("publisert");

    @JsonValue
    fun jsonValue(): String = value
}
