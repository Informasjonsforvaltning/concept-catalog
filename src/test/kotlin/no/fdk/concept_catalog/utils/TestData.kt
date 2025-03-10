package no.fdk.concept_catalog.utils

import no.fdk.concept_catalog.model.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

val BEGREP_0_OLD = Begrep(
    id = "id0-old",
    originaltBegrep = "id0-old",
    versjonsnr = SemVer(1, 0, 0),
    status = Status.PUBLISERT,
    statusURI = "http://publications.europa.eu/resource/authority/concept-status/DEPRECATED",
    erPublisert = true,
    sistPublisertId = "id0",
    publiseringsTidspunkt = ZonedDateTime.of(2019, 1, 1, 12, 0, 0, 0, ZoneId.of("Europe/Oslo")).toInstant(),
    anbefaltTerm = Term(navn = mapOf(Pair("nb", "old anbefaltTerm"))),
    tillattTerm = mapOf(Pair("nn", listOf("old tillattTerm"))),
    frarådetTerm = mapOf(Pair("nb", listOf("old fraraadetTerm"))),
    definisjon = Definisjon(
        tekst = mapOf(Pair("nb", "old definisjon")),
        kildebeskrivelse = Kildebeskrivelse(forholdTilKilde = ForholdTilKildeEnum.EGENDEFINERT, kilde = emptyList())
    ),
    merknad = mapOf(Pair("nn", "old merknad")),
    merkelapp = listOf("old merkelapp1", "old merkelapp2"),
    ansvarligVirksomhet = Virksomhet(
        id = "123456789"
    ),
    seOgså = listOf("http://begrepskatalogen/begrep/98da4336-dff2-11e7-a0fd-005056821322"),
    endringslogelement = Endringslogelement(
        endretAv = "bruker1",
        endringstidspunkt = ZonedDateTime.of(
            2019, 1, 1, 12, 0, 0, 0, ZoneId.of("Europe/Oslo")
        ).toInstant()
    ),
    interneFelt = emptyMap(),
    internErstattesAv = listOf("id1"),
)

val BEGREP_0 = Begrep(
    id = "id0",
    originaltBegrep = "id0-old",
    versjonsnr = SemVer(1, 0, 1),
    status = Status.PUBLISERT,
    statusURI = "http://publications.europa.eu/resource/authority/concept-status/CURRENT",
    erPublisert = true,
    publiseringsTidspunkt = ZonedDateTime.of(2020, 1, 2, 12, 0, 0, 0, ZoneId.of("Europe/Oslo")).toInstant(),
    sistPublisertId = "id0",
    anbefaltTerm = Term(navn = mapOf(Pair("nb", "anbefaltTerm"))),
    tillattTerm = mapOf(Pair("nn", listOf("tillattTerm", "tillattTerm2"))),
    frarådetTerm = mapOf(Pair("nb", listOf("fraraadetTerm", "fraraadetTerm2", "Lorem ipsum"))),
    definisjon = Definisjon(
        tekst = mapOf(Pair("nb", "definisjon")),
        kildebeskrivelse = Kildebeskrivelse(forholdTilKilde = ForholdTilKildeEnum.EGENDEFINERT, kilde = emptyList())
    ),
    definisjonForAllmennheten = Definisjon(
        tekst = mapOf(Pair("nb", "definisjon for allmennheten")),
        kildebeskrivelse = Kildebeskrivelse(forholdTilKilde = ForholdTilKildeEnum.EGENDEFINERT, kilde = emptyList())
    ),
    definisjonForSpesialister = Definisjon(
        tekst = mapOf(Pair("nb", "Definisjon for spesialister")),
        kildebeskrivelse = Kildebeskrivelse(forholdTilKilde = ForholdTilKildeEnum.EGENDEFINERT, kilde = emptyList())
    ),
    merknad = mapOf(Pair("nn", "merknad")),
    merkelapp = listOf("merkelapp1", "merkelapp2"),
    ansvarligVirksomhet = Virksomhet(
        id = "123456789"
    ),
    seOgså = listOf("http://begrepskatalogen/begrep/98da4336-dff2-11e7-a0fd-005056821322"),
    erstattesAv = listOf("http://begrepskatalogen/begrep/98da4336-dff2-11e7-a0fd-005056821322"),
    assignedUser = "user-id",
    internBegrepsRelasjon = listOf(
        BegrepsRelasjon(
            relasjon = "assosiativ",
            beskrivelse = mapOf(Pair("nb", "Beskrivelse")),
            relatertBegrep = "id1"
        )
    ),
    begrepsRelasjon = listOf(
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
            2020, 1, 2, 12, 0, 0, 0, ZoneId.of("Europe/Oslo")
        ).toInstant()
    ),
    interneFelt = mapOf(Pair("felt-id", InterntFelt("feltverdi"))),
    internErstattesAv = listOf("id1"),
)

