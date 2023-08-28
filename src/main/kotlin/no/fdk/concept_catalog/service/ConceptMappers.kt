package no.fdk.concept_catalog.service

import no.fdk.concept_catalog.model.*
import java.time.Instant
import java.time.ZonedDateTime
import java.util.*

val NEW_CONCEPT_VERSION = SemVer(0, 0, 1)

fun BegrepDBO.toDTO(highestPublishedVersion: SemVer?, highestPublishedId: String?, unpublishedRevision: String?): Begrep =
    Begrep(
        id,
        originaltBegrep,
        versjonsnr,
        erSistPublisert = isHighestPublishedVersion(highestPublishedVersion),
        revisjonAvSistPublisert = isRevisionOfHighestPublishedVersion(highestPublishedId),
        revisjonAv,
        status,
        statusURI,
        erPublisert,
        publiseringsTidspunkt,
        gjeldendeRevisjon = unpublishedRevision,
        anbefaltTerm,
        tillattTerm,
        frarådetTerm,
        definisjon,
        folkeligForklaring,
        rettsligForklaring,
        merknad,
        ansvarligVirksomhet,
        eksempel,
        fagområde,
        fagområdeKoder,
        omfang,
        kontaktpunkt,
        gyldigFom,
        gyldigTom,
        endringslogelement,
        opprettet,
        opprettetAv,
        seOgså,
        erstattesAv,
        assignedUser,
        abbreviatedLabel,
        begrepsRelasjon,
        interneFelt
    )

private fun BegrepDBO.isHighestPublishedVersion(highestPublishedVersion: SemVer?): Boolean =
    when {
        !erPublisert -> false
        highestPublishedVersion == null -> false
        versjonsnr == highestPublishedVersion -> true
        else -> false
    }

private fun BegrepDBO.isRevisionOfHighestPublishedVersion(highestPublishedId: String?): Boolean =
    when {
        erPublisert -> false
        highestPublishedId == null -> true
        revisjonAv == highestPublishedId -> true
        else -> false
    }

fun BegrepDBO.addUpdatableFieldsFromDTO(dto: Begrep) =
    copy(
        status = dto.status,
        statusURI = dto.statusURI,
        anbefaltTerm = dto.anbefaltTerm,
        tillattTerm = dto.tillattTerm,
        frarådetTerm = dto.frarådetTerm,
        definisjon = dto.definisjon,
        folkeligForklaring = dto.folkeligForklaring,
        rettsligForklaring = dto.rettsligForklaring,
        merknad = dto.merknad,
        eksempel = dto.eksempel,
        fagområde = dto.fagområde,
        fagområdeKoder = dto.fagområdeKoder,
        omfang = dto.omfang,
        kontaktpunkt = dto.kontaktpunkt,
        gyldigFom = dto.gyldigFom,
        gyldigTom = dto.gyldigTom,
        seOgså = dto.seOgså,
        erstattesAv = dto.erstattesAv,
        assignedUser = dto.assignedUser,
        abbreviatedLabel = dto.abbreviatedLabel,
        begrepsRelasjon = dto.begrepsRelasjon,
        interneFelt = dto.interneFelt
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

fun BegrepDBO.createNewRevision(user: User): BegrepDBO =
    copy(
        id = UUID.randomUUID().toString(),
        versjonsnr = incrementSemVer(versjonsnr),
        revisjonAv = id,
        status = Status.UTKAST,
        erPublisert = false,
        publiseringsTidspunkt = null,
        opprettet = Instant.now(),
        opprettetAv = user.name
    )

fun createNewConcept(org: Virksomhet, user: User): BegrepDBO {
    val newId = UUID.randomUUID().toString()
    return BegrepDBO(
        id = newId,
        originaltBegrep = newId,
        versjonsnr = NEW_CONCEPT_VERSION,
        revisjonAv = null,
        status = Status.UTKAST,
        erPublisert = false,
        publiseringsTidspunkt = null,
        opprettet = Instant.now(),
        opprettetAv = user.name,
        anbefaltTerm = null,
        tillattTerm = HashMap(),
        frarådetTerm = HashMap(),
        definisjon = null,
        folkeligForklaring = null,
        rettsligForklaring = null,
        merknad = HashMap(),
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
        erstattesAv = ArrayList(),
        assignedUser = null,
        abbreviatedLabel = null,
        begrepsRelasjon = ArrayList(),
        interneFelt = null
    )
}
