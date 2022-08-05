package no.fdk.concept_catalog.service

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