val BEGREP_1 = Begrep(
    id = "id1",
    originaltBegrep = "id1",
    versjonsnr = SemVer(1, 0, 0),
    definisjon = Definisjon(tekst = mapOf(Pair("nb", "is searchable")), null),
    status = Status.GODKJENT,
    statusURI = "http://publications.europa.eu/resource/authority/concept-status/CURRENT",
    anbefaltTerm = Term(navn = mapOf(Pair("nb", "Begrep 1 Lorem ipsum"))),
    ansvarligVirksomhet = Virksomhet(
        id = "123456789"
    ),
    merknad = mapOf(Pair("nb", "asdf")),
    endringslogelement = Endringslogelement(
        endretAv = "bruker1",
        endringstidspunkt = ZonedDateTime.of(
            2020, 12, 1, 12, 0, 0, 0, ZoneId.of("Europe/Oslo")
        ).toInstant()
    ),
    interneFelt = null,
    internErstattesAv = null,
)

val BEGREP_2 = Begrep(
    id = "id2",
    originaltBegrep = "id2",
    definisjon = Definisjon(
        tekst = mapOf(Pair("nb", "tekstnb")),
        kildebeskrivelse = Kildebeskrivelse(forholdTilKilde = ForholdTilKildeEnum.EGENDEFINERT, kilde = emptyList())
    ),
    versjonsnr = SemVer(1, 0, 1),
    status = Status.HOERING,
    statusURI = "http://publications.europa.eu/resource/authority/concept-status/CANDIDATE",
    anbefaltTerm = Term(navn = mapOf(Pair("nb", ""), Pair("nn", "begrep 2"))),
    tillattTerm = mapOf(Pair("nb", listOf("Lorem ipsum"))),
    ansvarligVirksomhet = Virksomhet(
        id = "123456789"
    ),
    endringslogelement = Endringslogelement(
        endretAv = "bruker1",
        endringstidspunkt = ZonedDateTime.of(
            2020, 1, 1, 12, 0, 0, 0, ZoneId.of("Europe/Oslo")
        ).toInstant()
    ),
    interneFelt = null,
    internErstattesAv = null,
)

val BEGREP_WRONG_ORG = Begrep(
    id = "id-wrong-org",
    originaltBegrep = "id-wrong-org",
    versjonsnr = SemVer(0, 0, 1),
    ansvarligVirksomhet = Virksomhet(
        id = "999888777"
    ),
    interneFelt = null,
    internErstattesAv = null,
)

val BEGREP_TO_BE_CREATED = Begrep(
    status = Status.UTKAST,
    statusURI = "http://publications.europa.eu/resource/authority/concept-status/DRAFT",
    anbefaltTerm = Term(navn = emptyMap()),
    ansvarligVirksomhet = Virksomhet(
        id = "123456789"
    ),
    interneFelt = null,
    internErstattesAv = null,
)

val BEGREP_TO_BE_DELETED = Begrep(
    id = "id-to-be-deleted",
    originaltBegrep = "id-to-be-deleted",
    versjonsnr = SemVer(0, 0, 1),
    status = Status.UTKAST,
    statusURI = "http://publications.europa.eu/resource/authority/concept-status/DRAFT",
    ansvarligVirksomhet = Virksomhet(
        id = "111111111"
    ),
    interneFelt = null,
    internErstattesAv = null,
)

