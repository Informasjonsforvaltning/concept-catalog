package no.fdk.concept_catalog.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate
import kotlin.collections.HashMap

@JsonIgnoreProperties(ignoreUnknown = true)
data class Begrep (
    val id: String? = null,
    val status: Status? = null,
    val anbefaltTerm: Term? = null,
    val tillattTerm: Map<String, String> = HashMap(),
    val frarådetTerm: Map<String, String> = HashMap(),
    val definisjon: Definisjon? = null,
    val kildebeskrivelse: Kildebeskrivelse? = null,
    val merknad: Map<String, String>? = HashMap(),
    val ansvarligVirksomhet: Virksomhet? = null,
    val eksempel: Map<String, String> = HashMap(),
    val fagområde: Map<String, String> = HashMap(),
    val bruksområde: Map<String, String> = HashMap(),
    val omfang: URITekst? = null,
    val kontaktpunkt: Kontaktpunkt? = null,
    val gyldigFom: LocalDate? = null,
    val gyldigTom: LocalDate? = null,
    val endringslogelement: Endringslogelement? = null,
    val seOgså: List<String> = ArrayList()
)
