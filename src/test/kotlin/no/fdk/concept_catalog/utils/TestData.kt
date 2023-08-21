package no.fdk.concept_catalog.utils

import no.fdk.concept_catalog.model.*
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap
import java.time.*

const val LOCAL_SERVER_PORT = 6000

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
    erPublisert = true,
    publiseringsTidspunkt = ZonedDateTime.of(2019, 1, 1, 12,0,0,0, ZoneId.of("Europe/Oslo")).toInstant(),
    gjeldendeRevisjon = null,
    anbefaltTerm = Term(navn = mapOf(Pair("nb", "old anbefaltTerm"))),
    tillattTerm = mapOf(Pair("nn", listOf("old tillattTerm"))),
    frarådetTerm = mapOf(Pair("nb", listOf("old fraraadetTerm"))),
    definisjon = Definisjon(
        tekst = mapOf(Pair("nb", "old definisjon")),
        kildebeskrivelse = Kildebeskrivelse(forholdTilKilde = ForholdTilKildeEnum.EGENDEFINERT, kilde = emptyList())),
    merknad = mapOf(Pair("nn", "old merknad")),
    ansvarligVirksomhet = Virksomhet(
        id = "123456789"
    ),
    seOgså = listOf("http://begrepskatalogen/begrep/98da4336-dff2-11e7-a0fd-005056821322"),
    endringslogelement = Endringslogelement(
        endretAv = "bruker1",
        endringstidspunkt = ZonedDateTime.of(
            2019, 1, 1, 12,0,0,0, ZoneId.of("Europe/Oslo")
        ).toInstant()),
    interneFelt = emptyMap()
)

val BEGREP_0 = Begrep(
    id = "id0",
    originaltBegrep = "id0-old",
    versjonsnr = SemVer(1, 0, 1),
    status = Status.PUBLISERT,
    erPublisert = true,
    publiseringsTidspunkt = ZonedDateTime.of(2020, 1, 2, 12,0,0,0, ZoneId.of("Europe/Oslo")).toInstant(),
    gjeldendeRevisjon = null,
    erSistPublisert = true,
    anbefaltTerm = Term(navn = mapOf(Pair("nb", "anbefaltTerm"))),
    tillattTerm = mapOf(Pair("nn", listOf("tillattTerm", "tillattTerm2"))),
    frarådetTerm = mapOf(Pair("nb", listOf("fraraadetTerm", "fraraadetTerm2", "Lorem ipsum"))),
    definisjon = Definisjon(
        tekst = mapOf(Pair("nb", "definisjon")),
        kildebeskrivelse = Kildebeskrivelse(forholdTilKilde = ForholdTilKildeEnum.EGENDEFINERT, kilde = emptyList())),
    folkeligForklaring = Definisjon(
        tekst = mapOf(Pair("nb", "Folkelig forklaring")),
        kildebeskrivelse = Kildebeskrivelse(forholdTilKilde = ForholdTilKildeEnum.EGENDEFINERT, kilde = emptyList())),
    rettsligForklaring = Definisjon(
        tekst = mapOf(Pair("nb", "Rettslig forklaring")),
        kildebeskrivelse = Kildebeskrivelse(forholdTilKilde = ForholdTilKildeEnum.EGENDEFINERT, kilde = emptyList())),
    merknad = mapOf(Pair("nn", "merknad")),
    ansvarligVirksomhet = Virksomhet(
        id = "123456789"
    ),
    seOgså = listOf("http://begrepskatalogen/begrep/98da4336-dff2-11e7-a0fd-005056821322"),
    erstattesAv = listOf("http://begrepskatalogen/begrep/98da4336-dff2-11e7-a0fd-005056821322"),
    assignedUser = "user-id",
    begrepsRelasjon = listOf(
        BegrepsRelasjon(
            relasjon = "assosiativ",
            beskrivelse = mapOf(Pair("nb", "Beskrivelse")),
            relatertBegrep = "http://begrepskatalogen/begrep/98da4336-dff2-11e7-a0fd-005056821322"
        ),
        BegrepsRelasjon(
            relasjon = "partitiv",
            relasjonsType = "omfatter",
            inndelingskriterium = mapOf(Pair("nb", "Inndelingskriterium")),
            relatertBegrep = "http://begrepskatalogen/begrep/98da4336-dff2-11e7-a0fd-005056821322"
        ),
        BegrepsRelasjon(
            relasjon = "partitiv",
            relasjonsType = "erDelAv",
            inndelingskriterium = mapOf(Pair("nb", "Inndelingskriterium")),
            relatertBegrep = "http://begrepskatalogen/begrep/98da4336-dff2-11e7-a0fd-005056821322"
        ),
        BegrepsRelasjon(
            relasjon = "generisk",
            relasjonsType = "underordnet",
            inndelingskriterium = mapOf(Pair("nb", "Inndelingskriterium")),
            relatertBegrep = "http://begrepskatalogen/begrep/98da4336-dff2-11e7-a0fd-005056821322"
        ),
        BegrepsRelasjon(
            relasjon = "generisk",
            relasjonsType = "overordnet",
            inndelingskriterium = mapOf(Pair("nb", "Inndelingskriterium")),
            relatertBegrep = "http://begrepskatalogen/begrep/98da4336-dff2-11e7-a0fd-005056821322"
        )
    ),
    endringslogelement = Endringslogelement(
        endretAv = "bruker1",
        endringstidspunkt = ZonedDateTime.of(
            2020, 1, 2, 12,0,0,0, ZoneId.of("Europe/Oslo")
        ).toInstant()),
    interneFelt = mapOf(Pair("felt-id", InterntFelt("feltverdi")))
)

