package no.fdk.concept_catalog.service

import no.fdk.concept_catalog.model.*
import no.fdk.concept_catalog.rdf.CONCEPT_STATUS
import java.time.Instant
import java.time.ZonedDateTime
import java.util.*

val NEW_CONCEPT_VERSION = SemVer(0, 1, 0)

fun BegrepDBO.toDTO(): Begrep =
    Begrep(
        id,
        originaltBegrep,
        versjonsnr,
        sistPublisertId = null,
        revisjonAv,
        status,
        statusURI,
        erPublisert,
        publiseringsTidspunkt,
        anbefaltTerm,
        tillattTerm,
        frarådetTerm,
        definisjon,
        definisjonForAllmennheten,
        definisjonForSpesialister,
        merknad,
        merkelapp,
        ansvarligVirksomhet,
        eksempel,
        fagområde,
        fagområdeKoder?.filterNotNull(),
        omfang,
        kontaktpunkt,
        gyldigFom,
        gyldigTom,
        endringslogelement,
        opprettet,
        opprettetAv,
        seOgså,
        internSeOgså,
        erstattesAv,
        assignedUser,
        abbreviatedLabel,
        begrepsRelasjon,
        internBegrepsRelasjon,
        interneFelt,
        internErstattesAv
    )

fun BegrepDBO.addUpdatableFieldsFromDTO(dto: Begrep) =
    copy(
        originaltBegrep = dto.originaltBegrep ?: originaltBegrep,
        status = dto.status,
        statusURI = dto.statusURI,
        versjonsnr = dto.versjonsnr ?: versjonsnr,
        anbefaltTerm = dto.anbefaltTerm,
        tillattTerm = dto.tillattTerm,
        frarådetTerm = dto.frarådetTerm,
        definisjon = dto.definisjon,
        definisjonForAllmennheten = dto.definisjonForAllmennheten,
        definisjonForSpesialister = dto.definisjonForSpesialister,
        merknad = dto.merknad,
        merkelapp = dto.merkelapp,
        eksempel = dto.eksempel,
        fagområde = dto.fagområde,
        fagområdeKoder = dto.fagområdeKoder,
        omfang = dto.omfang,
        kontaktpunkt = dto.kontaktpunkt,
        gyldigFom = dto.gyldigFom,
        gyldigTom = dto.gyldigTom,
        seOgså = dto.seOgså,
        internSeOgså = dto.internSeOgså,
        erstattesAv = dto.erstattesAv,
        assignedUser = dto.assignedUser,
        abbreviatedLabel = dto.abbreviatedLabel,
        begrepsRelasjon = dto.begrepsRelasjon,
        internBegrepsRelasjon = dto.internBegrepsRelasjon,
        interneFelt = dto.interneFelt,
        internErstattesAv = dto.internErstattesAv
    )

fun BegrepDBO.updateLastChangedAndByWhom(user: User): BegrepDBO =
    copy(
        endringslogelement = Endringslogelement(
            endringstidspunkt = ZonedDateTime.now().toInstant(),
            endretAv = user.name
        )
    )

fun incrementSemVer(semVer: SemVer?): SemVer =
    SemVer(
        major = semVer?.major ?: NEW_CONCEPT_VERSION.major,
        minor = semVer?.minor ?: NEW_CONCEPT_VERSION.minor,
        patch = semVer?.patch?.let { it + 1 } ?: NEW_CONCEPT_VERSION.patch
    )

fun BegrepDBO.createNewRevision(): BegrepDBO =
    copy(
        id = UUID.randomUUID().toString(),
        versjonsnr = incrementSemVer(versjonsnr),
        revisjonAv = id,
        status = Status.UTKAST,
        erPublisert = false,
        publiseringsTidspunkt = null
    )

fun createNewConcept(org: Virksomhet, user: User): BegrepDBO {
    val newId = UUID.randomUUID().toString()
    return BegrepDBO(
        id = newId,
        originaltBegrep = newId,
        versjonsnr = NEW_CONCEPT_VERSION,
        revisjonAv = null,
        status = Status.UTKAST,
        statusURI = CONCEPT_STATUS.draft.uri,
        erPublisert = false,
        publiseringsTidspunkt = null,
        opprettet = Instant.now(),
        opprettetAv = user.name,
        anbefaltTerm = null,
        tillattTerm = HashMap(),
        frarådetTerm = HashMap(),
        definisjon = null,
        definisjonForAllmennheten = null,
        definisjonForSpesialister = null,
        merknad = HashMap(),
        merkelapp = ArrayList(),
        ansvarligVirksomhet = org,
        eksempel = HashMap(),
        fagområde = HashMap(),
        fagområdeKoder = ArrayList(),
        omfang = null,
        kontaktpunkt = null,
        gyldigFom = null,
        gyldigTom = null,
        endringslogelement = null,
        seOgså = ArrayList(),
        internSeOgså = null,
        erstattesAv = ArrayList(),
        assignedUser = null,
        abbreviatedLabel = null,
        begrepsRelasjon = ArrayList(),
        internBegrepsRelasjon = null,
        interneFelt = null,
        internErstattesAv = null
    )
}