val BEGREP_TO_BE_UPDATED = Begrep(
    id = "id-to-be-updated",
    originaltBegrep = "id-to-be-updated",
    versjonsnr = SemVer(1, 0, 0),
    anbefaltTerm = Term(navn = mapOf(Pair("en", "To be updated"))),
    definisjon = Definisjon(tekst = mapOf(Pair("nb", "definisjon")), null),
    tillattTerm = mapOf(Pair("nn", listOf("To be removed")), Pair("en", listOf("To be moved"))),
    fagområde = mapOf(Pair("en", listOf("To be copied"))),
    fagområdeKoder = listOf("5e6b2561-6157-4eb4-b396-d773cd00de12", "fagomr2"),
    eksempel = mapOf(Pair("en", "Will be replaced by copy")),
    status = Status.UTKAST,
    ansvarligVirksomhet = Virksomhet(
        id = "111111111"
    ),
    assignedUser = "user-id",
    interneFelt = null,
    internErstattesAv = null,
)

val BEGREP_3 = Begrep(
    id = "id3",
    originaltBegrep = "id3",
    versjonsnr = SemVer(1, 0, 0),
    status = Status.PUBLISERT,
    statusURI = "http://publications.europa.eu/resource/authority/concept-status/CURRENT",
    erPublisert = true,
    publiseringsTidspunkt = ZonedDateTime.of(2020, 1, 2, 12, 0, 0, 0, ZoneId.of("Europe/Oslo")).toInstant(),
    anbefaltTerm = Term(navn = mapOf(Pair("nn", "Begrep 3"))),
    definisjon = Definisjon(
        tekst = mapOf(Pair("nb", "definisjon")),
        kildebeskrivelse = Kildebeskrivelse(
            forholdTilKilde = ForholdTilKildeEnum.BASERTPAAKILDE,
            kilde = listOf(
                URITekst(uri = "https://testdirektoratet.no", tekst = "Testdirektoratet"),
                URITekst(uri = "https://festdirektoratet.no", tekst = "Festdirektoratet")
            )
        )
    ),
    eksempel = mapOf(Pair("en", "example")),
    fagområde = mapOf(Pair("nb", listOf("fagområde"))),
    fagområdeKoder = listOf("5e6b2561-6157-4eb4-b396-d773cd00de12", "fagomr2"),
    omfang = URITekst(uri = "https://test.no"),
    internSeOgså = listOf("id4"),
    internBegrepsRelasjon = listOf(
        BegrepsRelasjon(
            relasjon = "assosiativ",
            beskrivelse = mapOf(Pair("nb", "Beskrivelse")),
            relatertBegrep = "id4"
        )
    ),
    gyldigFom = LocalDate.of(2020, 10, 10),
    ansvarligVirksomhet = Virksomhet(
        id = "111222333"
    ),
    interneFelt = null,
    internErstattesAv = listOf("id4")
)

val BEGREP_4 = Begrep(
    id = "id4",
    originaltBegrep = "id4",
    versjonsnr = SemVer(1, 0, 0),
    status = Status.PUBLISERT,
    statusURI = "http://publications.europa.eu/resource/authority/concept-status/CURRENT",
    erPublisert = true,
    sistPublisertId = "id4",
    publiseringsTidspunkt = ZonedDateTime.of(2020, 1, 2, 12, 0, 0, 0, ZoneId.of("Europe/Oslo")).toInstant(),
    anbefaltTerm = Term(navn = mapOf(Pair("en", "Begrep 4"))),
    definisjon = Definisjon(
        kildebeskrivelse = Kildebeskrivelse(
            forholdTilKilde = ForholdTilKildeEnum.SITATFRAKILDE,
            kilde = listOf(URITekst(uri = "https://testdirektoratet.no", tekst = "Testdirektoratet"))
        )
    ),
    fagområde = mapOf(Pair("nn", listOf("bruksområde"))),
    fagområdeKoder = listOf("fagomr3"),
    gyldigTom = LocalDate.of(2030, 10, 10),
    kontaktpunkt = Kontaktpunkt(harEpost = "test@test.no", harTelefon = "99887766"),
    ansvarligVirksomhet = Virksomhet(
        id = "111222333"
    ),
    interneFelt = mapOf(
        Pair("felt1", InterntFelt("true")),
        Pair("felt2", InterntFelt("false"))
    ),
    internErstattesAv = null,
    omfang = URITekst(tekst = "omfang")
)

