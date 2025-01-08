package no.fdk.concept_catalog.contract

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.tomakehurst.wiremock.client.WireMock.*
import no.fdk.concept_catalog.ContractTestsBase
import no.fdk.concept_catalog.model.*
import no.fdk.concept_catalog.utils.*
import no.fdk.concept_catalog.utils.jwk.Access
import no.fdk.concept_catalog.utils.jwk.JwtToken
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals

@Tag("contract")
class ChangeRequests : ContractTestsBase() {

    @Nested
    inner class GetChangeRequests {
        val path = "/111111111/endringsforslag"

        @Test
        fun unauthorizedWhenMissingToken() {
            val response = authorizedRequest(path, port, null, null, HttpMethod.GET)

            assertEquals(HttpStatus.UNAUTHORIZED.value(), response["status"])
        }

        @Test
        fun forbiddenWhenAuthorizedForOtherCatalog() {
            val response = authorizedRequest(path, port, null, JwtToken(Access.WRONG_ORG).toString(), HttpMethod.GET)

            assertEquals(HttpStatus.FORBIDDEN.value(), response["status"])
        }

        @Test
        fun ableToGetChangeRequestsForAllOrgRoles() {
            operations.insertAll(listOf(CHANGE_REQUEST_0, CHANGE_REQUEST_1, CHANGE_REQUEST_2))

            val responseRead = authorizedRequest(path, port, null, JwtToken(Access.ORG_READ).toString(), HttpMethod.GET)
            val responseWrite =
                authorizedRequest(path, port, null, JwtToken(Access.ORG_WRITE).toString(), HttpMethod.GET)

            assertEquals(HttpStatus.OK.value(), responseRead["status"])
            assertEquals(HttpStatus.OK.value(), responseWrite["status"])

            val resultRead: List<ChangeRequest> = mapper.readValue(responseRead["body"] as String)
            val resultWrite: List<ChangeRequest> = mapper.readValue(responseWrite["body"] as String)

            val expected = listOf(
                CHANGE_REQUEST_0.copy(timeForProposal = resultRead[0].timeForProposal),
                CHANGE_REQUEST_1.copy(timeForProposal = resultRead[1].timeForProposal),
                CHANGE_REQUEST_2.copy(timeForProposal = resultRead[2].timeForProposal)
            )

            assertEquals(expected, resultRead)
            assertEquals(expected, resultWrite)
        }

        @Test
        fun ableToGetChangeRequestsFilteredByStatus() {
            operations.insertAll(listOf(CHANGE_REQUEST_0, CHANGE_REQUEST_1, CHANGE_REQUEST_2))

            val responseOpen =
                authorizedRequest("$path?status=OPEN", port, null, JwtToken(Access.ORG_READ).toString(), HttpMethod.GET)

            val responseRejected = authorizedRequest(
                "$path?status=rejected",
                port,
                null,
                JwtToken(Access.ORG_READ).toString(),
                HttpMethod.GET
            )

            val responseAccepted = authorizedRequest(
                "$path?status=aCCepTed",
                port,
                null,
                JwtToken(Access.ORG_READ).toString(),
                HttpMethod.GET
            )

            assertEquals(HttpStatus.OK.value(), responseOpen["status"])
            assertEquals(HttpStatus.OK.value(), responseRejected["status"])
            assertEquals(HttpStatus.OK.value(), responseAccepted["status"])

            val resultOpen: List<ChangeRequest> = mapper.readValue(responseOpen["body"] as String)
            assertEquals(listOf(CHANGE_REQUEST_2.copy(timeForProposal = resultOpen[0].timeForProposal)), resultOpen)

            val resultRejected: List<ChangeRequest> = mapper.readValue(responseRejected["body"] as String)
            assertEquals(
                listOf(CHANGE_REQUEST_1.copy(timeForProposal = resultRejected[0].timeForProposal)),
                resultRejected
            )

            val resultAccepted: List<ChangeRequest> = mapper.readValue(responseAccepted["body"] as String)
            assertEquals(
                listOf(CHANGE_REQUEST_0.copy(timeForProposal = resultAccepted[0].timeForProposal)),
                resultAccepted
            )
        }
    }

