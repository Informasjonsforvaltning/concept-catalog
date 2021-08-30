package no.fdk.concept_catalog.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDate

@Document(collection = "begrep")
@JsonIgnoreProperties(ignoreUnknown = true)
data class Begrep (
    val id: String? = null,
    val versjonsnr: String? = null,
    val status: Status? = null,
    val anbefaltTerm: Term? = null,
    val tillattTerm: Map<String, List<String>>? = HashMap(),
    val frarådetTerm: Map<String, List<String>>? = HashMap(),
    val definisjon: Definisjon? = null,
    val kildebeskrivelse: Kildebeskrivelse? = null,
    val merknad: Map<String, List<String>>? = HashMap(),
    val ansvarligVirksomhet: Virksomhet? = null,
    val eksempel: Map<String, List<String>>? = HashMap(),
    val fagområde: Map<String, String>? = HashMap(),
    val bruksområde: Map<String, List<String>>? = HashMap(),
    val omfang: URITekst? = null,
    val kontaktpunkt: Kontaktpunkt? = null,
    val gyldigFom: LocalDate? = null,
    val gyldigTom: LocalDate? = null,
    val endringslogelement: Endringslogelement? = null,
    val seOgså: List<String>? = ArrayList()
)