val BEGREP_5 = Begrep(
    id = "id5",
    originaltBegrep = "id5",
    versjonsnr = SemVer(1, 0, 0),
    status = Status.PUBLISERT,
    statusURI = "http://publications.europa.eu/resource/authority/concept-status/CURRENT",
    erPublisert = true,
    sistPublisertId = "id5",
    publiseringsTidspunkt = ZonedDateTime.of(2020, 1, 2, 12, 0, 0, 0, ZoneId.of("Europe/Oslo")).toInstant(),
    anbefaltTerm = Term(navn = mapOf(Pair("en", "Begrep 5"))),
    fagområde = mapOf(Pair("nn", listOf("bruksområde"))),
    fagområdeKoder = listOf("5e6b2561-6157-4eb4-b396-d773cd00de12", "fagomr2"),
    gyldigTom = LocalDate.of(2030, 10, 10),
    kontaktpunkt = Kontaktpunkt(harEpost = "test@test.no", harTelefon = "99887766"),
    definisjon = Definisjon(
        tekst = mapOf(Pair("nb", "definisjon")),
        kildebeskrivelse = Kildebeskrivelse(
            forholdTilKilde = ForholdTilKildeEnum.SITATFRAKILDE,
            kilde = listOf(URITekst(uri = "https://testdirektoratet.no", tekst = "Testdirektoratet"))
        )
    ),
    ansvarligVirksomhet = Virksomhet(
        id = "111222333"
    ),
    interneFelt = null,
    internErstattesAv = null
)

val BEGREP_REVISION = Begrep(
    versjonsnr = SemVer(1, 0, 1),
    status = Status.UTKAST,
    statusURI = "http://publications.europa.eu/resource/authority/concept-status/DRAFT",
    erPublisert = true,
    publiseringsTidspunkt = ZonedDateTime.of(2020, 1, 2, 12, 0, 0, 0, ZoneId.of("Europe/Oslo")).toInstant(),
    anbefaltTerm = Term(navn = mapOf(Pair("en", "Begrep revisjon"))),
    definisjon = Definisjon(
        kildebeskrivelse = Kildebeskrivelse(
            forholdTilKilde = ForholdTilKildeEnum.SITATFRAKILDE,
            kilde = listOf(URITekst(uri = "https://testdirektoratet.no", tekst = "Testdirektoratet"))
        )
    ),
    fagområde = mapOf(Pair("nn", listOf("bruksområde"))),
    gyldigTom = LocalDate.of(2030, 10, 10),
    kontaktpunkt = Kontaktpunkt(harEpost = "test@test.no", harTelefon = "99887766"),
    ansvarligVirksomhet = Virksomhet(
        id = "111222333"
    ),
    interneFelt = null,
    internErstattesAv = null
)

val BEGREP_6 = Begrep(
    id = "id6",
    originaltBegrep = "id6",
    versjonsnr = SemVer(1, 0, 0),
    status = Status.PUBLISERT,
    statusURI = "http://publications.europa.eu/resource/authority/concept-status/CURRENT",
    erPublisert = true,
    publiseringsTidspunkt = ZonedDateTime.of(2020, 1, 2, 12, 0, 0, 0, ZoneId.of("Europe/Oslo")).toInstant(),
    ansvarligVirksomhet = Virksomhet(
        id = "987654321"
    ),
    definisjon = Definisjon(
        tekst = mapOf(Pair("nb", "definisjon")),
        kildebeskrivelse = Kildebeskrivelse(
            forholdTilKilde = ForholdTilKildeEnum.BASERTPAAKILDE,
            kilde = listOf(URITekst(uri = "", tekst = "hei"))
        )
    ),
    omfang = URITekst(tekst = "omfangtekst6"),
    interneFelt = null,
    internErstattesAv = null
)

