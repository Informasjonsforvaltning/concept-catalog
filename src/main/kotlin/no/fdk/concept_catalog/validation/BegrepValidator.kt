package no.fdk.concept_catalog.validation

import no.fdk.concept_catalog.model.Begrep
import no.fdk.concept_catalog.model.Status
import no.fdk.concept_catalog.model.Virksomhet
import java.time.LocalDate

fun Begrep.isValid(): Boolean = when {
    status == null -> false
    status == Status.UTKAST -> false
    anbefaltTerm == null -> false
    anbefaltTerm.navn.isNullOrEmpty() -> false
    !isValidTranslationsMap(anbefaltTerm.navn) -> false
    definisjon == null -> false
    definisjon.tekst.isNullOrEmpty() -> false
    !isValidTranslationsMap(definisjon.tekst) -> false
    ansvarligVirksomhet == null -> false
    !ansvarligVirksomhet.isValid() -> false
    !isValidValidityPeriod(gyldigFom, gyldigTom) -> false
    else -> true
}

private fun Virksomhet.isValid(): Boolean = when {
    id.isNullOrBlank() -> false
    id.length != 9 -> false
    else -> true
}

private fun isValidTranslationsMap(translations: Map<String, Any>): Boolean = when {
    !translations.values.stream().anyMatch { it is String && it.isNotBlank() } -> false
    else -> true
}

private fun isValidValidityPeriod(validFrom: LocalDate?, validTo: LocalDate?): Boolean = when {
    validFrom != null && validTo != null && validFrom.isAfter(validTo) -> false
    else -> true
}
