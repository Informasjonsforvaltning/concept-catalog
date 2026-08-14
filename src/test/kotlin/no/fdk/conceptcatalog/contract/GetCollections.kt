package no.fdk.conceptcatalog.contract

import com.fasterxml.jackson.module.kotlin.readValue
import no.fdk.conceptcatalog.ContractTestsBase
import no.fdk.conceptcatalog.model.Begrepssamling
import no.fdk.conceptcatalog.model.toEntity
import no.fdk.conceptcatalog.utils.Access
import no.fdk.conceptcatalog.utils.BEGREP_0
import no.fdk.conceptcatalog.utils.BEGREP_0_OLD
import no.fdk.conceptcatalog.utils.BEGREP_1
import no.fdk.conceptcatalog.utils.BEGREP_2
import no.fdk.conceptcatalog.utils.BEGREP_4
import no.fdk.conceptcatalog.utils.BEGREP_5
import no.fdk.conceptcatalog.utils.BEGREP_6
import no.fdk.conceptcatalog.utils.BEGREP_HAS_MULTIPLE_REVISIONS
import no.fdk.conceptcatalog.utils.BEGREP_HAS_REVISION
import no.fdk.conceptcatalog.utils.BEGREP_TO_BE_DELETED
import no.fdk.conceptcatalog.utils.BEGREP_TO_BE_UPDATED
import no.fdk.conceptcatalog.utils.BEGREP_UNPUBLISHED_REVISION
import no.fdk.conceptcatalog.utils.BEGREP_UNPUBLISHED_REVISION_MULTIPLE_FIRST
import no.fdk.conceptcatalog.utils.BEGREP_UNPUBLISHED_REVISION_MULTIPLE_SECOND
import no.fdk.conceptcatalog.utils.BEGREP_WRONG_ORG
import no.fdk.conceptcatalog.utils.JwtToken
import no.fdk.conceptcatalog.utils.toDBO
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
        conceptRepository.saveAll(
            listOf(
                BEGREP_0.toDBO().toEntity(),
                BEGREP_1.toDBO().toEntity(),
                BEGREP_2.toDBO().toEntity(),
                BEGREP_WRONG_ORG.toDBO().toEntity(),
                BEGREP_TO_BE_DELETED.toDBO().toEntity(),
                BEGREP_TO_BE_UPDATED.toDBO().toEntity(),
                BEGREP_4.toDBO().toEntity(),
                BEGREP_5.toDBO().toEntity(),
                BEGREP_0_OLD.toDBO().toEntity(),
                BEGREP_6.toDBO().toEntity(),
                BEGREP_HAS_REVISION.toDBO().toEntity(),
                BEGREP_UNPUBLISHED_REVISION.toDBO().toEntity(),
                BEGREP_HAS_MULTIPLE_REVISIONS.toDBO().toEntity(),
                BEGREP_UNPUBLISHED_REVISION_MULTIPLE_FIRST.toDBO().toEntity(),
                BEGREP_UNPUBLISHED_REVISION_MULTIPLE_SECOND.toDBO().toEntity(),
            ),
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
                Begrepssamling("999888777", 1),
            ),
            result.sortedBy { it.id },
        )
    }

    @Test
    fun `Only permitted collections for write access`() {
        conceptRepository.saveAll(
            listOf(
                BEGREP_0.toDBO().toEntity(),
                BEGREP_1.toDBO().toEntity(),
                BEGREP_2.toDBO().toEntity(),
                BEGREP_WRONG_ORG.toDBO().toEntity(),
                BEGREP_TO_BE_DELETED.toDBO().toEntity(),
                BEGREP_TO_BE_UPDATED.toDBO().toEntity(),
                BEGREP_4.toDBO().toEntity(),
                BEGREP_5.toDBO().toEntity(),
                BEGREP_0_OLD.toDBO().toEntity(),
                BEGREP_6.toDBO().toEntity(),
                BEGREP_HAS_REVISION.toDBO().toEntity(),
                BEGREP_UNPUBLISHED_REVISION.toDBO().toEntity(),
                BEGREP_HAS_MULTIPLE_REVISIONS.toDBO().toEntity(),
                BEGREP_UNPUBLISHED_REVISION_MULTIPLE_FIRST.toDBO().toEntity(),
                BEGREP_UNPUBLISHED_REVISION_MULTIPLE_SECOND.toDBO().toEntity(),
            ),
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
                Begrepssamling("222222222", 1),
            ),
            result.sortedBy { it.id },
        )
    }

    @Test
    fun `Only permitted collections for read access`() {
        conceptRepository.saveAll(
            listOf(
                BEGREP_0.toDBO().toEntity(),
                BEGREP_1.toDBO().toEntity(),
                BEGREP_2.toDBO().toEntity(),
                BEGREP_WRONG_ORG.toDBO().toEntity(),
                BEGREP_TO_BE_DELETED.toDBO().toEntity(),
                BEGREP_TO_BE_UPDATED.toDBO().toEntity(),
                BEGREP_4.toDBO().toEntity(),
                BEGREP_5.toDBO().toEntity(),
                BEGREP_0_OLD.toDBO().toEntity(),
                BEGREP_6.toDBO().toEntity(),
                BEGREP_HAS_REVISION.toDBO().toEntity(),
                BEGREP_UNPUBLISHED_REVISION.toDBO().toEntity(),
                BEGREP_HAS_MULTIPLE_REVISIONS.toDBO().toEntity(),
                BEGREP_UNPUBLISHED_REVISION_MULTIPLE_FIRST.toDBO().toEntity(),
                BEGREP_UNPUBLISHED_REVISION_MULTIPLE_SECOND.toDBO().toEntity(),
            ),
        )

        val response =
            authorizedRequest("/begrepssamlinger", null, JwtToken(Access.ORG_READ).toString(), HttpMethod.GET)

        assertEquals(HttpStatus.OK, response.statusCode)

        val result: List<Begrepssamling> = mapper.readValue(response.body as String)

        assertEquals(
            listOf(
                Begrepssamling("111111111", 3),
                Begrepssamling("111222333", 2),
                Begrepssamling("123456789", 3),
            ),
            result.sortedBy { it.id },
        )
    }
}
