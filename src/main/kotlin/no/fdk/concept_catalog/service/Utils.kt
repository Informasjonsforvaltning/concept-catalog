package no.fdk.concept_catalog.service

import no.fdk.concept_catalog.model.Status
import org.apache.jena.datatypes.xsd.XSDDateTime
import java.net.URI
import java.security.MessageDigest
import java.time.LocalDate
import java.util.Base64

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

fun encodeBase64(input: String): String {
    return Base64.getEncoder().encodeToString(input.toByteArray())
}

fun createHash(input: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

data class IdPair(
    val encodedId: String,
    val hashedId: String
)

fun statusFromString(str: String?): Status? =
    when (str?.lowercase()) {
        Status.UTKAST.value -> Status.UTKAST
        Status.GODKJENT.value -> Status.GODKJENT
        Status.HOERING.value -> Status.HOERING
        Status.PUBLISERT.value -> Status.PUBLISERT
        else -> null
    }

fun localDateToXSDDateTime(localDate: LocalDate): XSDDateTime {
    val o = IntArray(9)
    o[0] = localDate.getYear()
    o[1] = localDate.getMonthValue()
    o[2] = localDate.getDayOfMonth()
    return XSDDateTime(o, XSDDateTime.YEAR_MASK.toInt() or XSDDateTime.MONTH_MASK.toInt() or XSDDateTime.DAY_MASK.toInt())
}

fun escapeURI(uri: String?): String? {
    if (uri == null) {
        return null
    }
    val sb = StringBuilder()
    val legalCharacters = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-._~:/?#[]@!$&'()*+,;=" // and 0xA0->
    val hex = "0123456789ABCDEF"
    uri.forEach {
        when {
            it in legalCharacters -> sb.append(it)
            it.code > 0xA0 -> sb.append(it)
            else -> {
                sb.append('%')
                sb.append(hex[it.code and 0x00F0 shr 4])
                sb.append(hex[it.code and 0x000F])
            }
        }
    }
    return sb.toString()
}

fun getCollectionUri(collectionBaseUri: String, publisherId: String): String {
    return "$collectionBaseUri/collections/$publisherId"
}

fun getConceptUri(collectionUri: String, conceptId: String): String {
    return "$collectionUri/concepts/$conceptId"
}