val BEGREP_HAS_REVISION = Begrep(
    id = "id-has-revision",
    originaltBegrep = "id-has-revision",
    status = Status.PUBLISERT,
    statusURI = "http://publications.europa.eu/resource/authority/concept-status/CURRENT",
    erPublisert = true,
    publiseringsTidspunkt = ZonedDateTime.of(2020, 1, 2, 12, 0, 0, 0, ZoneId.of("Europe/Oslo")).toInstant(),
    versjonsnr = SemVer(0, 0, 1),
    ansvarligVirksomhet = Virksomhet(
        id = "111111111"
    ),
    interneFelt = null,
    internSeOgså = listOf("id-to-be-updated"),
    internErstattesAv = null
)

val BEGREP_UNPUBLISHED_REVISION = Begrep(
    id = "id-unpublished-revision",
    originaltBegrep = "id-has-revision",
    status = Status.UTKAST,
    statusURI = "http://publications.europa.eu/resource/authority/concept-status/DRAFT",
    erPublisert = false,
    versjonsnr = SemVer(0, 0, 1),
    ansvarligVirksomhet = Virksomhet(
        id = "111111111"
    ),
    interneFelt = null,
    internErstattesAv = listOf("id-to-be-updated"),
    internBegrepsRelasjon = listOf(
        BegrepsRelasjon(
            relasjon = "assosiativ",
            relatertBegrep = "id-to-be-updated"
        )
    )
)

val BEGREP_HAS_MULTIPLE_REVISIONS = Begrep(
    id = "id-has-multiple-revisions",
    originaltBegrep = "id-has-multiple-revisions",
    status = Status.PUBLISERT,
    statusURI = "http://publications.europa.eu/resource/authority/concept-status/CURRENT",
    erPublisert = true,
    sistPublisertId = "id-has-multiple-revisions",
    publiseringsTidspunkt = ZonedDateTime.of(2020, 1, 2, 12, 0, 0, 0, ZoneId.of("Europe/Oslo")).toInstant(),
    versjonsnr = SemVer(0, 0, 1),
    ansvarligVirksomhet = Virksomhet(
        id = "222222222"
    ),
    interneFelt = null,
    internErstattesAv = null
)

val BEGREP_UNPUBLISHED_REVISION_MULTIPLE_FIRST = Begrep(
    id = "id-unpublished-revision-multiple-first",
    originaltBegrep = "id-has-multiple-revisions",
    status = Status.UTKAST,
    statusURI = "http://publications.europa.eu/resource/authority/concept-status/DRAFT",
    erPublisert = false,
    sistPublisertId = "id-has-multiple-revisions",
    versjonsnr = SemVer(0, 0, 2),
    ansvarligVirksomhet = Virksomhet(
        id = "222222222"
    ),
    interneFelt = null,
    internErstattesAv = null
)

val BEGREP_UNPUBLISHED_REVISION_MULTIPLE_SECOND = Begrep(
    id = "id-unpublished-revision-multiple-second",
    originaltBegrep = "id-has-multiple-revisions",
    status = Status.UTKAST,
    statusURI = "http://publications.europa.eu/resource/authority/concept-status/DRAFT",
    erPublisert = false,
    sistPublisertId = "id-has-multiple-revisions",
    versjonsnr = SemVer(0, 0, 3),
    ansvarligVirksomhet = Virksomhet(
        id = "222222222"
    ),
    interneFelt = null,
    internErstattesAv = null
)

val CHANGE_REQUEST_0 = ChangeRequest(
    id = "cr0",
    conceptId = null,
    catalogId = "111111111",
    status = ChangeRequestStatus.ACCEPTED,
    operations = listOf(JsonPatchOperation(op = OpEnum.REPLACE, path = "/baz", value = "boo")),
    timeForProposal = Instant.now(),
    proposedBy = User(id = "1924782563", name = "TEST USER", email = null),
    title = "Endringsforslag 0"
)

val CHANGE_REQUEST_1 = ChangeRequest(
    id = "cr1",
    conceptId = null,
    catalogId = "111111111",
    status = ChangeRequestStatus.REJECTED,
    operations = emptyList(),
    timeForProposal = ZonedDateTime.of(2019, 1, 1, 12, 0, 0, 0, ZoneId.of("Europe/Oslo")).toInstant(),
    proposedBy = User(id = "1924782563", name = "TEST USER", email = null),
    title = "Endringsforslag 1"
)

