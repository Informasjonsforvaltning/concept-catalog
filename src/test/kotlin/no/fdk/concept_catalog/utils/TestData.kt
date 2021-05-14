package no.fdk.concept_catalog.utils

import no.fdk.concept_catalog.model.Begrep
import no.fdk.concept_catalog.model.Virksomhet

const val LOCAL_SERVER_PORT = 5000

val BEGREP_0 = Begrep(
    id = "id0",
    ansvarligVirksomhet = Virksomhet(
        id = "123456789"
    )
)

val BEGREP_WRONG_ORG = Begrep(
    id = "id0",
    ansvarligVirksomhet = Virksomhet(
        id = "999888777"
    )
)
