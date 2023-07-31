package no.fdk.concept_catalog.contract

import com.fasterxml.jackson.module.kotlin.readValue
import kotlin.test.assertEquals
import no.fdk.concept_catalog.configuration.JacksonConfigurer
import no.fdk.concept_catalog.model.Begrep
import no.fdk.concept_catalog.model.ChangeRequest
import no.fdk.concept_catalog.model.ChangeRequestForCreate
import no.fdk.concept_catalog.model.ChangeRequestStatus
import no.fdk.concept_catalog.model.Definisjon
import no.fdk.concept_catalog.model.Endringslogelement
import no.fdk.concept_catalog.model.JsonPatchOperation
import no.fdk.concept_catalog.model.OpEnum
import no.fdk.concept_catalog.model.SemVer
import no.fdk.concept_catalog.model.Status
import no.fdk.concept_catalog.model.Virksomhet
import no.fdk.concept_catalog.utils.*
import no.fdk.concept_catalog.utils.BEGREP_TO_BE_UPDATED
import no.fdk.concept_catalog.utils.jwk.Access
import no.fdk.concept_catalog.utils.jwk.JwtToken
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.ContextConfiguration

private val mapper = JacksonConfigurer().objectMapper()


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
    properties = ["spring.profiles.active=contract-test"],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ContextConfiguration(initializers = [ApiTestContext.Initializer::class])
@Tag("contract")
class ChangeRequests : ApiTestContext() {

    @Nested
    internal inner class GetChangeRequests {
        private val path = "/111111111/endringsforslag"

         @Test
         fun unauthorizedWhenMissingToken() {
             val rsp = authorizedRequest(path, port, null, null, HttpMethod.GET )

             assertEquals(HttpStatus.UNAUTHORIZED.value(), rsp["status"])
         }

        @Test
        fun forbiddenWhenAuthorizedForOtherCatalog() {
            val rsp = authorizedRequest(path, port, null, JwtToken(Access.WRONG_ORG).toString(), HttpMethod.GET )

            assertEquals(HttpStatus.FORBIDDEN.value(), rsp["status"])
        }

        @Test
        fun ableToGetChangeRequestsForAllOrgRoles() {
            val rspRead = authorizedRequest(path, port, null, JwtToken(Access.ORG_READ).toString(), HttpMethod.GET )
            val rspWrite = authorizedRequest(path, port, null, JwtToken(Access.ORG_WRITE).toString(), HttpMethod.GET )

            assertEquals(HttpStatus.OK.value(), rspRead["status"])
            assertEquals(HttpStatus.OK.value(), rspWrite["status"])
            val resultRead: List<ChangeRequest> = mapper.readValue(rspRead["body"] as String)
            val resultWrite: List<ChangeRequest> = mapper.readValue(rspWrite["body"] as String)

            val expected = listOf(CHANGE_REQUEST_0, CHANGE_REQUEST_1, CHANGE_REQUEST_2)
            assertEquals(expected, resultRead)
            assertEquals(expected, resultWrite)
        }

        @Test
        fun ableToGetChangeRequestsFilteredByStatus() {
            val rspOpen = authorizedRequest("$path?status=OPEN", port, null, JwtToken(Access.ORG_READ).toString(), HttpMethod.GET )
            val rspRejected = authorizedRequest("$path?status=rejected", port, null, JwtToken(Access.ORG_READ).toString(), HttpMethod.GET )
            val rspAccepted = authorizedRequest("$path?status=aCCepTed", port, null, JwtToken(Access.ORG_READ).toString(), HttpMethod.GET )

            assertEquals(HttpStatus.OK.value(), rspOpen["status"])
            assertEquals(HttpStatus.OK.value(), rspRejected["status"])
            assertEquals(HttpStatus.OK.value(), rspAccepted["status"])

            val resultOpen: List<ChangeRequest> = mapper.readValue(rspOpen["body"] as String)
            val resultRejected: List<ChangeRequest> = mapper.readValue(rspRejected["body"] as String)
            val resultAccepted: List<ChangeRequest> = mapper.readValue(rspAccepted["body"] as String)

            assertEquals(listOf(CHANGE_REQUEST_2), resultOpen)
            assertEquals(listOf(CHANGE_REQUEST_1), resultRejected)
            assertEquals(listOf(CHANGE_REQUEST_0), resultAccepted)
        }
    }