val BEGREP_1 = Begrep(
    id = "id1",
    originaltBegrep = "id1",
    versjonsnr = SemVer(1, 0, 0),
    revisjonAvSistPublisert = true,
    gjeldendeRevisjon = null,
    definisjon = Definisjon(tekst = mapOf(Pair("nb", "is searchable")), null),
    status = Status.GODKJENT,
    anbefaltTerm = Term(navn = mapOf(Pair("nb", "Begrep 1"), Pair("en", "Lorem ipsum"))),
    ansvarligVirksomhet = Virksomhet(
        id = "123456789"
    ),
    endringslogelement = Endringslogelement(
        endretAv = "bruker1",
        endringstidspunkt = ZonedDateTime.of(
            2020, 12, 1, 12,0,0,0, ZoneId.of("Europe/Oslo")
        ).toInstant()),
    interneFelt = null
)

val BEGREP_2 = Begrep(
    id = "id2",
    originaltBegrep = "id2",
    definisjon = Definisjon(
        tekst = mapOf(Pair("nb", "tekstnb")),
        kildebeskrivelse = Kildebeskrivelse(forholdTilKilde = ForholdTilKildeEnum.EGENDEFINERT, kilde = emptyList())),
    versjonsnr = SemVer(1, 0, 1),
    revisjonAvSistPublisert = true,
    gjeldendeRevisjon = null,
    status = Status.HOERING,
    anbefaltTerm = Term(navn = mapOf(Pair("nb", "Begrep 2"))),
    tillattTerm = mapOf(Pair("nn", listOf("Lorem ipsum"))),
    ansvarligVirksomhet = Virksomhet(
        id = "123456789"
    ),
    endringslogelement = Endringslogelement(
        endretAv = "bruker1",
        endringstidspunkt = ZonedDateTime.of(
            2020, 1, 1, 12,0,0,0, ZoneId.of("Europe/Oslo")
        ).toInstant()),
    interneFelt = null,
)

val BEGREP_WRONG_ORG = Begrep(
    id = "id-wrong-org",
    originaltBegrep = "id-wrong-org",
    gjeldendeRevisjon = null,
    versjonsnr = SemVer(0, 0, 1),
    ansvarligVirksomhet = Virksomhet(
        id = "999888777"
    ),
    interneFelt = null
)

val BEGREP_TO_BE_CREATED = Begrep(
    status = Status.UTKAST,
    anbefaltTerm = Term(navn = emptyMap()),
    gjeldendeRevisjon = null,
    ansvarligVirksomhet = Virksomhet(
        id = "111111111"
    ),
    interneFelt = null
)

val BEGREP_TO_BE_DELETED = Begrep(
    id = "id-to-be-deleted",
    originaltBegrep = "id-to-be-deleted",
    versjonsnr = SemVer(0, 0, 1),
    gjeldendeRevisjon = null,
    status = Status.UTKAST,
    ansvarligVirksomhet = Virksomhet(
        id = "111111111"
    ),
    interneFelt = null
)

