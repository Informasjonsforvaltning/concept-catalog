package no.fdk.concept_catalog.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonValue

@JsonIgnoreProperties(ignoreUnknown = true)
data class Kildebeskrivelse (
    val forholdTilKilde: ForholdTilKildeEnum?,
    val kilde: List<URITekst>? = ArrayList()
)

enum class ForholdTilKildeEnum(val value: String) {
    EGENDEFINERT("egendefinert"),
    BASERTPAAKILDE("basertPaaKilde"),
    SITATFRAKILDE("sitatFraKilde");

    @JsonValue
    fun jsonValue(): String = value
}