    @Nested
    internal inner class GetChangeRequestById {
        private val path = "/111111111/endringsforslag/${CHANGE_REQUEST_0.id}"

        @Test
        fun unauthorizedWhenMissingToken() {
            val rsp = authorizedRequest(path, port, null, null, HttpMethod.GET )

            assertEquals(HttpStatus.UNAUTHORIZED.value(), rsp["status"])
        }

        @Test
        fun forbiddenWhenAuthorizedForOtherCatalog() {
            val rsp = authorizedRequest(path, port, null, JwtToken(Access.WRONG_ORG).toString(), HttpMethod.GET )

            assertEquals(HttpStatus.FORBIDDEN.value(), rsp["status"])
        }

        @Test
        fun notFoundForInvalidId() {
            val rsp = authorizedRequest("/111111111/endringsforslag/invalid", port, null, JwtToken(Access.ORG_READ).toString(), HttpMethod.GET )

            assertEquals(HttpStatus.NOT_FOUND.value(), rsp["status"])
        }

        @Test
        fun ableToGetChangeRequestForAllOrgRoles() {
            val rspRead = authorizedRequest(path, port, null, JwtToken(Access.ORG_READ).toString(), HttpMethod.GET )
            val rspWrite = authorizedRequest(path, port, null, JwtToken(Access.ORG_WRITE).toString(), HttpMethod.GET )

            assertEquals(HttpStatus.OK.value(), rspRead["status"])
            assertEquals(HttpStatus.OK.value(), rspWrite["status"])
            val resultRead: ChangeRequest = mapper.readValue(rspRead["body"] as String)
            val resultWrite: ChangeRequest = mapper.readValue(rspWrite["body"] as String)

            val expected = CHANGE_REQUEST_0
            assertEquals(expected, resultRead)
            assertEquals(expected, resultWrite)
        }
    }

    @Nested
    internal inner class DeleteChangeRequest {
        private val path = "/111111111/endringsforslag/${CHANGE_REQUEST_0.id}"

        @Test
        fun unauthorizedWhenMissingToken() {
            val rsp = authorizedRequest(path, port, null, null, HttpMethod.DELETE )

            assertEquals(HttpStatus.UNAUTHORIZED.value(), rsp["status"])
        }

        @Test
        fun forbiddenWhenAuthorizedForOtherCatalog() {
            val rsp = authorizedRequest(path, port, null, JwtToken(Access.WRONG_ORG).toString(), HttpMethod.DELETE )

            assertEquals(HttpStatus.FORBIDDEN.value(), rsp["status"])
        }

        @Test
        fun forbiddenWhenAuthorizedForReadAccess() {
            val rsp = authorizedRequest(path, port, null, JwtToken(Access.ORG_READ).toString(), HttpMethod.DELETE )

            assertEquals(HttpStatus.FORBIDDEN.value(), rsp["status"])
        }

        @Test
        fun notFoundForInvalidId() {
            val rsp = authorizedRequest("/111111111/endringsforslag/invalid", port, null, JwtToken(Access.ORG_WRITE).toString(), HttpMethod.DELETE )
            assertEquals(HttpStatus.NOT_FOUND.value(), rsp["status"])
        }

        @Test
        fun ableToDeleteChangeRequest() {
            val rsp = authorizedRequest(path, port, null, JwtToken(Access.ORG_WRITE).toString(), HttpMethod.DELETE )
            assertEquals(HttpStatus.NO_CONTENT.value(), rsp["status"])
        }
    }

