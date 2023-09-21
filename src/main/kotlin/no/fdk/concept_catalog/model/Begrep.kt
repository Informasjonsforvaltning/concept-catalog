package no.fdk.concept_catalog.model

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant
import java.time.LocalDate

@Document(collection = "begrep")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class BegrepDBO (
    val id: String,
    val originaltBegrep: String,
    val versjonsnr: SemVer,
    val revisjonAv: String?,
    val status: Status?,
    val statusURI: String? = null,
    val erPublisert: Boolean = false,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "Europe/Oslo")
    val publiseringsTidspunkt: Instant? = null,
    val anbefaltTerm: Term?,
    val tillattTerm: Map<String, List<String>>?,
    val frarådetTerm: Map<String, List<String>>?,
    val definisjon: Definisjon?,
    val definisjonForAllmennheten: Definisjon?,
    val rettsligForklaring: Definisjon?,
    val merknad: Map<String, String>?,
    val merkelapp: List<String>?,
    val ansvarligVirksomhet: Virksomhet,
    val eksempel: Map<String, String>?,
    val fagområde: Map<String, List<String>>?,
    val fagområdeKoder: List<String>?,
    val omfang: URITekst?,
    val kontaktpunkt: Kontaktpunkt?,
    val gyldigFom: LocalDate?,
    val gyldigTom: LocalDate?,
    val endringslogelement: Endringslogelement?,
    val opprettet: Instant? = null,
    val opprettetAv: String? = null,
    val seOgså: List<String>?,
    val erstattesAv: List<String>?,
    val assignedUser: String?,
    val abbreviatedLabel: String?,
    val begrepsRelasjon: List<BegrepsRelasjon>?,
    val interneFelt: Map<String, InterntFelt>?
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
    val statusURI: String? = null,
    val erPublisert: Boolean = false,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "Europe/Oslo")
    val publiseringsTidspunkt: Instant? = null,
    val gjeldendeRevisjon: String? = null,
    val anbefaltTerm: Term? = null,
    val tillattTerm: Map<String, List<String>>? = HashMap(),
    val frarådetTerm: Map<String, List<String>>? = HashMap(),
    val definisjon: Definisjon? = null,
    val definisjonForAllmennheten: Definisjon? = null,
    val rettsligForklaring: Definisjon? = null,
    val merknad: Map<String, String>? = HashMap(),
    val merkelapp: List<String>? = ArrayList(),
    val ansvarligVirksomhet: Virksomhet,
    val eksempel: Map<String, String>? = HashMap(),
    val fagområde: Map<String, List<String>>? = HashMap(),
    val fagområdeKoder: List<String>? = ArrayList(),
    val omfang: URITekst? = null,
    val kontaktpunkt: Kontaktpunkt? = null,
    val gyldigFom: LocalDate? = null,
    val gyldigTom: LocalDate? = null,
    val endringslogelement: Endringslogelement? = null,
    val opprettet: Instant? = null,
    val opprettetAv: String? = null,
    val seOgså: List<String>? = ArrayList(),
    val erstattesAv: List<String>? = ArrayList(),
    val assignedUser: String? = null,
    val abbreviatedLabel: String? = null,
    val begrepsRelasjon: List<BegrepsRelasjon>? = ArrayList(),
    val interneFelt: Map<String, InterntFelt>?
)

data class SemVer(val major: Int, val minor: Int, val patch: Int): Comparable<SemVer> {
    override fun compareTo(other: SemVer): Int =
        compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch })
}