val BEGREP_TO_BE_UPDATED = Begrep(
    id = "id-to-be-updated",
    originaltBegrep = "id-to-be-updated",
    versjonsnr = SemVer(1, 0, 0),
    gjeldendeRevisjon = null,
    anbefaltTerm = Term(navn = mapOf(Pair("en", "To be updated"))),
    definisjon = Definisjon(tekst = mapOf(Pair("nb", "definisjon")), null),
    tillattTerm = mapOf(Pair("nn", listOf("To be removed")), Pair("en", listOf("To be moved"))),
    fagområde = mapOf(Pair("en", listOf("To be copied"))),
    fagområdeKoder = listOf("fagomr1","fagomr2"),
    eksempel = mapOf(Pair("en", "Will be replaced by copy")),
    status = Status.UTKAST,
    ansvarligVirksomhet = Virksomhet(
        id = "111111111"
    ),
    assignedUser = "user-id",
    interneFelt = null,
)

val BEGREP_3 = Begrep(
    id = "id3",
    originaltBegrep = "id3",
    versjonsnr = SemVer(1, 0, 0),
    status = Status.PUBLISERT,
    erPublisert = true,
    publiseringsTidspunkt = ZonedDateTime.of(2020, 1, 2, 12,0,0,0, ZoneId.of("Europe/Oslo")).toInstant(),
    gjeldendeRevisjon = null,
    anbefaltTerm = Term(navn = mapOf(Pair("nn", "Begrep 3"))),
    definisjon = Definisjon(
        tekst = mapOf(Pair("nb", "definisjon")),
        kildebeskrivelse = Kildebeskrivelse(
            forholdTilKilde = ForholdTilKildeEnum.BASERTPAAKILDE,
            kilde = listOf(
                URITekst(uri = "https://testdirektoratet.no", tekst = "Testdirektoratet"),
                URITekst(uri = "https://festdirektoratet.no", tekst = "Festdirektoratet")))),
    eksempel = mapOf(Pair("en", "example")),
    fagområde = mapOf(Pair("nb", listOf("fagområde"))),
    fagområdeKoder = listOf("fagomr1","fagomr2"),
    omfang = URITekst(uri = "https://test.no", tekst = "Test"),
    gyldigFom = LocalDate.of(2020, 10, 10),
    ansvarligVirksomhet = Virksomhet(
        id = "111222333"
    ),
    interneFelt = null
)

val BEGREP_4 = Begrep(
    id = "id4",
    originaltBegrep = "id4",
    versjonsnr = SemVer(1, 0, 0),
    status = Status.PUBLISERT,
    erPublisert = true,
    publiseringsTidspunkt = ZonedDateTime.of(2020, 1, 2, 12,0,0,0, ZoneId.of("Europe/Oslo")).toInstant(),
    gjeldendeRevisjon = null,
    anbefaltTerm = Term(navn = mapOf(Pair("en", "Begrep 4"))),
    definisjon = Definisjon(
        kildebeskrivelse = Kildebeskrivelse(
            forholdTilKilde = ForholdTilKildeEnum.SITATFRAKILDE,
            kilde = listOf(URITekst(uri = "https://testdirektoratet.no", tekst = "Testdirektoratet")))),
    fagområde = mapOf(Pair("nn", listOf("bruksområde"))),
    fagområdeKoder = listOf("fagomr1","fagomr2"),
    gyldigTom = LocalDate.of(2030, 10, 10),
    kontaktpunkt = Kontaktpunkt(harEpost = "test@test.no", harTelefon = "99887766"),
    ansvarligVirksomhet = Virksomhet(
        id = "111222333"
    ),
    interneFelt = null
)

