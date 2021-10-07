package no.fdk.concept_catalog.service

import no.fdk.concept_catalog.model.*
import java.time.ZonedDateTime
import java.util.*

val NEW_CONCEPT_VERSION = SemVer(0, 0, 1)

fun BegrepDBO.toDTO(): Begrep =
    Begrep(
        id,
        originaltBegrep,
        versjonsnr,
        revisjonAv,
        status,
        anbefaltTerm,
        tillattTerm,
        frarådetTerm,
        definisjon,
        kildebeskrivelse,
        merknad,
        ansvarligVirksomhet,
        eksempel,
        fagområde,
        bruksområde,
        omfang,
        kontaktpunkt,
        gyldigFom,
        gyldigTom,
        endringslogelement,
        seOgså
    )

fun Begrep.createRevision(original: BegrepDBO): BegrepDBO =
    BegrepDBO(
        id = UUID.randomUUID().toString(),
        originaltBegrep = original.originaltBegrep,
        versjonsnr = incrementSemVer(original.versjonsnr),
        revisjonAv = original.id,
        status = Status.UTKAST,
        anbefaltTerm,
        tillattTerm,
        frarådetTerm,
        definisjon,
        kildebeskrivelse,
        merknad,
        ansvarligVirksomhet = original.ansvarligVirksomhet,
        eksempel,
        fagområde,
        bruksområde,
        omfang,
        kontaktpunkt,
        gyldigFom,
        gyldigTom,
        endringslogelement,
        seOgså
    )

fun Begrep.mapForCreation(): BegrepDBO {
    val newId = UUID.randomUUID().toString()

    return BegrepDBO(
        id = newId,
        originaltBegrep = newId,
        versjonsnr = NEW_CONCEPT_VERSION,
        revisjonAv = null,
        status,
        anbefaltTerm,
        tillattTerm,
        frarådetTerm,
        definisjon,
        kildebeskrivelse,
        merknad,
        ansvarligVirksomhet,
        eksempel,
        fagområde,
        bruksområde,
        omfang,
        kontaktpunkt,
        gyldigFom,
        gyldigTom,
        endringslogelement,
        seOgså
    )
}

fun BegrepDBO.updateLastChangedAndByWhom(userId: String): BegrepDBO =
    copy(
        endringslogelement = Endringslogelement(
            endringstidspunkt = ZonedDateTime.now().toInstant(),
            brukerId = userId
        )
    )

private fun incrementSemVer(semVer: SemVer?): SemVer =
    SemVer(
        major = semVer?.major ?: NEW_CONCEPT_VERSION.major,
        minor = semVer?.minor ?: NEW_CONCEPT_VERSION.minor,
        patch = semVer?.patch?.let { it + 1 } ?: NEW_CONCEPT_VERSION.patch
    )
