package no.fdk.concept_catalog.model

import com.fasterxml.jackson.annotation.JsonValue

enum class ForholdTilKildeEnum(val value: String) {
    EGENDEFINERT("egendefinert"),
    BASERTPAAKILDE("basertPaaKilde"),
    SITATFRAKILDE("sitatFraKilde");

    @JsonValue
    fun jsonValue(): String = value
}
