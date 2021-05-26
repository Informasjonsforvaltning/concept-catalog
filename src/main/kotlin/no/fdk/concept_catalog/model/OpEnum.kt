package no.fdk.concept_catalog.model

import com.fasterxml.jackson.annotation.JsonValue

enum class OpEnum(val value: String) {
    ADD("add"),
    REMOVE("remove"),
    REPLACE("replace"),
    MOVE("move"),
    COPY("copy"),
    TEST("test");

    @JsonValue
    fun jsonValue(): String = value
}