val CHANGE_REQUEST_2 = ChangeRequest(
    id = "cr2",
    conceptId = null,
    catalogId = "111111111",
    status = ChangeRequestStatus.OPEN,
    operations = emptyList(),
    timeForProposal = ZonedDateTime.of(2019, 1, 1, 12, 0, 0, 0, ZoneId.of("Europe/Oslo")).toInstant(),
    proposedBy = User(id = "1924782563", name = "TEST USER", email = null),
    title = "Endringsforslag 2"
)

val CHANGE_REQUEST_3 = ChangeRequest(
    id = "cr3",
    conceptId = "id0-old",
    catalogId = "123456789",
    status = ChangeRequestStatus.OPEN,
    operations = listOf(JsonPatchOperation(op = OpEnum.ADD, path = "/assignedUser", value = "newUserId", from = null)),
    timeForProposal = ZonedDateTime.of(2019, 1, 1, 12, 0, 0, 0, ZoneId.of("Europe/Oslo")).toInstant(),
    proposedBy = User(id = "1924782563", name = "TEST USER", email = null),
    title = "Endringsforslag 3"
)

val CHANGE_REQUEST_4 = ChangeRequest(
    id = "cr4",
    conceptId = BEGREP_2.id,
    catalogId = "123456789",
    status = ChangeRequestStatus.OPEN,
    operations = listOf(
        JsonPatchOperation(op = OpEnum.ADD, path = "/assignedUser", value = "newUserId", from = null),
    ),
    timeForProposal = ZonedDateTime.of(2019, 1, 1, 12, 0, 0, 0, ZoneId.of("Europe/Oslo")).toInstant(),
    proposedBy = User(id = "1924782563", name = "TEST USER", email = null),
    title = "Endringsforslag 4"
)

val CHANGE_REQUEST_5 = ChangeRequest(
    id = "cr5",
    conceptId = null,
    catalogId = "123456789",
    status = ChangeRequestStatus.OPEN,
    operations = listOf(JsonPatchOperation(op = OpEnum.ADD, path = "/assignedUser", value = "newUserId", from = null)),
    timeForProposal = ZonedDateTime.of(2019, 1, 1, 12, 0, 0, 0, ZoneId.of("Europe/Oslo")).toInstant(),
    proposedBy = User(id = "1924782563", name = "TEST USER", email = null),
    title = "Endringsforslag 5"
)

val CHANGE_REQUEST_6 = ChangeRequest(
    id = "cr6",
    conceptId = BEGREP_2.id,
    catalogId = "123456789",
    status = ChangeRequestStatus.REJECTED,
    operations = listOf(),
    timeForProposal = ZonedDateTime.of(2019, 1, 1, 12, 0, 0, 0, ZoneId.of("Europe/Oslo")).toInstant(),
    proposedBy = User(id = "1924782563", name = "TEST USER", email = null),
    title = "Endringsforslag 6"
)

val CHANGE_REQUEST_UPDATE_BODY_NEW = ChangeRequestUpdateBody(
    conceptId = null,
    operations = listOf(
        JsonPatchOperation(op = OpEnum.ADD, path = "/assignedUser", value = "newUserId", from = null),
    ),
    title = "Forslag til nytt begrep"
)

val CHANGE_REQUEST_UPDATE_BODY_UPDATE = ChangeRequestUpdateBody(
    conceptId = "123456789",
    operations = listOf(
        JsonPatchOperation(op = OpEnum.ADD, path = "/assignedUser", value = "newUserId", from = null)
    ),
    title = "Ny tittel endringsforslag"
)

val CHANGE_REQUEST_UPDATE_BODY_0 = ChangeRequestUpdateBody(
    conceptId = BEGREP_TO_BE_UPDATED.id,
    operations = listOf(
        JsonPatchOperation(
            op = OpEnum.ADD,
            path = "/anbefaltTerm/navn/nb",
            value = "Enda en ny anbefalt term"
        )
    ),
    title = "Endringsforslag 7"
)