    @Nested
    inner class GetChangeRequestById {
        val path = "/111111111/endringsforslag/${CHANGE_REQUEST_0.id}"

        @Test
        fun unauthorizedWhenMissingToken() {
            val response = authorizedRequest(path, port, null, null, HttpMethod.GET)

            assertEquals(HttpStatus.UNAUTHORIZED.value(), response["status"])
        }

        @Test
        fun forbiddenWhenAuthorizedForOtherCatalog() {
            val response = authorizedRequest(path, port, null, JwtToken(Access.WRONG_ORG).toString(), HttpMethod.GET)

            assertEquals(HttpStatus.FORBIDDEN.value(), response["status"])
        }

        @Test
        fun notFoundForInvalidId() {
            val response = authorizedRequest(
                "/111111111/endringsforslag/invalid",
                port,
                null,
                JwtToken(Access.ORG_READ).toString(),
                HttpMethod.GET
            )

            assertEquals(HttpStatus.NOT_FOUND.value(), response["status"])
        }

        @Test
        fun ableToGetChangeRequestForAllOrgRoles() {
            operations.insert(CHANGE_REQUEST_0)

            val responseRead = authorizedRequest(path, port, null, JwtToken(Access.ORG_READ).toString(), HttpMethod.GET)
            val responseWrite =
                authorizedRequest(path, port, null, JwtToken(Access.ORG_WRITE).toString(), HttpMethod.GET)

            assertEquals(HttpStatus.OK.value(), responseRead["status"])
            assertEquals(HttpStatus.OK.value(), responseWrite["status"])

            val resultRead: ChangeRequest = mapper.readValue(responseRead["body"] as String)
            val resultWrite: ChangeRequest = mapper.readValue(responseWrite["body"] as String)

            val expected = CHANGE_REQUEST_0.copy(timeForProposal = resultRead.timeForProposal)
            assertEquals(expected, resultRead)
            assertEquals(expected, resultWrite)
        }

        @Test
        fun getChangeRequestByConceptId() {
            operations.insertAll(listOf(BEGREP_2, CHANGE_REQUEST_4, CHANGE_REQUEST_6))

            val responseWrite = authorizedRequest(
                "/123456789/endringsforslag?concept=${BEGREP_2.id}",
                port,
                null,
                JwtToken(Access.ORG_READ).toString(),
                HttpMethod.GET
            )

            assertEquals(HttpStatus.OK.value(), responseWrite["status"])

            val result: List<ChangeRequest> = mapper.readValue(responseWrite["body"] as String)
            val expected = listOf(CHANGE_REQUEST_4, CHANGE_REQUEST_6)

            assertEquals(expected, result)
        }

        @Test
        fun getChangeRequestByConceptIdAndStatus() {
            operations.insertAll(listOf(BEGREP_2, CHANGE_REQUEST_4))

            val responseWrite = authorizedRequest(
                "/123456789/endringsforslag?concept=${BEGREP_2.id}&status=open",
                port,
                null,
                JwtToken(Access.ORG_READ).toString(),
                HttpMethod.GET
            )

            assertEquals(HttpStatus.OK.value(), responseWrite["status"])

            val result: List<ChangeRequest> = mapper.readValue(responseWrite["body"] as String)
            val expected = listOf(CHANGE_REQUEST_4)

            assertEquals(expected, result)
        }
    }

    @Nested
    inner class DeleteChangeRequest {
        val path = "/111111111/endringsforslag/${CHANGE_REQUEST_0.id}"

        @Test
        fun unauthorizedWhenMissingToken() {
            val response = authorizedRequest(path, port, null, null, HttpMethod.DELETE)

            assertEquals(HttpStatus.UNAUTHORIZED.value(), response["status"])
        }

        @Test
        fun forbiddenWhenAuthorizedForOtherCatalog() {
            val response = authorizedRequest(path, port, null, JwtToken(Access.WRONG_ORG).toString(), HttpMethod.DELETE)

            assertEquals(HttpStatus.FORBIDDEN.value(), response["status"])
        }

        @Test
        fun forbiddenWhenAuthorizedForReadAccess() {
            val response = authorizedRequest(path, port, null, JwtToken(Access.ORG_READ).toString(), HttpMethod.DELETE)

            assertEquals(HttpStatus.FORBIDDEN.value(), response["status"])
        }

        @Test
        fun notFoundForInvalidId() {
            val response = authorizedRequest(
                "/111111111/endringsforslag/invalid",
                port,
                null,
                JwtToken(Access.ORG_WRITE).toString(),
                HttpMethod.DELETE
            )

            assertEquals(HttpStatus.NOT_FOUND.value(), response["status"])
        }

        @Test
        fun ableToDeleteChangeRequest() {
            operations.insert(CHANGE_REQUEST_0)

            val response = authorizedRequest(path, port, null, JwtToken(Access.ORG_WRITE).toString(), HttpMethod.DELETE)

            assertEquals(HttpStatus.NO_CONTENT.value(), response["status"])
        }
    }

