package no.fdk.concept_catalog.utils

import no.fdk.concept_catalog.model.*
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap
import java.time.LocalDate

const val LOCAL_SERVER_PORT = 5000

const val MONGO_USER = "testuser"
const val MONGO_PASSWORD = "testpassword"
const val MONGO_PORT = 27017
const val MONGO_DB_NAME = "concept-catalogue"

val MONGO_ENV_VALUES: Map<String, String> = ImmutableMap.of(
    "MONGO_INITDB_ROOT_USERNAME", MONGO_USER,
    "MONGO_INITDB_ROOT_PASSWORD", MONGO_PASSWORD
)

val BEGREP_0_OLD = Begrep(
    id = "id0-old",
    originaltBegrep = "id0-old",
    versjonsnr = SemVer(1, 0, 0),
    status = Status.PUBLISERT,
    anbefaltTerm = Term(navn = mapOf(Pair("nb", "old anbefaltTerm"))),
    tillattTerm = mapOf(Pair("nn", listOf("old tillattTerm"))),
    frarådetTerm = mapOf(Pair("nb", listOf("old fraraadetTerm"))),
    definisjon = Definisjon(tekst = mapOf(Pair("nb", "old definisjon"))),
    kildebeskrivelse = Kildebeskrivelse(forholdTilKilde = ForholdTilKildeEnum.EGENDEFINERT, kilde = emptyList()),
    merknad = mapOf(Pair("nn", listOf("old merknad"))),
    ansvarligVirksomhet = Virksomhet(
        id = "123456789"
    ),
    seOgså = listOf("http://begrepskatalogen/begrep/98da4336-dff2-11e7-a0fd-005056821322")
)

val BEGREP_0 = Begrep(
    id = "id0",
    originaltBegrep = "id0-old",
    versjonsnr = SemVer(1, 0, 1),
    status = Status.PUBLISERT,
    erSistPublisert = true,
    anbefaltTerm = Term(navn = mapOf(Pair("nb", "anbefaltTerm"))),
    tillattTerm = mapOf(Pair("nn", listOf("tillattTerm", "tillattTerm2"))),
    frarådetTerm = mapOf(Pair("nb", listOf("fraraadetTerm", "fraraadetTerm2"))),
    definisjon = Definisjon(tekst = mapOf(Pair("nb", "definisjon"))),
    kildebeskrivelse = Kildebeskrivelse(forholdTilKilde = ForholdTilKildeEnum.EGENDEFINERT, kilde = emptyList()),
    merknad = mapOf(Pair("nn", listOf("merknad"))),
    ansvarligVirksomhet = Virksomhet(
        id = "123456789"
    ),
    seOgså = listOf("http://begrepskatalogen/begrep/98da4336-dff2-11e7-a0fd-005056821322"),
    erstattesAv = listOf("http://begrepskatalogen/begrep/98da4336-dff2-11e7-a0fd-005056821322"),
    tildeltBruker = Bruker(id = "Test Testesen"),
    begrepsRelasjon = listOf(
        BegrepsRelasjon(
            relasjon = "assosiativ",
            beskrivelse = mapOf(Pair("nb", "Beskrivelse")),
            relatertBegrep = "uri-1"
        ),
        BegrepsRelasjon(
            relasjon = "partitiv",
            relasjonsType = "overordnet",
            inndelingskriterium = mapOf(Pair("nb", "Inndelingskriterium")),
            relatertBegrep = "uri-1"
        ),
        BegrepsRelasjon(
            relasjon = "generisk",
            relasjonsType = "underordnet",
            inndelingskriterium = mapOf(Pair("nb", "Inndelingskriterium")),
            relatertBegrep = "uri-1"
        )
    )
)

val BEGREP_1 = Begrep(
    id = "id1",
    originaltBegrep = "id1",
    versjonsnr = SemVer(1, 0, 0),
    revisjonAvSistPublisert = true,
    status = Status.GODKJENT,
    anbefaltTerm = Term(navn = mapOf(Pair("nb", "Begrep 1"))),
    ansvarligVirksomhet = Virksomhet(
        id = "123456789"
    )
)

val BEGREP_2 = Begrep(
    id = "id2",
    originaltBegrep = "id2",
    versjonsnr = SemVer(0, 0, 1),
    revisjonAvSistPublisert = true,
    status = Status.HOERING,
    anbefaltTerm = Term(navn = mapOf(Pair("nb", "Begrep 2"))),
    ansvarligVirksomhet = Virksomhet(
        id = "123456789"
    )
)

val BEGREP_WRONG_ORG = Begrep(
    id = "id-wrong-org",
    originaltBegrep = "id-wrong-org",
    versjonsnr = SemVer(0, 0, 1),
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
    originaltBegrep = "id-to-be-deleted",
    versjonsnr = SemVer(0, 0, 1),
    status = Status.UTKAST,
    ansvarligVirksomhet = Virksomhet(
        id = "111111111"
    )
)

