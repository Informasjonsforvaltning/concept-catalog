package no.fdk.concept_catalog.contract

import com.fasterxml.jackson.module.kotlin.readValue
import kotlin.test.assertEquals
import no.fdk.concept_catalog.configuration.JacksonConfigurer
import no.fdk.concept_catalog.model.*
import no.fdk.concept_catalog.utils.*
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

            val expected = listOf(
                CHANGE_REQUEST_0.copy(timeForProposal = resultRead[0].timeForProposal),
                CHANGE_REQUEST_1.copy(timeForProposal = resultRead[1].timeForProposal),
                CHANGE_REQUEST_2.copy(timeForProposal = resultRead[2].timeForProposal))
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

            assertEquals(listOf(CHANGE_REQUEST_2.copy(timeForProposal = resultOpen[0].timeForProposal)), resultOpen)
            assertEquals(listOf(CHANGE_REQUEST_1.copy(timeForProposal = resultRejected[0].timeForProposal)), resultRejected)
            assertEquals(listOf(CHANGE_REQUEST_0.copy(timeForProposal = resultAccepted[0].timeForProposal)), resultAccepted)
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

            val expected = CHANGE_REQUEST_0.copy(timeForProposal = resultRead.timeForProposal)
            assertEquals(expected, resultRead)
            assertEquals(expected, resultWrite)
        }

        @Test
        fun getChangeRequestByConceptId()  {
            val rspWrite = authorizedRequest("/123456789/endringsforslag?concept=${BEGREP_2.id}", port, null, JwtToken(Access.ORG_READ).toString(), HttpMethod.GET )
            assertEquals(HttpStatus.OK.value(), rspWrite["status"])
            val result: List<ChangeRequest> = mapper.readValue(rspWrite["body"] as String)
            val expected = listOf(CHANGE_REQUEST_4, CHANGE_REQUEST_6)
            assertEquals(expected, result)
        }

        @Test
        fun getChangeRequestByConceptIdAndStatus()  {
            val rspWrite = authorizedRequest("/123456789/endringsforslag?concept=${BEGREP_2.id}&status=open", port, null, JwtToken(Access.ORG_READ).toString(), HttpMethod.GET )
            assertEquals(HttpStatus.OK.value(), rspWrite["status"])
            val result: List<ChangeRequest> = mapper.readValue(rspWrite["body"] as String)
            val expected = listOf(CHANGE_REQUEST_4)
            assertEquals(expected, result)
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
            val rsp = authorizedRequest(path+"?concept=${BEGREP_0.originaltBegrep}", port, null, null, HttpMethod.POST )
            assertEquals(HttpStatus.UNAUTHORIZED.value(), rsp["status"])
        }

        @Test
        fun forbiddenWhenAuthorizedForOtherCatalog() {
            val rsp = authorizedRequest(path, port, mapper.writeValueAsString(CHANGE_REQUEST_UPDATE_BODY_NEW), JwtToken(Access.WRONG_ORG).toString(), HttpMethod.POST )

            assertEquals(HttpStatus.FORBIDDEN.value(), rsp["status"])
        }

        @Test
        fun badRequestWhenAttemptingToRequestChangesOnNonOriginalId() {

            val rsp = authorizedRequest(
                "/${BEGREP_0.ansvarligVirksomhet.id}/endringsforslag?concept=id0-old",
                port,
                null,
                JwtToken(Access.ORG_WRITE).toString(),
                HttpMethod.POST
            )
            assertEquals(HttpStatus.BAD_REQUEST.value(), rsp["status"])
        }

        @Test
        fun badRequestForNonValidCatalogId() {
            val rsp = authorizedRequest(
                "/invalid/endringsforslag",
                port,
                null,
                JwtToken(Access.WRONG_ORG).toString(),
                HttpMethod.POST
            )
            assertEquals(HttpStatus.BAD_REQUEST.value(), rsp["status"])
        }

        @Test
        fun ableToCreateChangeRequest() {
            val rsp0 = authorizedRequest(path, port, mapper.writeValueAsString(CHANGE_REQUEST_UPDATE_BODY_NEW), JwtToken(Access.ORG_READ).toString(), HttpMethod.POST )
            assertEquals(HttpStatus.CREATED.value(), rsp0["status"])

            val responseHeaders0: HttpHeaders = rsp0["header"] as HttpHeaders
            val location0 = responseHeaders0.location
            assertNotNull(location0)

            val getResponse0 = authorizedRequest(location0!!.toString(), port, null, JwtToken(Access.ORG_READ).toString(), HttpMethod.GET)
            assertEquals(HttpStatus.OK.value(), getResponse0["status"])
            val result0: ChangeRequest = mapper.readValue(getResponse0["body"] as String)

            val expected0 = ChangeRequest(
                id = result0.id,
                catalogId = "111111111",
                conceptId = null,
                status = ChangeRequestStatus.OPEN,
                operations = emptyList(),
                proposedBy = User(id="1924782563", name="TEST USER", email=null),
                timeForProposal = result0.timeForProposal,
                title = "Nytt endringsforslag"
            )
            assertEquals(expected0, result0)
        }
    }

    @Nested
    internal inner class UpdateChangeRequest {
        private val path = "/111111111/endringsforslag/${CHANGE_REQUEST_0.id}"

        @Test
        fun unauthorizedWhenMissingToken() {
            val body = listOf(JsonPatchOperation(op = OpEnum.ADD, "/tillattTerm", mapOf(Pair("nb", "tillatt nb"), Pair("nn", "tillatt nn"))))
            val rsp = authorizedRequest(path, port, mapper.writeValueAsString(body), null, HttpMethod.POST )

            assertEquals(HttpStatus.UNAUTHORIZED.value(), rsp["status"])
        }

        @Test
        fun forbiddenWhenAuthorizedForOtherCatalog() {
            val rsp = authorizedRequest(path, port, mapper.writeValueAsString(CHANGE_REQUEST_UPDATE_BODY_UPDATE), JwtToken(Access.WRONG_ORG).toString(), HttpMethod.POST )

            assertEquals(HttpStatus.FORBIDDEN.value(), rsp["status"])
        }

        @Test
        fun notFoundForInvalidId() {

            val rsp = authorizedRequest("/111111111/endringsforslag/invalid", port, mapper.writeValueAsString(CHANGE_REQUEST_UPDATE_BODY_UPDATE), JwtToken(Access.ORG_WRITE).toString(), HttpMethod.POST )

            assertEquals(HttpStatus.NOT_FOUND.value(), rsp["status"])
        }

        @Test
        fun ableToUpdateChangeRequest() {
            val rsp = authorizedRequest(path, port, mapper.writeValueAsString(CHANGE_REQUEST_UPDATE_BODY_UPDATE), JwtToken(Access.ORG_READ).toString(), HttpMethod.POST )
            assertEquals(HttpStatus.OK.value(), rsp["status"])

            val result: ChangeRequest = mapper.readValue(rsp["body"] as String)
            val expected = CHANGE_REQUEST_0.copy(operations = CHANGE_REQUEST_UPDATE_BODY_UPDATE.operations, title = CHANGE_REQUEST_UPDATE_BODY_UPDATE.title, timeForProposal = result.timeForProposal)
            assertEquals(expected, result)
        }

        @Test
        fun badRequestWhenUpdatingIdFields() {
            val bodyId = listOf(JsonPatchOperation(op = OpEnum.REPLACE, "/id", "123456"))
            val bodyCatalogId = listOf(JsonPatchOperation(op = OpEnum.REPLACE, "/catalogId", "123456"))
            val bodyConceptId = listOf(JsonPatchOperation(op = OpEnum.ADD, "/conceptId", "123456"))
            val rspId = authorizedRequest(path, port, mapper.writeValueAsString(bodyId), JwtToken(Access.ORG_WRITE).toString(), HttpMethod.POST )
            val rspCatalogId = authorizedRequest(path, port, mapper.writeValueAsString(bodyCatalogId), JwtToken(Access.ORG_WRITE).toString(), HttpMethod.POST )
            val rspConceptId = authorizedRequest(path, port, mapper.writeValueAsString(bodyConceptId), JwtToken(Access.ORG_WRITE).toString(), HttpMethod.POST )

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

            val getResponse = authorizedRequest(location!!.toString(), port, null, JwtToken(Access.ORG_READ).toString(), HttpMethod.GET)
            assertEquals(HttpStatus.OK.value(), getResponse["status"])
            val result: Begrep = mapper.readValue(getResponse["body"] as String)
            val expected = BEGREP_0.copy(
                id = result.id,
                versjonsnr = SemVer(1, 0, 2),
                erPublisert = false,
                erSistPublisert = false,
                publiseringsTidspunkt = null,
                revisjonAv = BEGREP_0.id,
                revisjonAvSistPublisert = true,
                opprettet = result.opprettet,
                opprettetAv = "TEST USER",
                endringslogelement = Endringslogelement(endretAv = "TEST USER", endringstidspunkt = result.endringslogelement!!.endringstidspunkt),
                status = Status.UTKAST,
                assignedUser = "newUserId",
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

            val getResponse = authorizedRequest(location!!.toString(), port, null, JwtToken(Access.ORG_READ).toString(), HttpMethod.GET)
            assertEquals(HttpStatus.OK.value(), getResponse["status"])
            val result: Begrep = mapper.readValue(getResponse["body"] as String)
            val expected = BEGREP_2.copy(
                endringslogelement = Endringslogelement(endretAv = "TEST USER", endringstidspunkt = result.endringslogelement!!.endringstidspunkt),
                assignedUser = "newUserId"
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

            val getResponse = authorizedRequest(location!!.toString(), port, null, JwtToken(Access.ORG_READ).toString(), HttpMethod.GET)
            assertEquals(HttpStatus.OK.value(), getResponse["status"])
            val result: Begrep = mapper.readValue(getResponse["body"] as String)
            val expected = Begrep(
                id = result.id,
                originaltBegrep = result.id,
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
                endringslogelement = Endringslogelement(endretAv = "TEST USER", endringstidspunkt = result.endringslogelement!!.endringstidspunkt),
                status = Status.UTKAST,
                assignedUser="newUserId"
            )
            assertEquals(expected, result)
        }

        @Test
        fun acceptIsRevertedWhenUpdateOfHistoryServiceFails() {
            val rsp = authorizedRequest("/111111111/endringsforslag/${CHANGE_REQUEST_2.id}/accept", port, null, JwtToken(Access.ORG_WRITE).toString(), HttpMethod.POST )
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), rsp["status"])

            val rspGet = authorizedRequest("/111111111/endringsforslag/${CHANGE_REQUEST_2.id}", port, null, JwtToken(Access.ORG_WRITE).toString(), HttpMethod.GET )

            assertEquals(HttpStatus.OK.value(), rspGet["status"])
            val resultGet: ChangeRequest = mapper.readValue(rspGet["body"] as String)

            assertEquals(CHANGE_REQUEST_2, resultGet)
        }
    }
}