    @Nested
    inner class CreateChangeRequest {
        val path = "/111111111/endringsforslag"

        @Test
        fun unauthorizedWhenMissingToken() {
            val response =
                authorizedRequest(path + "?concept=${BEGREP_0.originaltBegrep}", port, null, null, HttpMethod.POST)

            assertEquals(HttpStatus.UNAUTHORIZED.value(), response["status"])
        }

        @Test
        fun forbiddenWhenAuthorizedForOtherCatalog() {
            val response = authorizedRequest(
                path,
                port,
                mapper.writeValueAsString(CHANGE_REQUEST_UPDATE_BODY_NEW),
                JwtToken(Access.WRONG_ORG).toString(),
                HttpMethod.POST
            )

            assertEquals(HttpStatus.FORBIDDEN.value(), response["status"])
        }

        @Test
        fun badRequestWhenAttemptingToRequestChangesOnNonOriginalId() {
            val response = authorizedRequest(
                "/${BEGREP_0.ansvarligVirksomhet.id}/endringsforslag?concept=id0-old",
                port,
                null,
                JwtToken(Access.ORG_WRITE).toString(),
                HttpMethod.POST
            )

            assertEquals(HttpStatus.BAD_REQUEST.value(), response["status"])
        }

        @Test
        fun badRequestForNonValidCatalogId() {
            val response = authorizedRequest(
                "/invalid/endringsforslag",
                port,
                null,
                JwtToken(Access.WRONG_ORG).toString(),
                HttpMethod.POST
            )

            assertEquals(HttpStatus.BAD_REQUEST.value(), response["status"])
        }

        @Test
        fun ableToCreateSuggestionForNewConcept() {
            val rsp0 = authorizedRequest(
                path,
                port,
                mapper.writeValueAsString(CHANGE_REQUEST_UPDATE_BODY_NEW),
                JwtToken(Access.ORG_READ).toString(),
                HttpMethod.POST
            )
            assertEquals(HttpStatus.CREATED.value(), rsp0["status"])

            val responseHeaders0: HttpHeaders = rsp0["header"] as HttpHeaders
            val location0 = responseHeaders0.location
            assertNotNull(location0)

            val getResponse0 = authorizedRequest(
                location0!!.toString(),
                port,
                mapper.writeValueAsString(CHANGE_REQUEST_UPDATE_BODY_NEW),
                JwtToken(Access.ORG_READ).toString(),
                HttpMethod.GET
            )
            assertEquals(HttpStatus.OK.value(), getResponse0["status"])
            val result0: ChangeRequest = mapper.readValue(getResponse0["body"] as String)

            val expected0 = ChangeRequest(
                id = result0.id,
                catalogId = "111111111",
                conceptId = null,
                status = ChangeRequestStatus.OPEN,
                operations = CHANGE_REQUEST_UPDATE_BODY_NEW.operations,
                proposedBy = User(id = "1924782563", name = "TEST USER", email = null),
                timeForProposal = result0.timeForProposal,
                title = "Forslag til nytt begrep"
            )
            assertEquals(expected0, result0)
        }

        @Test
        fun ableToCreateChangeRequest() {
            operations.insertAll(listOf(BEGREP_TO_BE_UPDATED, CHANGE_REQUEST_UPDATE_BODY_0))

            val response = authorizedRequest(
                path,
                port,
                mapper.writeValueAsString(CHANGE_REQUEST_UPDATE_BODY_0),
                JwtToken(Access.ORG_READ).toString(),
                HttpMethod.POST
            )

            assertEquals(HttpStatus.CREATED.value(), response["status"])

            val responseHeaders: HttpHeaders = response["header"] as HttpHeaders
            val locationHeader = responseHeaders.location

            assertNotNull(locationHeader)

            val locationResponse = authorizedRequest(
                locationHeader!!.toString(),
                port,
                null,
                JwtToken(Access.ORG_READ).toString(),
                HttpMethod.GET
            )

            assertEquals(HttpStatus.OK.value(), locationResponse["status"])

            val location: ChangeRequest = mapper.readValue(locationResponse["body"] as String)

            val expected = ChangeRequest(
                id = location.id,
                catalogId = "111111111",
                conceptId = BEGREP_TO_BE_UPDATED.id,
                status = ChangeRequestStatus.OPEN,
                operations = CHANGE_REQUEST_UPDATE_BODY_0.operations,
                proposedBy = User(id = "1924782563", name = "TEST USER", email = null),
                timeForProposal = location.timeForProposal,
                title = "Endringsforslag 7"
            )

            assertEquals(expected, location)
        }
    }

