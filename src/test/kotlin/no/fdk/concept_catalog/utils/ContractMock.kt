package no.fdk.concept_catalog.utils

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import no.fdk.concept_catalog.utils.jwk.JwkStore

private val mockserver = WireMockServer(LOCAL_SERVER_PORT)

fun startMockServer() {
    if(!mockserver.isRunning) {
        mockserver.stubFor(get(urlEqualTo("/ping"))
                .willReturn(aResponse()
                        .withStatus(200))
        )
        mockserver.stubFor(get(urlEqualTo("/auth/realms/fdk/protocol/openid-connect/certs"))
            .willReturn(okJson(JwkStore.get())))
        mockserver.stubFor(post(urlEqualTo("/111111111/id-to-be-updated/updates"))
            .willReturn(aResponse().withStatus(200)))
        mockserver.stubFor(post(urlEqualTo("/111111111/id-to-be-deleted/updates"))
            .willReturn(aResponse().withStatus(500)))
        mockserver.stubFor(post(urlMatching("/123456789/.*/updates"))
            .willReturn(aResponse().withStatus(200)))
        mockserver.stubFor(post(urlMatching("/111222333/.*/updates"))
            .willReturn(aResponse().withStatus(200)))
        mockserver.stubFor(post(urlMatching("/444555666/.*/updates"))
            .willReturn(aResponse().withStatus(200)))
        mockserver.start()
    }
}

fun stopMockServer() {
    if (mockserver.isRunning) mockserver.stop()
}
