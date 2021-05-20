package no.fdk.concept_catalog.utils

import no.fdk.concept_catalog.model.Begrep
import no.fdk.concept_catalog.model.Virksomhet
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap

const val LOCAL_SERVER_PORT = 5000

const val MONGO_USER = "testuser"
const val MONGO_PASSWORD = "testpassword"
const val MONGO_PORT = 27017
const val MONGO_DB_NAME = "datasetCatalog"

val MONGO_ENV_VALUES: Map<String, String> = ImmutableMap.of(
    "MONGO_INITDB_ROOT_USERNAME", MONGO_USER,
    "MONGO_INITDB_ROOT_PASSWORD", MONGO_PASSWORD
)

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