    @Nested
    internal inner class UpdateChangeRequest {
        val path = "/111111111/endringsforslag/${CHANGE_REQUEST_0.id}"

        @Test
        fun unauthorizedWhenMissingToken() {
            val operations = listOf(
                JsonPatchOperation(
                    op = OpEnum.ADD,
                    "/tillattTerm",
                    mapOf(Pair("nb", "tillatt nb"), Pair("nn", "tillatt nn"))
                )
            )

            val response = authorizedRequest(path, port, mapper.writeValueAsString(operations), null, HttpMethod.POST)

            assertEquals(HttpStatus.UNAUTHORIZED.value(), response["status"])
        }

        @Test
        fun forbiddenWhenAuthorizedForOtherCatalog() {
            val response = authorizedRequest(
                path,
                port,
                mapper.writeValueAsString(CHANGE_REQUEST_UPDATE_BODY_UPDATE),
                JwtToken(Access.WRONG_ORG).toString(),
                HttpMethod.POST
            )

            assertEquals(HttpStatus.FORBIDDEN.value(), response["status"])
        }

        @Test
        fun notFoundForInvalidId() {
            val response = authorizedRequest(
                "/111111111/endringsforslag/invalid",
                port,
                mapper.writeValueAsString(CHANGE_REQUEST_UPDATE_BODY_UPDATE),
                JwtToken(Access.ORG_WRITE).toString(),
                HttpMethod.POST
            )

            assertEquals(HttpStatus.NOT_FOUND.value(), response["status"])
        }

        @Test
        fun ableToUpdateChangeRequest() {
            operations.insertAll(listOf(CHANGE_REQUEST_0, CHANGE_REQUEST_UPDATE_BODY_UPDATE))

            val response = authorizedRequest(
                path,
                port,
                mapper.writeValueAsString(CHANGE_REQUEST_UPDATE_BODY_UPDATE),
                JwtToken(Access.ORG_READ).toString(),
                HttpMethod.POST
            )

            assertEquals(HttpStatus.OK.value(), response["status"])

            val result: ChangeRequest = mapper.readValue(response["body"] as String)

            val expected = CHANGE_REQUEST_0.copy(
                operations = CHANGE_REQUEST_UPDATE_BODY_UPDATE.operations,
                title = CHANGE_REQUEST_UPDATE_BODY_UPDATE.title,
                timeForProposal = result.timeForProposal
            )

            assertEquals(expected, result)
        }

        @Test
        fun badRequestWhenUpdatingIdFields() {
            val errMsg = "Patch of paths [/id, /catalogId, /conceptId, /status] is not permitted"

            val illegalIdReplace = CHANGE_REQUEST_UPDATE_BODY_0.copy(
                operations = listOf(
                    JsonPatchOperation(
                        op = OpEnum.REPLACE,
                        "/id",
                        "123456"
                    )
                )
            )

            val illegalIdResponse = authorizedRequest(
                path,
                port,
                mapper.writeValueAsString(illegalIdReplace),
                JwtToken(Access.ORG_WRITE).toString(),
                HttpMethod.POST
            )

            assertEquals(HttpStatus.BAD_REQUEST.value(), illegalIdResponse["status"])
            assertEquals(
                mapper.readValue<HashMap<String, Any>>(illegalIdResponse["body"] as String)["message"] as String,
                errMsg
            )

            val illegalCatalogIdReplace = CHANGE_REQUEST_UPDATE_BODY_0.copy(
                operations = listOf(
                    JsonPatchOperation(
                        op = OpEnum.REPLACE,
                        "/catalogId",
                        "123456"
                    )
                )
            )

            val illegalCatalogIdResponse = authorizedRequest(
                path,
                port,
                mapper.writeValueAsString(illegalCatalogIdReplace),
                JwtToken(Access.ORG_WRITE).toString(),
                HttpMethod.POST
            )

            assertEquals(HttpStatus.BAD_REQUEST.value(), illegalCatalogIdResponse["status"])
            assertEquals(
                mapper.readValue<HashMap<String, Any>>(illegalCatalogIdResponse["body"] as String)["message"] as String,
                errMsg
            )

            val illegalConceptIdAdd = CHANGE_REQUEST_UPDATE_BODY_0.copy(
                operations = listOf(
                    JsonPatchOperation(
                        op = OpEnum.ADD,
                        "/conceptId",
                        "123456"
                    )
                )
            )

            val illegalConceptIdResponse = authorizedRequest(
                path,
                port,
                mapper.writeValueAsString(illegalConceptIdAdd),
                JwtToken(Access.ORG_WRITE).toString(),
                HttpMethod.POST
            )

            assertEquals(HttpStatus.BAD_REQUEST.value(), illegalConceptIdResponse["status"])
            assertEquals(
                mapper.readValue<HashMap<String, Any>>(illegalConceptIdResponse["body"] as String)["message"] as String,
                errMsg
            )
        }
    }

