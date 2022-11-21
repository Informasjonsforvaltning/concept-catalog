package no.fdk.concept_catalog.service

import no.fdk.concept_catalog.model.Status
import java.net.URI

fun String?.isValidURI(): Boolean =
    if (this.isNullOrBlank()) {
        false
    } else {
        try {
            URI(this)
            true
        } catch (e: Exception) {
            false
        }
    }


fun statusFromString(str: String?): Status? =
    when (str?.lowercase()) {
        Status.UTKAST.value -> Status.UTKAST
        Status.GODKJENT.value -> Status.GODKJENT
        Status.HOERING.value -> Status.HOERING
        Status.PUBLISERT.value -> Status.PUBLISERT
        else -> null
    }
