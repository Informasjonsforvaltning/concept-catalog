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
        mockserver.start()
    }
}

fun stopMockServer() {
    if (mockserver.isRunning) mockserver.stop()
}