    @Nested
    inner class RejectChangeRequest {
        val path = "/111111111/endringsforslag/${CHANGE_REQUEST_2.id}/reject"

        @Test
        fun unauthorizedWhenMissingToken() {
            val response = authorizedRequest(path, port, null, null, HttpMethod.POST)

            assertEquals(HttpStatus.UNAUTHORIZED.value(), response["status"])
        }

        @Test
        fun forbiddenWhenAuthorizedForOtherCatalog() {
            val response = authorizedRequest(path, port, null, JwtToken(Access.WRONG_ORG).toString(), HttpMethod.POST)

            assertEquals(HttpStatus.FORBIDDEN.value(), response["status"])
        }

        @Test
        fun forbiddenWhenAuthorizedForReadAccess() {
            val response = authorizedRequest(path, port, null, JwtToken(Access.ORG_READ).toString(), HttpMethod.POST)

            assertEquals(HttpStatus.FORBIDDEN.value(), response["status"])
        }

        @Test
        fun notFoundForInvalidId() {
            val response = authorizedRequest(
                "/111111111/endringsforslag/invalid/reject",
                port,
                null,
                JwtToken(Access.ORG_WRITE).toString(),
                HttpMethod.POST
            )

            assertEquals(HttpStatus.NOT_FOUND.value(), response["status"])
        }

        @Test
        fun badRequestWhenRejectingNonOpen() {
            operations.insert(CHANGE_REQUEST_0)

            val alreadyAccepted = authorizedRequest(
                "/111111111/endringsforslag/${CHANGE_REQUEST_0.id}/reject",
                port,
                null,
                JwtToken(Access.ORG_WRITE).toString(),
                HttpMethod.POST
            )

            assertEquals(HttpStatus.BAD_REQUEST.value(), alreadyAccepted["status"])

            operations.insert(CHANGE_REQUEST_1)

            val alreadyRejected = authorizedRequest(
                "/111111111/endringsforslag/${CHANGE_REQUEST_1.id}/reject",
                port,
                null,
                JwtToken(Access.ORG_WRITE).toString(),
                HttpMethod.POST
            )

            assertEquals(HttpStatus.BAD_REQUEST.value(), alreadyRejected["status"])
        }

        @Test
        fun ableToRejectChangeRequest() {
            operations.insert(CHANGE_REQUEST_2)

            val response = authorizedRequest(path, port, null, JwtToken(Access.ORG_WRITE).toString(), HttpMethod.POST)

            assertEquals(HttpStatus.OK.value(), response["status"])
        }
    }