val BEGREP_TO_BE_UPDATED = Begrep(
    id = "id-to-be-updated",
    originaltBegrep = "id-to-be-updated",
    versjonsnr = SemVer(1, 0, 0),
    anbefaltTerm = Term(navn = mapOf(Pair("en", "To be updated"))),
    tillattTerm = mapOf(Pair("nn", listOf("To be removed")), Pair("en", listOf("To be moved"))),
    bruksområde = mapOf(Pair("en", listOf("To be copied"))),
    eksempel = mapOf(Pair("en", listOf("Will be replaced by copy"))),
    status = Status.UTKAST,
    ansvarligVirksomhet = Virksomhet(
        id = "111111111"
    ),
    tildeltBruker = Bruker(id = "Test Testesen")
)

val BEGREP_3 = Begrep(
    id = "id3",
    originaltBegrep = "id3",
    versjonsnr = SemVer(1, 0, 0),
    status = Status.PUBLISERT,
    anbefaltTerm = Term(navn = mapOf(Pair("nn", "Begrep 3"))),
    definisjon = Definisjon(tekst = mapOf(Pair("nb", "definisjon"))),
    kildebeskrivelse = Kildebeskrivelse(
        forholdTilKilde = ForholdTilKildeEnum.BASERTPAAKILDE,
        kilde = listOf(
            URITekst(uri = "https://testdirektoratet.no", tekst = "Testdirektoratet"),
            URITekst(uri = "https://festdirektoratet.no", tekst = "Festdirektoratet"))),
    eksempel = mapOf(Pair("en", listOf("example"))),
    fagområde = mapOf(Pair("nb", "fagområde")),
    omfang = URITekst(uri = "https://test.no", tekst = "Test"),
    gyldigFom = LocalDate.of(2020, 10, 10),
    ansvarligVirksomhet = Virksomhet(
        id = "111222333"
    )
)

val BEGREP_4 = Begrep(
    id = "id4",
    originaltBegrep = "id4",
    versjonsnr = SemVer(1, 0, 0),
    status = Status.PUBLISERT,
    anbefaltTerm = Term(navn = mapOf(Pair("en", "Begrep 4"))),
    kildebeskrivelse = Kildebeskrivelse(
        forholdTilKilde = ForholdTilKildeEnum.SITATFRAKILDE,
        kilde = listOf(URITekst(uri = "https://testdirektoratet.no", tekst = "Testdirektoratet"))),
    bruksområde = mapOf(Pair("nn", listOf("bruksområde"))),
    gyldigTom = LocalDate.of(2030, 10, 10),
    kontaktpunkt = Kontaktpunkt(harEpost = "test@test.no", harTelefon = "99887766"),
    ansvarligVirksomhet = Virksomhet(
        id = "111222333"
    )
)

val BEGREP_5 = Begrep(
    id = "id5",
    originaltBegrep = "id5",
    versjonsnr = SemVer(1, 0, 0),
    status = Status.PUBLISERT,
    anbefaltTerm = Term(navn = mapOf(Pair("en", "Begrep 5"))),
    kildebeskrivelse = Kildebeskrivelse(
        forholdTilKilde = ForholdTilKildeEnum.SITATFRAKILDE,
        kilde = listOf(URITekst(uri = "https://testdirektoratet.no", tekst = "Testdirektoratet"))),
    bruksområde = mapOf(Pair("nn", listOf("bruksområde"))),
    gyldigTom = LocalDate.of(2030, 10, 10),
    kontaktpunkt = Kontaktpunkt(harEpost = "test@test.no", harTelefon = "99887766"),
    definisjon = Definisjon(tekst = mapOf(Pair("nb", "definisjon"))),
    ansvarligVirksomhet = Virksomhet(
        id = "111222333"
    )
)

val BEGREP_REVISION = Begrep(
    versjonsnr = SemVer(1, 0, 0),
    status = Status.PUBLISERT,
    anbefaltTerm = Term(navn = mapOf(Pair("en", "Begrep revisjon"))),
    kildebeskrivelse = Kildebeskrivelse(
        forholdTilKilde = ForholdTilKildeEnum.SITATFRAKILDE,
        kilde = listOf(URITekst(uri = "https://testdirektoratet.no", tekst = "Testdirektoratet"))),
    bruksområde = mapOf(Pair("nn", listOf("bruksområde"))),
    gyldigTom = LocalDate.of(2030, 10, 10),
    kontaktpunkt = Kontaktpunkt(harEpost = "test@test.no", harTelefon = "99887766")
)
