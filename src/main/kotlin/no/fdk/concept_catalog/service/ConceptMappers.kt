package no.fdk.concept_catalog.service

import no.fdk.concept_catalog.model.*
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
        erPublisert,
        publiseringsTidspunkt,
        upublisertRevisjon = unpublishedRevision,
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
        seOgså,
        erstattesAv,
        tildeltBruker,
        begrepsRelasjon
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

fun Begrep.createRevision(original: BegrepDBO): BegrepDBO =
    BegrepDBO(
        id = UUID.randomUUID().toString(),
        originaltBegrep = original.originaltBegrep,
        versjonsnr = incrementSemVer(original.versjonsnr),
        revisjonAv = original.id,
        status = Status.UTKAST,
        erPublisert = false,
        publiseringsTidspunkt = null,
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
        seOgså,
        erstattesAv,
        tildeltBruker,
        begrepsRelasjon
    )

fun Begrep.mapForCreation(): BegrepDBO {
    val newId = UUID.randomUUID().toString()

    return BegrepDBO(
        id = newId,
        originaltBegrep = newId,
        versjonsnr = NEW_CONCEPT_VERSION,
        revisjonAv = null,
        status,
        erPublisert = false,
        publiseringsTidspunkt = null,
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
        seOgså,
        erstattesAv,
        tildeltBruker,
        begrepsRelasjon
    )
}

fun BegrepDBO.updateLastChangedAndByWhom(user: User): BegrepDBO =
    copy(
        endringslogelement = Endringslogelement(
            endringstidspunkt = ZonedDateTime.now().toInstant(),
            brukerId = user.id
        )
    )

private fun incrementSemVer(semVer: SemVer?): SemVer =
    SemVer(
        major = semVer?.major ?: NEW_CONCEPT_VERSION.major,
        minor = semVer?.minor ?: NEW_CONCEPT_VERSION.minor,
        patch = semVer?.patch?.let { it + 1 } ?: NEW_CONCEPT_VERSION.patch
    )