    @Nested
    inner class AcceptChangeRequest {
        val path = "/123456789/endringsforslag/${CHANGE_REQUEST_3.id}/accept"

        @Test
        fun unauthorizedWhenMissingToken() {
            val response = authorizedRequest(path, port, null, null, HttpMethod.POST)

            assertEquals(HttpStatus.UNAUTHORIZED.value(), response["status"])
        }

        @Test
        fun forbiddenWhenAuthorizedForOtherCatalog() {
            val response = authorizedRequest(path, port, null, JwtToken(Access.WRONG_ORG).toString(), HttpMethod.POST)

            assertEquals(HttpStatus.FORBIDDEN.value(), response["status"])
        }

        @Test
        fun forbiddenWhenAuthorizedForReadAccess() {
            val response = authorizedRequest(path, port, null, JwtToken(Access.ORG_READ).toString(), HttpMethod.POST)

            assertEquals(HttpStatus.FORBIDDEN.value(), response["status"])
        }

        @Test
        fun notFoundForInvalidId() {
            val response = authorizedRequest(
                "/111111111/endringsforslag/invalid/accept",
                port,
                null,
                JwtToken(Access.ORG_WRITE).toString(),
                HttpMethod.POST
            )

            assertEquals(HttpStatus.NOT_FOUND.value(), response["status"])
        }

        @Test
        fun badRequestWhenAcceptingNonOpen() {
            operations.insert(CHANGE_REQUEST_0)

            val alreadyAccepted = authorizedRequest(
                "/111111111/endringsforslag/${CHANGE_REQUEST_0.id}/accept",
                port,
                null,
                JwtToken(Access.ORG_WRITE).toString(),
                HttpMethod.POST
            )

            assertEquals(HttpStatus.BAD_REQUEST.value(), alreadyAccepted["status"])

            operations.insert(CHANGE_REQUEST_1)

            val alreadyRejected = authorizedRequest(
                "/111111111/endringsforslag/${CHANGE_REQUEST_1.id}/accept",
                port,
                null,
                JwtToken(Access.ORG_WRITE).toString(),
                HttpMethod.POST
            )

            assertEquals(HttpStatus.BAD_REQUEST.value(), alreadyRejected["status"])
        }

        @Test
        fun acceptOfChangeRequestToPublishedConceptCreatesNewRevision() {
            operations.insertAll(listOf(BEGREP_0, CHANGE_REQUEST_3))

            stubFor(post(urlMatching("/123456789/.*/updates")).willReturn(aResponse().withStatus(200)))

            val response = authorizedRequest(path, port, null, JwtToken(Access.ORG_WRITE).toString(), HttpMethod.POST)

            assertEquals(HttpStatus.OK.value(), response["status"])

            val responseHeaders: HttpHeaders = response["header"] as HttpHeaders
            val locationHeader = responseHeaders.location

            assertNotNull(locationHeader)

            val locationResponse = authorizedRequest(
                locationHeader!!.toString(),
                port,
                null,
                JwtToken(Access.ORG_READ).toString(),
                HttpMethod.GET
            )

            assertEquals(HttpStatus.OK.value(), locationResponse["status"])

            val location: Begrep = mapper.readValue(locationResponse["body"] as String)

            val expected = BEGREP_0.copy(
                id = location.id,
                versjonsnr = SemVer(1, 0, 2),
                erPublisert = false,
                erSistPublisert = false,
                sistPublisertId = BEGREP_0.id,
                publiseringsTidspunkt = null,
                revisjonAv = BEGREP_0.id,
                revisjonAvSistPublisert = true,
                endringslogelement = Endringslogelement(
                    endretAv = "TEST USER",
                    endringstidspunkt = location.endringslogelement!!.endringstidspunkt
                ),
                status = Status.UTKAST,
                assignedUser = "newUserId",
                internErstattesAv = listOf("id1")
            )

            assertEquals(expected, location)
        }

        @Test
        fun acceptOfChangeRequestToUnpublishedConceptUpdatesExistingConcept() {
            operations.insertAll(listOf(BEGREP_2, CHANGE_REQUEST_4))

            stubFor(post(urlMatching("/123456789/.*/updates")).willReturn(aResponse().withStatus(200)))

            val response = authorizedRequest(
                "/123456789/endringsforslag/${CHANGE_REQUEST_4.id}/accept",
                port,
                null,
                JwtToken(Access.ORG_WRITE).toString(),
                HttpMethod.POST
            )

            assertEquals(HttpStatus.OK.value(), response["status"])

            val responseHeaders: HttpHeaders = response["header"] as HttpHeaders
            val locationHeader = responseHeaders.location

            assertNotNull(locationHeader)

            val locationResponse = authorizedRequest(
                locationHeader!!.toString(),
                port,
                null,
                JwtToken(Access.ORG_READ).toString(),
                HttpMethod.GET
            )

            assertEquals(HttpStatus.OK.value(), locationResponse["status"])

            val location: Begrep = mapper.readValue(locationResponse["body"] as String)

            val expected = BEGREP_2.copy(
                endringslogelement = Endringslogelement(
                    endretAv = "TEST USER",
                    endringstidspunkt = location.endringslogelement!!.endringstidspunkt
                ),
                assignedUser = "newUserId"
            )

            assertEquals(expected, location)
        }

        @Test
        fun acceptOfChangeRequestWithNoAssociatedConceptCreatesNewConcept() {
            operations.insertAll(listOf(BEGREP_0, CHANGE_REQUEST_5))

            stubFor(post(urlMatching("/123456789/.*/updates")).willReturn(aResponse().withStatus(200)))

            val response = authorizedRequest(
                "/123456789/endringsforslag/${CHANGE_REQUEST_5.id}/accept",
                port,
                null,
                JwtToken(Access.ORG_WRITE).toString(),
                HttpMethod.POST
            )

            assertEquals(HttpStatus.OK.value(), response["status"])

            val responseHeaders: HttpHeaders = response["header"] as HttpHeaders
            val locationHeader = responseHeaders.location

            assertNotNull(locationHeader)

            val locationResponse = authorizedRequest(
                locationHeader!!.toString(),
                port,
                null,
                JwtToken(Access.ORG_READ).toString(),
                HttpMethod.GET
            )

            assertEquals(HttpStatus.OK.value(), locationResponse["status"])

            val location: Begrep = mapper.readValue(locationResponse["body"] as String)

            val expected = Begrep(
                id = location.id,
                originaltBegrep = location.id,
                erPublisert = false,
                gjeldendeRevisjon = null,
                revisjonAvSistPublisert = true,
                versjonsnr = SemVer(0, 1, 0),
                ansvarligVirksomhet = Virksomhet(
                    id = "123456789"
                ),
                interneFelt = null,
                opprettet = location.opprettet,
                opprettetAv = "TEST USER",
                endringslogelement = Endringslogelement(
                    endretAv = "TEST USER",
                    endringstidspunkt = location.endringslogelement!!.endringstidspunkt
                ),
                status = Status.UTKAST,
                assignedUser = "newUserId",
                internErstattesAv = null
            )

            assertEquals(expected, location)
        }

        @Test
        fun acceptIsRevertedWhenUpdateOfHistoryServiceFails() {
            operations.insert(CHANGE_REQUEST_2)

            val response = authorizedRequest(
                "/111111111/endringsforslag/${CHANGE_REQUEST_2.id}/accept",
                port,
                null,
                JwtToken(Access.ORG_WRITE).toString(),
                HttpMethod.POST
            )

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response["status"])

            val changeRequest = authorizedRequest(
                "/111111111/endringsforslag/${CHANGE_REQUEST_2.id}",
                port,
                null,
                JwtToken(Access.ORG_WRITE).toString(),
                HttpMethod.GET
            )

            assertEquals(HttpStatus.OK.value(), changeRequest["status"])

            val changeRequestBody: ChangeRequest = mapper.readValue(changeRequest["body"] as String)

            assertEquals(CHANGE_REQUEST_2, changeRequestBody)
        }
    }
}