    @Nested
    internal inner class CreateChangeRequest {
        private val path = "/111111111/endringsforslag"

        @Test
        fun unauthorizedWhenMissingToken() {
            val body = ChangeRequestForCreate(
                conceptId = BEGREP_TO_BE_UPDATED.originaltBegrep,
                anbefaltTerm = BEGREP_TO_BE_UPDATED.anbefaltTerm,
                tillattTerm = BEGREP_TO_BE_UPDATED.tillattTerm,
                frarådetTerm = BEGREP_TO_BE_UPDATED.frarådetTerm,
                definisjon = Definisjon(tekst = mapOf(Pair("nb", "definisjon nb"), Pair("nn", "definisjon nn")), null)
            )
            val rsp = authorizedRequest(path, port, mapper.writeValueAsString(body), null, HttpMethod.POST )

            assertEquals(HttpStatus.UNAUTHORIZED.value(), rsp["status"])
        }

        @Test
        fun forbiddenWhenAuthorizedForOtherCatalog() {
            val body = ChangeRequestForCreate(
                conceptId = BEGREP_TO_BE_UPDATED.originaltBegrep,
                anbefaltTerm = BEGREP_TO_BE_UPDATED.anbefaltTerm,
                tillattTerm = BEGREP_TO_BE_UPDATED.tillattTerm,
                frarådetTerm = BEGREP_TO_BE_UPDATED.frarådetTerm,
                definisjon = Definisjon(tekst = mapOf(Pair("nb", "definisjon nb"), Pair("nn", "definisjon nn")), null)
            )
            val rsp = authorizedRequest(path, port, mapper.writeValueAsString(body), JwtToken(Access.WRONG_ORG).toString(), HttpMethod.POST )

            assertEquals(HttpStatus.FORBIDDEN.value(), rsp["status"])
        }

        @Test
        fun forbiddenWhenAuthorizedForReadAccess() {
            val body = ChangeRequestForCreate(
                conceptId = BEGREP_TO_BE_UPDATED.originaltBegrep,
                anbefaltTerm = BEGREP_TO_BE_UPDATED.anbefaltTerm,
                tillattTerm = BEGREP_TO_BE_UPDATED.tillattTerm,
                frarådetTerm = BEGREP_TO_BE_UPDATED.frarådetTerm,
                definisjon = Definisjon(tekst = mapOf(Pair("nb", "definisjon nb"), Pair("nn", "definisjon nn")), null)
            )
            val rsp = authorizedRequest(path, port, mapper.writeValueAsString(body), JwtToken(Access.ORG_READ).toString(), HttpMethod.POST )

            assertEquals(HttpStatus.FORBIDDEN.value(), rsp["status"])
        }

        @Test
        fun badRequestWhenAttemptingToRequestChangesOnNonOriginalId() {
            val body = ChangeRequestForCreate(
                conceptId = BEGREP_0.id,
                anbefaltTerm = BEGREP_0.anbefaltTerm,
                tillattTerm = BEGREP_0.tillattTerm,
                frarådetTerm = BEGREP_0.frarådetTerm,
                definisjon = Definisjon(tekst = mapOf(Pair("nb", "definisjon nb"), Pair("nn", "definisjon nn")), null)
            )
            val rsp = authorizedRequest(
                "/${BEGREP_0.ansvarligVirksomhet?.id}/endringsforslag",
                port,
                mapper.writeValueAsString(body),
                JwtToken(Access.ORG_WRITE).toString(),
                HttpMethod.POST
            )
            assertEquals(HttpStatus.BAD_REQUEST.value(), rsp["status"])
        }

        @Test
        fun ableToCreateChangeRequest() {
            val body = ChangeRequestForCreate(
                conceptId = BEGREP_TO_BE_UPDATED.originaltBegrep,
                anbefaltTerm = BEGREP_TO_BE_UPDATED.anbefaltTerm,
                tillattTerm = BEGREP_TO_BE_UPDATED.tillattTerm,
                frarådetTerm = BEGREP_TO_BE_UPDATED.frarådetTerm,
                definisjon = Definisjon(tekst = mapOf(Pair("nb", "definisjon nb"), Pair("nn", "definisjon nn")), null)
            )
            val rsp = authorizedRequest(path, port, mapper.writeValueAsString(body), JwtToken(Access.ORG_WRITE).toString(), HttpMethod.POST )
            assertEquals(HttpStatus.CREATED.value(), rsp["status"])

            val responseHeaders: HttpHeaders = rsp["header"] as HttpHeaders
            val location = responseHeaders.location
            assertNotNull(location)

            val getResponse = authorizedRequest(location.toString(), port, null, JwtToken(Access.ORG_READ).toString(), HttpMethod.GET)
            assertEquals(HttpStatus.OK.value(), getResponse["status"])
            val result: ChangeRequest = mapper.readValue(getResponse["body"] as String)
            val expected = ChangeRequest(
                id = result.id,
                catalogId = "111111111",
                conceptId = body.conceptId,
                anbefaltTerm = body.anbefaltTerm,
                tillattTerm = body.tillattTerm,
                frarådetTerm = body.frarådetTerm,
                definisjon = body.definisjon,
                status = ChangeRequestStatus.OPEN
            )
            assertEquals(expected, result)
        }
    }

