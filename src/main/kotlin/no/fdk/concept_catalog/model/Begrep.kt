package no.fdk.concept_catalog.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDate

@Document(collection = "begrep")
data class BegrepDBO (
    val id: String,
    val originaltBegrep: String,
    val versjonsnr: SemVer,
    val revisjonAv: String?,
    val status: Status?,
    val anbefaltTerm: Term?,
    val tillattTerm: Map<String, List<String>>?,
    val frarådetTerm: Map<String, List<String>>?,
    val definisjon: Definisjon?,
    val kildebeskrivelse: Kildebeskrivelse?,
    val merknad: Map<String, List<String>>?,
    val ansvarligVirksomhet: Virksomhet?,
    val eksempel: Map<String, List<String>>?,
    val fagområde: Map<String, String>?,
    val bruksområde: Map<String, List<String>>?,
    val omfang: URITekst?,
    val kontaktpunkt: Kontaktpunkt?,
    val gyldigFom: LocalDate?,
    val gyldigTom: LocalDate?,
    val endringslogelement: Endringslogelement?,
    val seOgså: List<String>?,
    val erstattesAv: List<String>?,
    val tildeltBruker: Bruker?,
    val begrepsRelasjon: List<BegrepsRelasjon>?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Begrep (
    val id: String? = null,
    val originaltBegrep: String? = null,
    val versjonsnr: SemVer? = null,
    val erSistPublisert: Boolean = false,
    val revisjonAvSistPublisert: Boolean = false,
    val revisjonAv: String? = null,
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
    val seOgså: List<String>? = ArrayList(),
    val erstattesAv: List<String>? = ArrayList(),
    val tildeltBruker: Bruker? = null,
    val begrepsRelasjon: List<BegrepsRelasjon>? = ArrayList()
)

data class SemVer(val major: Int, val minor: Int, val patch: Int): Comparable<SemVer> {
    override fun compareTo(other: SemVer): Int =
        compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch })
}
