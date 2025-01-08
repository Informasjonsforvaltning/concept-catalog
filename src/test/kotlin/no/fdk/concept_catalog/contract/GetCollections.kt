package no.fdk.concept_catalog.contract

import com.fasterxml.jackson.module.kotlin.readValue
import no.fdk.concept_catalog.ContractTestsBase
import no.fdk.concept_catalog.model.Begrepssamling
import no.fdk.concept_catalog.utils.*
import no.fdk.concept_catalog.utils.jwk.Access
import no.fdk.concept_catalog.utils.jwk.JwtToken
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals

@Tag("contract")
class GetCollections : ContractTestsBase() {

    @Test
    fun `Unauthorized when access token is not included`() {
        val response = authorizedRequest("/begrepssamlinger", port, null, null, HttpMethod.GET)

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response["status"])
    }

    @Test
    fun `All collections for root access`() {
        operations.insertAll(
            listOf(
                BEGREP_0,
                BEGREP_1,
                BEGREP_2,
                BEGREP_WRONG_ORG,
                BEGREP_TO_BE_DELETED,
                BEGREP_TO_BE_UPDATED,
                BEGREP_4,
                BEGREP_5,
                BEGREP_0_OLD,
                BEGREP_6,
                BEGREP_HAS_REVISION,
                BEGREP_UNPUBLISHED_REVISION,
                BEGREP_HAS_MULTIPLE_REVISIONS,
                BEGREP_UNPUBLISHED_REVISION_MULTIPLE_FIRST,
                BEGREP_UNPUBLISHED_REVISION_MULTIPLE_SECOND
            )
        )

        val response =
            authorizedRequest("/begrepssamlinger", port, null, JwtToken(Access.ROOT).toString(), HttpMethod.GET)

        assertEquals(HttpStatus.OK.value(), response["status"])

        val result: List<Begrepssamling> = mapper.readValue(response["body"] as String)

        assertEquals(
            listOf(
                Begrepssamling("111111111", 3),
                Begrepssamling("111222333", 2),
                Begrepssamling("123456789", 3),
                Begrepssamling("222222222", 1),
                Begrepssamling("987654321", 1),
                Begrepssamling("999888777", 1)
            ), result.sortedBy { it.id })
    }

    @Test
    fun `Only permitted collections for write access`() {
        operations.insertAll(
            listOf(
                BEGREP_0,
                BEGREP_1,
                BEGREP_2,
                BEGREP_WRONG_ORG,
                BEGREP_TO_BE_DELETED,
                BEGREP_TO_BE_UPDATED,
                BEGREP_4,
                BEGREP_5,
                BEGREP_0_OLD,
                BEGREP_6,
                BEGREP_HAS_REVISION,
                BEGREP_UNPUBLISHED_REVISION,
                BEGREP_HAS_MULTIPLE_REVISIONS,
                BEGREP_UNPUBLISHED_REVISION_MULTIPLE_FIRST,
                BEGREP_UNPUBLISHED_REVISION_MULTIPLE_SECOND
            )
        )

        val response =
            authorizedRequest("/begrepssamlinger", port, null, JwtToken(Access.ORG_WRITE).toString(), HttpMethod.GET)

        assertEquals(HttpStatus.OK.value(), response["status"])

        val result: List<Begrepssamling> = mapper.readValue(response["body"] as String)

        assertEquals(
            listOf(
                Begrepssamling("111111111", 3),
                Begrepssamling("111222333", 2),
                Begrepssamling("123456789", 3),
                Begrepssamling("222222222", 1)
            ), result.sortedBy { it.id })
    }

    @Test
    fun `Only permitted collections for read access`() {
        operations.insertAll(
            listOf(
                BEGREP_0,
                BEGREP_1,
                BEGREP_2,
                BEGREP_WRONG_ORG,
                BEGREP_TO_BE_DELETED,
                BEGREP_TO_BE_UPDATED,
                BEGREP_4,
                BEGREP_5,
                BEGREP_0_OLD,
                BEGREP_6,
                BEGREP_HAS_REVISION,
                BEGREP_UNPUBLISHED_REVISION,
                BEGREP_HAS_MULTIPLE_REVISIONS,
                BEGREP_UNPUBLISHED_REVISION_MULTIPLE_FIRST,
                BEGREP_UNPUBLISHED_REVISION_MULTIPLE_SECOND
            )
        )

        val response =
            authorizedRequest("/begrepssamlinger", port, null, JwtToken(Access.ORG_READ).toString(), HttpMethod.GET)

        assertEquals(HttpStatus.OK.value(), response["status"])

        val result: List<Begrepssamling> = mapper.readValue(response["body"] as String)

        assertEquals(
            listOf(
                Begrepssamling("111111111", 3),
                Begrepssamling("111222333", 2),
                Begrepssamling("123456789", 3)
            ), result.sortedBy { it.id })
    }
}
