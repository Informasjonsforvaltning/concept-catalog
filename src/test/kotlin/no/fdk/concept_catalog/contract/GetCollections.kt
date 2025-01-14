package no.fdk.concept_catalog.contract

import com.fasterxml.jackson.module.kotlin.readValue
import no.fdk.concept_catalog.ContractTestsBase
import no.fdk.concept_catalog.model.Begrepssamling
import no.fdk.concept_catalog.utils.*
import no.fdk.concept_catalog.utils.Access
import no.fdk.concept_catalog.utils.JwtToken
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals

@Tag("contract")
class GetCollections : ContractTestsBase() {

    @Test
    fun `Unauthorized when access token is not included`() {
        val response = authorizedRequest("/begrepssamlinger", null, null, HttpMethod.GET)

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }

    @Test
    fun `All collections for root access`() {
        mongoOperations.insertAll(
            listOf(
                BEGREP_0.toDBO(),
                BEGREP_1.toDBO(),
                BEGREP_2.toDBO(),
                BEGREP_WRONG_ORG.toDBO(),
                BEGREP_TO_BE_DELETED.toDBO(),
                BEGREP_TO_BE_UPDATED.toDBO(),
                BEGREP_4.toDBO(),
                BEGREP_5.toDBO(),
                BEGREP_0_OLD.toDBO(),
                BEGREP_6.toDBO(),
                BEGREP_HAS_REVISION.toDBO(),
                BEGREP_UNPUBLISHED_REVISION.toDBO(),
                BEGREP_HAS_MULTIPLE_REVISIONS.toDBO(),
                BEGREP_UNPUBLISHED_REVISION_MULTIPLE_FIRST.toDBO(),
                BEGREP_UNPUBLISHED_REVISION_MULTIPLE_SECOND.toDBO()
            )
        )

        val response =
            authorizedRequest("/begrepssamlinger", null, JwtToken(Access.ROOT).toString(), HttpMethod.GET)

        assertEquals(HttpStatus.OK, response.statusCode)

        val result: List<Begrepssamling> = mapper.readValue(response.body as String)

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
        mongoOperations.insertAll(
            listOf(
                BEGREP_0.toDBO(),
                BEGREP_1.toDBO(),
                BEGREP_2.toDBO(),
                BEGREP_WRONG_ORG.toDBO(),
                BEGREP_TO_BE_DELETED.toDBO(),
                BEGREP_TO_BE_UPDATED.toDBO(),
                BEGREP_4.toDBO(),
                BEGREP_5.toDBO(),
                BEGREP_0_OLD.toDBO(),
                BEGREP_6.toDBO(),
                BEGREP_HAS_REVISION.toDBO(),
                BEGREP_UNPUBLISHED_REVISION.toDBO(),
                BEGREP_HAS_MULTIPLE_REVISIONS.toDBO(),
                BEGREP_UNPUBLISHED_REVISION_MULTIPLE_FIRST.toDBO(),
                BEGREP_UNPUBLISHED_REVISION_MULTIPLE_SECOND.toDBO()
            )
        )

        val response =
            authorizedRequest("/begrepssamlinger", null, JwtToken(Access.ORG_WRITE).toString(), HttpMethod.GET)

        assertEquals(HttpStatus.OK, response.statusCode)

        val result: List<Begrepssamling> = mapper.readValue(response.body as String)

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
        mongoOperations.insertAll(
            listOf(
                BEGREP_0.toDBO(),
                BEGREP_1.toDBO(),
                BEGREP_2.toDBO(),
                BEGREP_WRONG_ORG.toDBO(),
                BEGREP_TO_BE_DELETED.toDBO(),
                BEGREP_TO_BE_UPDATED.toDBO(),
                BEGREP_4.toDBO(),
                BEGREP_5.toDBO(),
                BEGREP_0_OLD.toDBO(),
                BEGREP_6.toDBO(),
                BEGREP_HAS_REVISION.toDBO(),
                BEGREP_UNPUBLISHED_REVISION.toDBO(),
                BEGREP_HAS_MULTIPLE_REVISIONS.toDBO(),
                BEGREP_UNPUBLISHED_REVISION_MULTIPLE_FIRST.toDBO(),
                BEGREP_UNPUBLISHED_REVISION_MULTIPLE_SECOND.toDBO()
            )
        )

        val response =
            authorizedRequest("/begrepssamlinger", null, JwtToken(Access.ORG_READ).toString(), HttpMethod.GET)

        assertEquals(HttpStatus.OK, response.statusCode)

        val result: List<Begrepssamling> = mapper.readValue(response.body as String)

        assertEquals(
            listOf(
                Begrepssamling("111111111", 3),
                Begrepssamling("111222333", 2),
                Begrepssamling("123456789", 3)
            ), result.sortedBy { it.id })
    }
}