val BEGREP_5 = Begrep(
    id = "id5",
    originaltBegrep = "id5",
    versjonsnr = SemVer(1, 0, 0),
    status = Status.PUBLISERT,
    erPublisert = true,
    publiseringsTidspunkt = ZonedDateTime.of(2020, 1, 2, 12,0,0,0, ZoneId.of("Europe/Oslo")).toInstant(),
    gjeldendeRevisjon = null,
    anbefaltTerm = Term(navn = mapOf(Pair("en", "Begrep 5"))),
    fagområde = mapOf(Pair("nn", listOf("bruksområde"))),
    fagområdeKoder = listOf("fagomr1","fagomr2"),
    gyldigTom = LocalDate.of(2030, 10, 10),
    kontaktpunkt = Kontaktpunkt(harEpost = "test@test.no", harTelefon = "99887766"),
    definisjon = Definisjon(
        tekst = mapOf(Pair("nb", "definisjon")),
        kildebeskrivelse = Kildebeskrivelse(
            forholdTilKilde = ForholdTilKildeEnum.SITATFRAKILDE,
            kilde = listOf(URITekst(uri = "https://testdirektoratet.no", tekst = "Testdirektoratet")))),
    ansvarligVirksomhet = Virksomhet(
        id = "111222333"
    ),
    interneFelt = null
)

val BEGREP_REVISION = Begrep(
    versjonsnr = SemVer(1, 0, 0),
    status = Status.PUBLISERT,
    erPublisert = true,
    publiseringsTidspunkt = ZonedDateTime.of(2020, 1, 2, 12,0,0,0, ZoneId.of("Europe/Oslo")).toInstant(),
    gjeldendeRevisjon = null,
    anbefaltTerm = Term(navn = mapOf(Pair("en", "Begrep revisjon"))),
    definisjon = Definisjon(
        kildebeskrivelse = Kildebeskrivelse(
            forholdTilKilde = ForholdTilKildeEnum.SITATFRAKILDE,
            kilde = listOf(URITekst(uri = "https://testdirektoratet.no", tekst = "Testdirektoratet")))),
    fagområde = mapOf(Pair("nn", listOf("bruksområde"))),
    gyldigTom = LocalDate.of(2030, 10, 10),
    kontaktpunkt = Kontaktpunkt(harEpost = "test@test.no", harTelefon = "99887766"),
    interneFelt = null
)

val BEGREP_6 = Begrep(
    id = "id6",
    originaltBegrep = "id6",
    versjonsnr = SemVer(1, 0, 0),
    revisjonAvSistPublisert = false,
    status = Status.PUBLISERT,
    erPublisert = true,
    publiseringsTidspunkt = ZonedDateTime.of(2020, 1, 2, 12,0,0,0, ZoneId.of("Europe/Oslo")).toInstant(),
    gjeldendeRevisjon = null,
    ansvarligVirksomhet = Virksomhet(
        id = "987654321"
    ),
    definisjon = Definisjon(
        tekst = mapOf(Pair("nb", "definisjon")),
        kildebeskrivelse = Kildebeskrivelse(
            forholdTilKilde = ForholdTilKildeEnum.BASERTPAAKILDE,
            kilde = listOf(URITekst(uri = "", tekst = "hei")))),
    omfang = URITekst(uri = "", tekst = "omfangtekst6"),
    interneFelt = null
)

val BEGREP_HAS_REVISION = Begrep(
    id = "id-has-revision",
    originaltBegrep = "id-has-revision",
    status = Status.PUBLISERT,
    erPublisert = true,
    publiseringsTidspunkt = ZonedDateTime.of(2020, 1, 2, 12,0,0,0, ZoneId.of("Europe/Oslo")).toInstant(),
    gjeldendeRevisjon = null,
    versjonsnr = SemVer(0, 0, 1),
    ansvarligVirksomhet = Virksomhet(
        id = "111111111"
    ),
    interneFelt = null
)

val BEGREP_UNPUBLISHED_REVISION = Begrep(
    id = "id-unpublished-revision",
    originaltBegrep = "id-has-revision",
    status = Status.UTKAST,
    erPublisert = false,
    gjeldendeRevisjon = null,
    versjonsnr = SemVer(0, 0, 1),
    ansvarligVirksomhet = Virksomhet(
        id = "111111111"
    ),
    interneFelt = null
)

val BEGREP_HAS_MULTIPLE_REVISIONS = Begrep(
    id = "id-has-multiple-revisions",
    originaltBegrep = "id-has-multiple-revisions",
    status = Status.PUBLISERT,
    erPublisert = true,
    erSistPublisert = true,
    publiseringsTidspunkt = ZonedDateTime.of(2020, 1, 2, 12,0,0,0, ZoneId.of("Europe/Oslo")).toInstant(),
    gjeldendeRevisjon = "id-unpublished-revision-multiple-first",
    versjonsnr = SemVer(0, 0, 1),
    ansvarligVirksomhet = Virksomhet(
        id = "222222222"
    ),
    interneFelt = null
)