    @Nested
    internal inner class UpdateChangeRequest {
        private val path = "/111111111/endringsforslag/${CHANGE_REQUEST_0.id}"

        @Test
        fun unauthorizedWhenMissingToken() {
            val body = listOf(JsonPatchOperation(op = OpEnum.ADD, "/tillattTerm", mapOf(Pair("nb", "tillatt nb"), Pair("nn", "tillatt nn"))))
            val rsp = authorizedRequest(path, port, mapper.writeValueAsString(body), null, HttpMethod.PATCH )

            assertEquals(HttpStatus.UNAUTHORIZED.value(), rsp["status"])
        }

        @Test
        fun forbiddenWhenAuthorizedForOtherCatalog() {
            val body = listOf(JsonPatchOperation(op = OpEnum.ADD, "/tillattTerm", mapOf(Pair("nb", "tillatt nb"), Pair("nn", "tillatt nn"))))
            val rsp = authorizedRequest(path, port, mapper.writeValueAsString(body), JwtToken(Access.WRONG_ORG).toString(), HttpMethod.PATCH )

            assertEquals(HttpStatus.FORBIDDEN.value(), rsp["status"])
        }

        @Test
        fun forbiddenWhenAuthorizedForReadAccess() {
            val body = listOf(JsonPatchOperation(op = OpEnum.ADD, "/tillattTerm", mapOf(Pair("nb", "tillatt nb"), Pair("nn", "tillatt nn"))))
            val rsp = authorizedRequest(path, port, mapper.writeValueAsString(body), JwtToken(Access.ORG_READ).toString(), HttpMethod.PATCH )

            assertEquals(HttpStatus.FORBIDDEN.value(), rsp["status"])
        }

        @Test
        fun notFoundForInvalidId() {
            val body = listOf(JsonPatchOperation(op = OpEnum.ADD, "/tillattTerm", mapOf(Pair("nb", "tillatt nb"), Pair("nn", "tillatt nn"))))
            val rsp = authorizedRequest("/111111111/endringsforslag/invalid", port, mapper.writeValueAsString(body), JwtToken(Access.ORG_WRITE).toString(), HttpMethod.PATCH )

            assertEquals(HttpStatus.NOT_FOUND.value(), rsp["status"])
        }

        @Test
        fun ableToUpdateChangeRequest() {
            val newTillatt = mapOf(Pair("nb", listOf("tillatt nb")), Pair("nn", listOf("tillatt nn")))
            val body = listOf(JsonPatchOperation(op = OpEnum.ADD, "/tillattTerm", newTillatt))
            val rsp = authorizedRequest(path, port, mapper.writeValueAsString(body), JwtToken(Access.ORG_WRITE).toString(), HttpMethod.PATCH )
            assertEquals(HttpStatus.OK.value(), rsp["status"])

            val result: ChangeRequest = mapper.readValue(rsp["body"] as String)
            val expected = CHANGE_REQUEST_0.copy(tillattTerm = newTillatt)
            assertEquals(expected, result)
        }

        @Test
        fun badRequestWhenUpdatingIdFields() {
            val bodyId = listOf(JsonPatchOperation(op = OpEnum.REPLACE, "/id", "123456"))
            val bodyCatalogId = listOf(JsonPatchOperation(op = OpEnum.REPLACE, "/catalogId", "123456"))
            val bodyConceptId = listOf(JsonPatchOperation(op = OpEnum.ADD, "/conceptId", "123456"))
            val rspId = authorizedRequest(path, port, mapper.writeValueAsString(bodyId), JwtToken(Access.ORG_WRITE).toString(), HttpMethod.PATCH )
            val rspCatalogId = authorizedRequest(path, port, mapper.writeValueAsString(bodyCatalogId), JwtToken(Access.ORG_WRITE).toString(), HttpMethod.PATCH )
            val rspConceptId = authorizedRequest(path, port, mapper.writeValueAsString(bodyConceptId), JwtToken(Access.ORG_WRITE).toString(), HttpMethod.PATCH )

            assertEquals(HttpStatus.BAD_REQUEST.value(), rspId["status"])
            assertEquals(HttpStatus.BAD_REQUEST.value(), rspCatalogId["status"])
            assertEquals(HttpStatus.BAD_REQUEST.value(), rspConceptId["status"])
        }

    }

