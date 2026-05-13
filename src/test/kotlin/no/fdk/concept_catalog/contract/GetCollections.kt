package no.fdk.concept_catalog.contract

import com.fasterxml.jackson.module.kotlin.readValue
import no.fdk.concept_catalog.ContractTestsBase
import no.fdk.concept_catalog.model.Begrepssamling
import no.fdk.concept_catalog.model.toEntity
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
                BEGREP_UNPUBLISHED_REVISION_MULTIPLE_SECOND.toDBO().toEntity()
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
                BEGREP_UNPUBLISHED_REVISION_MULTIPLE_SECOND.toDBO().toEntity()
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
                BEGREP_UNPUBLISHED_REVISION_MULTIPLE_SECOND.toDBO().toEntity()
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