val BEGREP_UNPUBLISHED_REVISION_MULTIPLE_FIRST = Begrep(
    id = "id-unpublished-revision-multiple-first",
    originaltBegrep = "id-has-multiple-revisions",
    status = Status.UTKAST,
    erPublisert = false,
    gjeldendeRevisjon = null,
    versjonsnr = SemVer(0, 0, 2),
    ansvarligVirksomhet = Virksomhet(
        id = "222222222"
    ),
    interneFelt = null,
)

val BEGREP_UNPUBLISHED_REVISION_MULTIPLE_SECOND = Begrep(
    id = "id-unpublished-revision-multiple-second",
    originaltBegrep = "id-has-multiple-revisions",
    status = Status.UTKAST,
    erPublisert = false,
    gjeldendeRevisjon = null,
    versjonsnr = SemVer(0, 0, 2),
    ansvarligVirksomhet = Virksomhet(
        id = "222222222"
    ),
    interneFelt = null,
)

val CHANGE_REQUEST_0 = ChangeRequest(
    id = "cr0",
    conceptId = null,
    catalogId = "111111111",
    status = ChangeRequestStatus.ACCEPTED,
    operations = listOf( JsonPatchOperation(op = OpEnum.REPLACE, path = "/baz", value = "boo") ),
    timeForProposal = Instant.now(),
    proposedBy = User(id="1924782563", name="TEST USER", email=null)
)

val CHANGE_REQUEST_1 = ChangeRequest(
    id = "cr1",
    conceptId = null,
    catalogId = "111111111",
    status = ChangeRequestStatus.REJECTED,
    operations = emptyList(),
    timeForProposal =  ZonedDateTime.of(2019, 1, 1, 12,0,0,0, ZoneId.of("Europe/Oslo")).toInstant(),
    proposedBy = User(id="1924782563", name="TEST USER", email=null)
)

val CHANGE_REQUEST_2 = ChangeRequest(
    id = "cr2",
    conceptId = null,
    catalogId = "111111111",
    status = ChangeRequestStatus.OPEN,
    operations = emptyList(),
    timeForProposal = ZonedDateTime.of(2019, 1, 1, 12,0,0,0, ZoneId.of("Europe/Oslo")).toInstant(),
    proposedBy = User(id="1924782563", name="TEST USER", email=null)
)

val CHANGE_REQUEST_3 = ChangeRequest(
    id = "cr3",
    conceptId = "id0-old",
    catalogId = "123456789",
    status = ChangeRequestStatus.OPEN,
    operations = listOf(JsonPatchOperation(op= OpEnum.ADD, path="/assignedUser", value="newUserId", from=null)),
    timeForProposal = ZonedDateTime.of(2019, 1, 1, 12,0,0,0, ZoneId.of("Europe/Oslo")).toInstant(),
    proposedBy = User(id="1924782563", name="TEST USER", email=null)
)

val CHANGE_REQUEST_4 = ChangeRequest(
    id = "cr4",
    conceptId = BEGREP_2.id,
    catalogId = "123456789",
    status = ChangeRequestStatus.OPEN,
    operations = listOf(
        JsonPatchOperation(op= OpEnum.ADD, path="/assignedUser", value="newUserId", from=null),
    ),
    timeForProposal = ZonedDateTime.of(2019, 1, 1, 12,0,0,0, ZoneId.of("Europe/Oslo")).toInstant(),
    proposedBy = User(id="1924782563", name="TEST USER", email=null)
)

val CHANGE_REQUEST_5 = ChangeRequest(
    id = "cr5",
    conceptId = null,
    catalogId = "123456789",
    status = ChangeRequestStatus.OPEN,
    operations = listOf(JsonPatchOperation(op= OpEnum.ADD, path="/assignedUser", value="newUserId", from=null)),
    timeForProposal = ZonedDateTime.of(2019, 1, 1, 12,0,0,0, ZoneId.of("Europe/Oslo")).toInstant(),
    proposedBy = User(id="1924782563", name="TEST USER", email=null)
)