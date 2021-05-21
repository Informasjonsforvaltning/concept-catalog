package no.fdk.concept_catalog.utils

import no.fdk.concept_catalog.model.*
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap

const val LOCAL_SERVER_PORT = 5000

const val MONGO_USER = "testuser"
const val MONGO_PASSWORD = "testpassword"
const val MONGO_PORT = 27017
const val MONGO_DB_NAME = "concept-catalogue"

val MONGO_ENV_VALUES: Map<String, String> = ImmutableMap.of(
    "MONGO_INITDB_ROOT_USERNAME", MONGO_USER,
    "MONGO_INITDB_ROOT_PASSWORD", MONGO_PASSWORD
)

val BEGREP_0 = Begrep(
    id = "id0",
    status = Status.UTKAST,
    anbefaltTerm = Term(navn = mapOf(Pair("nb", "anbefaltTerm"))),
    tillattTerm = mapOf(Pair("nn", listOf("tillattTerm"))),
    frarådetTerm = mapOf(Pair("nb", listOf("fraraadetTerm"))),
    definisjon = Definisjon(tekst = mapOf(Pair("nb", "definisjon"))),
    kildebeskrivelse = Kildebeskrivelse(forholdTilKilde = ForholdTilKildeEnum.EGENDEFINERT, kilde = emptyList()),
    merknad = mapOf(Pair("nn", listOf("merknad"))),
    ansvarligVirksomhet = Virksomhet(
        id = "123456789"
    ),
    seOgså = listOf("http://begrepskatalogen/begrep/98da4336-dff2-11e7-a0fd-005056821322")
)

val BEGREP_1 = Begrep(
    id = "id1",
    status = Status.GODKJENT,
    anbefaltTerm = Term(navn = mapOf(Pair("nb", "Begrep 1"))),
    ansvarligVirksomhet = Virksomhet(
        id = "123456789"
    )
)

val BEGREP_2 = Begrep(
    id = "id2",
    status = Status.PUBLISERT,
    anbefaltTerm = Term(navn = mapOf(Pair("nb", "Begrep 2"))),
    ansvarligVirksomhet = Virksomhet(
        id = "123456789"
    )
)

val BEGREP_WRONG_ORG = Begrep(
    id = "id-wrong-org",
    ansvarligVirksomhet = Virksomhet(
        id = "999888777"
    )
)

val BEGREP_TO_BE_CREATED = Begrep(
    status = Status.UTKAST,
    anbefaltTerm = Term(navn = emptyMap()),
    ansvarligVirksomhet = Virksomhet(
        id = "111111111"
    )
)

val BEGREP_TO_BE_DELETED = Begrep(
    id = "id-to-be-deleted",
    status = Status.UTKAST,
    ansvarligVirksomhet = Virksomhet(
        id = "111111111"
    )
)