    @Nested
    internal inner class RejectChangeRequest {
        private val path = "/111111111/endringsforslag/${CHANGE_REQUEST_2.id}/reject"

        @Test
        fun unauthorizedWhenMissingToken() {
            val rsp = authorizedRequest(path, port, null, null, HttpMethod.POST )

            assertEquals(HttpStatus.UNAUTHORIZED.value(), rsp["status"])
        }

        @Test
        fun forbiddenWhenAuthorizedForOtherCatalog() {
            val rsp = authorizedRequest(path, port, null, JwtToken(Access.WRONG_ORG).toString(), HttpMethod.POST )

            assertEquals(HttpStatus.FORBIDDEN.value(), rsp["status"])
        }

        @Test
        fun forbiddenWhenAuthorizedForReadAccess() {
            val rsp = authorizedRequest(path, port, null, JwtToken(Access.ORG_READ).toString(), HttpMethod.POST )

            assertEquals(HttpStatus.FORBIDDEN.value(), rsp["status"])
        }

        @Test
        fun notFoundForInvalidId() {
            val rsp = authorizedRequest("/111111111/endringsforslag/invalid/reject", port, null, JwtToken(Access.ORG_WRITE).toString(), HttpMethod.POST )
            assertEquals(HttpStatus.NOT_FOUND.value(), rsp["status"])
        }

        @Test
        fun badRequestWhenRejectingNonOpen() {
            val alreadyAccepted = authorizedRequest("/111111111/endringsforslag/${CHANGE_REQUEST_0.id}/reject", port, null, JwtToken(Access.ORG_WRITE).toString(), HttpMethod.POST )
            val alreadyRejected = authorizedRequest("/111111111/endringsforslag/${CHANGE_REQUEST_1.id}/reject", port, null, JwtToken(Access.ORG_WRITE).toString(), HttpMethod.POST )
            assertEquals(HttpStatus.BAD_REQUEST.value(), alreadyAccepted["status"])
            assertEquals(HttpStatus.BAD_REQUEST.value(), alreadyRejected["status"])
        }

        @Test
        fun ableToRejectChangeRequest() {
            val rsp = authorizedRequest(path, port, null, JwtToken(Access.ORG_WRITE).toString(), HttpMethod.POST )
            assertEquals(HttpStatus.OK.value(), rsp["status"])
        }
    }

