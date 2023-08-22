package no.fdk.concept_catalog.contract

import com.fasterxml.jackson.module.kotlin.readValue
import no.fdk.concept_catalog.configuration.JacksonConfigurer
import no.fdk.concept_catalog.model.Begrep
import no.fdk.concept_catalog.model.SemVer
import no.fdk.concept_catalog.model.Status
import no.fdk.concept_catalog.utils.*
import no.fdk.concept_catalog.utils.jwk.Access
import no.fdk.concept_catalog.utils.jwk.JwtToken
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.ContextConfiguration
import kotlin.test.assertEquals

private val mapper = JacksonConfigurer().objectMapper()

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
    properties = ["spring.profiles.active=contract-test"],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ContextConfiguration(initializers = [ApiTestContext.Initializer::class])
@Tag("contract")
class CreateRevision : ApiTestContext() {

    @Test
    fun `Unauthorized when access token is not included`() {
        val rsp = authorizedRequest(
            "/begreper/${BEGREP_4.id}/revisjon", port,
            mapper.writeValueAsString(BEGREP_REVISION), null, HttpMethod.POST
        )

        assertEquals(HttpStatus.UNAUTHORIZED.value(), rsp["status"])
    }

    @Test
    fun `Forbidden for read access`() {
        val rsp = authorizedRequest(
            "/begreper/${BEGREP_4.id}/revisjon", port, mapper.writeValueAsString(BEGREP_REVISION),
            JwtToken(Access.ORG_READ).toString(), HttpMethod.POST
        )

        assertEquals(HttpStatus.FORBIDDEN.value(), rsp["status"])
    }

    @Test
    fun `Bad request when attempting to create revision of unpublished concept`() {
        val rsp = authorizedRequest(
            "/begreper/${BEGREP_2.id}/revisjon", port, mapper.writeValueAsString(BEGREP_REVISION),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.POST
        )
        assertEquals(HttpStatus.BAD_REQUEST.value(), rsp["status"])
    }

    @Test
    fun `Bad request when attempting to create revision of concept with existing unpublished revision`() {
        val rsp = authorizedRequest(
            "/begreper/${BEGREP_HAS_REVISION.id}/revisjon", port, mapper.writeValueAsString(BEGREP_UNPUBLISHED_REVISION),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.POST
        )
        assertEquals(HttpStatus.BAD_REQUEST.value(), rsp["status"])
    }

    @Test
    fun `Ok - Created - for write access`() {
        val before = authorizedRequest(
            "/begreper?orgNummer=${BEGREP_4.ansvarligVirksomhet?.id}",
            port, null, JwtToken(Access.ORG_WRITE).toString(), HttpMethod.GET
        )

        val rsp = authorizedRequest(
            "/begreper/${BEGREP_4.id}/revisjon", port, mapper.writeValueAsString(BEGREP_REVISION),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.POST
        )
        assertEquals(HttpStatus.CREATED.value(), rsp["status"])

        val after = authorizedRequest(
            "/begreper?orgNummer=${BEGREP_4.ansvarligVirksomhet?.id}",
            port, null, JwtToken(Access.ORG_WRITE).toString(), HttpMethod.GET
        )

        val beforeList: List<Begrep> = mapper.readValue<List<Begrep>>(before["body"] as String).filter{ it -> it.id != "id5" }
        val afterList: List<Begrep> = mapper.readValue<List<Begrep>>(after["body"] as String).filter{ it -> it.id != "id5" }
        assertEquals(beforeList.size + 1, afterList.size)

        val revision: Begrep? = afterList.firstOrNull { it.id != "id4" }
        val concept4After: Begrep? = afterList.firstOrNull { it.id == "id4" }

        assertEquals(BEGREP_4.originaltBegrep, revision?.originaltBegrep)
        assertEquals(SemVer(1, 0, 1), revision?.versjonsnr)
        assertEquals(Status.UTKAST, revision?.status)
        assertEquals(false, revision?.erPublisert)
        assertEquals(BEGREP_REVISION.anbefaltTerm, revision?.anbefaltTerm)
        assertEquals(BEGREP_4.ansvarligVirksomhet, revision?.ansvarligVirksomhet)
        assertEquals(revision?.id, concept4After?.gjeldendeRevisjon)
    }
}
