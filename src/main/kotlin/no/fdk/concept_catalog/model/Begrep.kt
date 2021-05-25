package no.fdk.concept_catalog.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDate
import kotlin.collections.HashMap

@Document(collection = "begrep")
@JsonIgnoreProperties(ignoreUnknown = true)
data class Begrep (
    val id: String? = null,
    val status: Status? = null,
    val anbefaltTerm: Term? = null,
    val tillattTerm: Map<String, List<String>> = HashMap(),
    val frarådetTerm: Map<String, List<String>> = HashMap(),
    val definisjon: Definisjon? = null,
    val kildebeskrivelse: Kildebeskrivelse? = null,
    val merknad: Map<String, List<String>> = HashMap(),
    val ansvarligVirksomhet: Virksomhet? = null,
    val eksempel: Map<String, List<String>> = HashMap(),
    val fagområde: Map<String, String> = HashMap(),
    val bruksområde: Map<String, List<String>> = HashMap(),
    val omfang: URITekst? = null,
    val kontaktpunkt: Kontaktpunkt? = null,
    @JsonSerialize(using = LocalDateSerializer::class)
    @JsonDeserialize(using = LocalDateDeserializer::class)
    val gyldigFom: LocalDate? = null,
    @JsonSerialize(using = LocalDateSerializer::class)
    @JsonDeserialize(using = LocalDateDeserializer::class)
    val gyldigTom: LocalDate? = null,
    val endringslogelement: Endringslogelement? = null,
    val seOgså: List<String> = ArrayList()
)