    @Nested
    internal inner class AcceptChangeRequest {
        private val path = "/123456789/endringsforslag/${CHANGE_REQUEST_3.id}/accept"

        @Test
        fun unauthorizedWhenMissingToken() {
            val rsp = authorizedRequest(path, port, null, null, HttpMethod.POST )

            assertEquals(HttpStatus.UNAUTHORIZED.value(), rsp["status"])
        }

        @Test
        fun forbiddenWhenAuthorizedForOtherCatalog() {
            val rsp = authorizedRequest(path, port, null, JwtToken(Access.WRONG_ORG).toString(), HttpMethod.POST )

            assertEquals(HttpStatus.FORBIDDEN.value(), rsp["status"])
        }

        @Test
        fun forbiddenWhenAuthorizedForReadAccess() {
            val rsp = authorizedRequest(path, port, null, JwtToken(Access.ORG_READ).toString(), HttpMethod.POST )

            assertEquals(HttpStatus.FORBIDDEN.value(), rsp["status"])
        }

        @Test
        fun notFoundForInvalidId() {
            val rsp = authorizedRequest("/111111111/endringsforslag/invalid/accept", port, null, JwtToken(Access.ORG_WRITE).toString(), HttpMethod.POST )
            assertEquals(HttpStatus.NOT_FOUND.value(), rsp["status"])
        }

        @Test
        fun badRequestWhenAcceptingNonOpen() {
            val alreadyAccepted = authorizedRequest("/111111111/endringsforslag/${CHANGE_REQUEST_0.id}/accept", port, null, JwtToken(Access.ORG_WRITE).toString(), HttpMethod.POST )
            val alreadyRejected = authorizedRequest("/111111111/endringsforslag/${CHANGE_REQUEST_1.id}/accept", port, null, JwtToken(Access.ORG_WRITE).toString(), HttpMethod.POST )
            assertEquals(HttpStatus.BAD_REQUEST.value(), alreadyAccepted["status"])
            assertEquals(HttpStatus.BAD_REQUEST.value(), alreadyRejected["status"])
        }

        @Test
        fun acceptOfChangeRequestToPublishedConceptCreatesNewRevision() {
            val rsp = authorizedRequest(path, port, null, JwtToken(Access.ORG_WRITE).toString(), HttpMethod.POST )
            assertEquals(HttpStatus.OK.value(), rsp["status"])

            val responseHeaders: HttpHeaders = rsp["header"] as HttpHeaders
            val location = responseHeaders.location
            assertNotNull(location)

            val getResponse = authorizedRequest(location.toString(), port, null, JwtToken(Access.ORG_READ).toString(), HttpMethod.GET)
            assertEquals(HttpStatus.OK.value(), getResponse["status"])
            val result: Begrep = mapper.readValue(getResponse["body"] as String)
            val expected = BEGREP_0.copy(
                id = result.id,
                versjonsnr = SemVer(1, 0, 2),
                status = Status.UTKAST,
                erPublisert = false,
                erSistPublisert = false,
                publiseringsTidspunkt = null,
                revisjonAv = BEGREP_0.id,
                revisjonAvSistPublisert = true,
                opprettet = result.opprettet,
                opprettetAv = "TEST USER",
                anbefaltTerm = CHANGE_REQUEST_3.anbefaltTerm,
                tillattTerm = CHANGE_REQUEST_3.tillattTerm,
                frarådetTerm = CHANGE_REQUEST_3.frarådetTerm,
                definisjon = CHANGE_REQUEST_3.definisjon,
                endringslogelement = Endringslogelement(endretAv = "TEST USER", endringstidspunkt = result.endringslogelement!!.endringstidspunkt)
            )
            assertEquals(expected, result)
        }

        @Test
        fun acceptOfChangeRequestToUnpublishedConceptUpdatesExistingConcept() {
            val rsp = authorizedRequest("/123456789/endringsforslag/${CHANGE_REQUEST_4.id}/accept", port, null, JwtToken(Access.ORG_WRITE).toString(), HttpMethod.POST )
            assertEquals(HttpStatus.OK.value(), rsp["status"])

            val responseHeaders: HttpHeaders = rsp["header"] as HttpHeaders
            val location = responseHeaders.location
            assertNotNull(location)

            val getResponse = authorizedRequest(location.toString(), port, null, JwtToken(Access.ORG_READ).toString(), HttpMethod.GET)
            assertEquals(HttpStatus.OK.value(), getResponse["status"])
            val result: Begrep = mapper.readValue(getResponse["body"] as String)
            val expected = BEGREP_2.copy(
                anbefaltTerm = CHANGE_REQUEST_4.anbefaltTerm,
                tillattTerm = CHANGE_REQUEST_4.tillattTerm,
                frarådetTerm = CHANGE_REQUEST_4.frarådetTerm,
                definisjon = CHANGE_REQUEST_4.definisjon,
                endringslogelement = Endringslogelement(endretAv = "TEST USER", endringstidspunkt = result.endringslogelement!!.endringstidspunkt)
            )
            assertEquals(expected, result)
        }

        @Test
        fun acceptOfChangeRequestWithNoAssociatedConceptCreatesNewConcept() {
            val rsp = authorizedRequest("/123456789/endringsforslag/${CHANGE_REQUEST_5.id}/accept", port, null, JwtToken(Access.ORG_WRITE).toString(), HttpMethod.POST )
            assertEquals(HttpStatus.OK.value(), rsp["status"])

            val responseHeaders: HttpHeaders = rsp["header"] as HttpHeaders
            val location = responseHeaders.location
            assertNotNull(location)

            val getResponse = authorizedRequest(location.toString(), port, null, JwtToken(Access.ORG_READ).toString(), HttpMethod.GET)
            assertEquals(HttpStatus.OK.value(), getResponse["status"])
            val result: Begrep = mapper.readValue(getResponse["body"] as String)
            val expected = Begrep(
                id = result.id,
                originaltBegrep = result.id,
                status = Status.UTKAST,
                erPublisert = false,
                gjeldendeRevisjon = null,
                revisjonAvSistPublisert = true,
                versjonsnr = SemVer(0, 0, 1),
                ansvarligVirksomhet = Virksomhet(
                    id = "123456789"
                ),
                interneFelt = null,
                opprettet = result.opprettet,
                opprettetAv = "TEST USER",
                anbefaltTerm = CHANGE_REQUEST_5.anbefaltTerm,
                tillattTerm = CHANGE_REQUEST_5.tillattTerm,
                frarådetTerm = CHANGE_REQUEST_5.frarådetTerm,
                definisjon = CHANGE_REQUEST_5.definisjon,
                endringslogelement = Endringslogelement(endretAv = "TEST USER", endringstidspunkt = result.endringslogelement!!.endringstidspunkt)
            )
            assertEquals(expected, result)
        }
    }
}
