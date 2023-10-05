package no.fdk.concept_catalog.model

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.*
import java.time.Instant
import java.time.LocalDate

@Document(indexName = "concepts-current")
@Setting(settingPath = "/elastic/settings.json")
@Mapping(mappingPath = "/elastic/mappings.json")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CurrentConcept(
    val idOfThisVersion: String,
    @Id val originaltBegrep: String,
    val versjonsnr: SemVer,
    val revisjonAv: String?,
    val status: Status?,
    val statusURI: String? = null,
    val erPublisert: Boolean = false,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "Europe/Oslo")
    @Field(type = FieldType.Date)
    val publiseringsTidspunkt: Instant? = null,
    val anbefaltTerm: Term?,
    val tillattTerm: Map<String, List<String>>?,
    val frarådetTerm: Map<String, List<String>>?,
    val definisjon: Definisjon?,
    val definisjonForAllmennheten: Definisjon?,
    val definisjonForSpesialister: Definisjon?,
    val merknad: Map<String, String>?,
    val merkelapp: List<String>?,
    val ansvarligVirksomhet: Virksomhet,
    val eksempel: Map<String, String>?,
    val fagområde: Map<String, List<String>>?,
    val fagområdeKoder: List<String>?,
    val omfang: URITekst?,
    val kontaktpunkt: Kontaktpunkt?,
    @Field(type = FieldType.Date)
    val gyldigFom: LocalDate?,
    @Field(type = FieldType.Date)
    val gyldigTom: LocalDate?,
    val endringslogelement: Endringslogelement?,
    @Field(type = FieldType.Date)
    val opprettet: Instant? = null,
    val opprettetAv: String? = null,
    val seOgså: List<String>?,
    val erstattesAv: List<String>?,
    val assignedUser: String?,
    val abbreviatedLabel: String?,
    val begrepsRelasjon: List<BegrepsRelasjon>?,
    val interneFelt: Map<String, InterntFelt>?
) {
    constructor(dbo: BegrepDBO) : this(
        dbo.id, dbo.originaltBegrep, dbo.versjonsnr, dbo.revisjonAv,
        dbo.status, dbo.statusURI, dbo.erPublisert, dbo.publiseringsTidspunkt,
        dbo.anbefaltTerm, dbo.tillattTerm, dbo.frarådetTerm, dbo.definisjon,
        dbo.definisjonForAllmennheten, dbo.definisjonForSpesialister,
        dbo.merknad, dbo.merkelapp, dbo.ansvarligVirksomhet, dbo.eksempel,
        dbo.fagområde, dbo.fagområdeKoder, dbo.omfang, dbo.kontaktpunkt,
        dbo.gyldigFom, dbo.gyldigTom, dbo.endringslogelement, dbo.opprettet,
        dbo.opprettetAv, dbo.seOgså, dbo.erstattesAv, dbo.assignedUser,
        dbo.abbreviatedLabel, dbo.begrepsRelasjon, dbo.interneFelt
    )

    fun toDBO(): BegrepDBO =
        BegrepDBO(
            idOfThisVersion, originaltBegrep, versjonsnr, revisjonAv,
            status, statusURI, erPublisert, publiseringsTidspunkt,
            anbefaltTerm, tillattTerm, frarådetTerm, definisjon,
            definisjonForAllmennheten, definisjonForSpesialister,
            merknad, merkelapp, ansvarligVirksomhet, eksempel,
            fagområde, fagområdeKoder, omfang, kontaktpunkt,
            gyldigFom, gyldigTom, endringslogelement, opprettet,
            opprettetAv, seOgså, erstattesAv, assignedUser,
            abbreviatedLabel, begrepsRelasjon, interneFelt
        )
}
