package no.fdk.concept_catalog.utils

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import no.fdk.concept_catalog.model.*
import no.fdk.concept_catalog.rdf.rdfResponse
import no.fdk.concept_catalog.utils.ApiTestContext.Companion.mongoContainer
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.pojo.PojoCodecProvider
import org.slf4j.Logger
import org.springframework.http.*
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.Reader
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.time.ZoneId
import java.time.ZonedDateTime

fun apiGet(port: Int, endpoint: String, acceptHeader: MediaType): Map<String, Any> {

    return try {
        val connection = URL("http://localhost:$port$endpoint").openConnection() as HttpURLConnection
        connection.setRequestProperty("Accept", acceptHeader.toString())
        connection.connect()

        if (isOK(connection.responseCode)) {
            val responseBody = connection.inputStream.bufferedReader().use(BufferedReader::readText)
            mapOf(
                "body" to responseBody,
                "header" to connection.headerFields.toString(),
                "status" to connection.responseCode
            )
        } else {
            mapOf(
                "status" to connection.responseCode,
                "header" to " ",
                "body" to " "
            )
        }
    } catch (e: Exception) {
        mapOf(
            "status" to e.toString(),
            "header" to " ",
            "body" to " "
        )
    }
}

private fun isOK(response: Int?): Boolean =
    if (response == null) false
    else HttpStatus.resolve(response)?.is2xxSuccessful == true

fun authorizedRequest(
    path: String,
    port: Int,
    body: String? = null,
    token: String? = null,
    httpMethod: HttpMethod,
    accept: MediaType = MediaType.APPLICATION_JSON
): Map<String, Any> {
    val request = RestTemplate()
    request.requestFactory = HttpComponentsClientHttpRequestFactory()
    val url = "http://localhost:$port$path"
    val headers = HttpHeaders()
    headers.accept = listOf(accept)
    token?.let { headers.setBearerAuth(it) }
    headers.contentType = MediaType.APPLICATION_JSON
    val entity: HttpEntity<String> = HttpEntity(body, headers)

    return try {
        val response = request.exchange(url, httpMethod, entity, String::class.java)
        mapOf(
            "body" to response.body,
            "header" to response.headers,
            "status" to response.statusCode.value()
        )

    } catch (e: HttpClientErrorException) {
        mapOf(
            "status" to e.statusCode.value(),
            "header" to " ",
            "body" to e.toString()
        )
    } catch (e: HttpServerErrorException) {
        mapOf(
            "status" to e.statusCode.value(),
            "header" to " ",
            "body" to e.toString()
        )
    } catch (e: Exception) {
        mapOf(
            "status" to e.toString(),
            "header" to " ",
            "body" to " "
        )
    }

}

fun Begrep.toDBO(): BegrepDBO =
    BegrepDBO(
        id = id!!,
        originaltBegrep = originaltBegrep!!,
        versjonsnr = versjonsnr!!,
        revisjonAv,
        status,
        statusURI,
        erPublisert,
        publiseringsTidspunkt,
        anbefaltTerm,
        tillattTerm,
        frarådetTerm,
        definisjon,
        definisjonForAllmennheten,
        definisjonForSpesialister,
        merknad,
        merkelapp,
        ansvarligVirksomhet,
        eksempel,
        fagområde,
        fagområdeKoder,
        omfang,
        kontaktpunkt,
        gyldigFom,
        gyldigTom,
        endringslogelement,
        opprettet,
        opprettetAv,
        seOgså,
        erstattesAv,
        assignedUser,
        abbreviatedLabel,
        begrepsRelasjon,
        interneFelt
    )

class TestResponseReader {
    private fun resourceAsReader(resourceName: String): Reader {
        return InputStreamReader(javaClass.classLoader.getResourceAsStream(resourceName)!!, StandardCharsets.UTF_8)
    }

    fun parseTurtleFile(filename: String): Model {
        val expected = ModelFactory.createDefaultModel()
        expected.read(resourceAsReader(filename), "", "TURTLE")
        return expected
    }
}

fun resetDB() {
    val connectionString = ConnectionString("mongodb://${MONGO_USER}:${MONGO_PASSWORD}@localhost:${mongoContainer.getMappedPort(MONGO_PORT)}/$MONGO_DB_NAME?authSource=admin&authMechanism=SCRAM-SHA-1")
    val pojoCodecRegistry = CodecRegistries.fromRegistries(
        MongoClientSettings.getDefaultCodecRegistry(), CodecRegistries.fromProviders(
            PojoCodecProvider.builder().automatic(true).build()))

    val client: MongoClient = MongoClients.create(connectionString)
    val mongoDatabase = client.getDatabase(MONGO_DB_NAME).withCodecRegistry(pojoCodecRegistry)

    val catalogCollection = mongoDatabase.getCollection("begrep")
    catalogCollection.deleteMany(org.bson.Document())
    catalogCollection.insertMany(conceptDbPopulation())

    val changeRequestCollection = mongoDatabase.getCollection("changeRequest")
    changeRequestCollection.deleteMany(org.bson.Document())
    changeRequestCollection.insertMany(changeRequestPopulation())

    client.close()
}

fun conceptDbPopulation() = listOf(BEGREP_0, BEGREP_1, BEGREP_2, BEGREP_WRONG_ORG, BEGREP_TO_BE_DELETED,
    BEGREP_TO_BE_UPDATED, BEGREP_4, BEGREP_5, BEGREP_0_OLD, BEGREP_6, BEGREP_HAS_REVISION, BEGREP_UNPUBLISHED_REVISION,
    BEGREP_HAS_MULTIPLE_REVISIONS, BEGREP_UNPUBLISHED_REVISION_MULTIPLE_FIRST, BEGREP_UNPUBLISHED_REVISION_MULTIPLE_SECOND)
    .map { it.mapDBO() }

fun changeRequestPopulation() = listOf(CHANGE_REQUEST_0, CHANGE_REQUEST_1, CHANGE_REQUEST_2, CHANGE_REQUEST_3,
    CHANGE_REQUEST_4, CHANGE_REQUEST_5, CHANGE_REQUEST_6)
    .map { it.mapDBO() }

private fun Begrep.mapDBO(): org.bson.Document =
    org.bson.Document()
        .append("_id", id)
        .append("originaltBegrep", originaltBegrep)
        .append("versjonsnr", versjonsnr?.mapDBO())
        .append("status", status?.toString())
        .append("statusURI", statusURI)
        .append("erPublisert", erPublisert)
        .append("publiseringsTidspunkt", publiseringsTidspunkt)
        .append("anbefaltTerm", anbefaltTerm?.mapDBO())
        .append("tillattTerm", tillattTerm)
        .append("frarådetTerm", frarådetTerm)
        .append("definisjon", definisjon?.mapDBO())
        .append("definisjonForAllmennheten", definisjonForAllmennheten?.mapDBO())
        .append("definisjonForSpesialister", definisjonForSpesialister?.mapDBO())
        .append("merknad", merknad)
        .append("merkelapp", merkelapp)
        .append("eksempel", eksempel)
        .append("ansvarligVirksomhet", ansvarligVirksomhet.mapDBO())
        .append("seOgså", seOgså)
        .append("erstattesAv", erstattesAv)
        .append("fagområde", fagområde)
        .append("fagområdeKoder", fagområdeKoder)
        .append("assignedUser", assignedUser)
        .append("begrepsRelasjon", begrepsRelasjon?.map { it.mapDBO() })
        .append("kontaktpunkt", kontaktpunkt?.mapDBO())
        .append("gyldigFom", gyldigFom)
        .append("gyldigTom", gyldigTom)
        .append("endringslogelement", endringslogelement?.mapDBO())
        .append("omfang", omfang?.mapDBO())
        .append("interneFelt", interneFelt)

private fun Term.mapDBO(): org.bson.Document =
    org.bson.Document()
        .append("navn", navn)

private fun Endringslogelement.mapDBO(): org.bson.Document =
    org.bson.Document()
        .append("endretAv", endretAv)
        .append("endringstidspunkt", endringstidspunkt)

private fun Definisjon.mapDBO(): org.bson.Document =
    org.bson.Document()
        .append("tekst", tekst)
        .append("kildebeskrivelse", kildebeskrivelse?.mapDBO())

private fun Kildebeskrivelse.mapDBO(): org.bson.Document =
    org.bson.Document()
        .append("forholdTilKilde", forholdTilKilde.toString())
        .append("kilde", kilde?.map { it.mapDBO() })

private fun Virksomhet.mapDBO(): org.bson.Document =
    org.bson.Document()
        .append("_id", id)

private fun URITekst.mapDBO(): org.bson.Document =
    org.bson.Document()
        .append("uri", uri)
        .append("tekst", tekst)

private fun SemVer.mapDBO(): org.bson.Document =
    org.bson.Document()
        .append("major", major)
        .append("minor", minor)
        .append("patch", patch)

private fun BegrepsRelasjon.mapDBO(): org.bson.Document =
    org.bson.Document()
        .append("relasjon", relasjon)
        .append("relasjonsType", relasjonsType)
        .append("beskrivelse", beskrivelse)
        .append("inndelingskriterium", inndelingskriterium)
        .append("relatertBegrep", relatertBegrep)

private fun Kontaktpunkt.mapDBO(): org.bson.Document =
    org.bson.Document()
        .append("harTelefon", harTelefon)
        .append("harEpost", harEpost)

private fun ChangeRequest.mapDBO(): org.bson.Document =
    org.bson.Document()
        .append("_id", id)
        .append("conceptId", conceptId)
        .append("catalogId", catalogId)
        .append("status", status)
        .append("operations", operations.map{it.mapDBO()})
        .append("timeForProposal", timeForProposal)
        .append("proposedBy", proposedBy)
        .append("title", title)

private fun JsonPatchOperation.mapDBO(): org.bson.Document =
    org.bson.Document()
        .append("op", op)
        .append("path", path)
        .append("value", value)
        .append("from", from)

fun checkIfIsomorphicAndPrintDiff(actual: Model, expected: Model, name: String, logger: Logger): Boolean {
    // Its necessary to parse the created models from strings to have the same base, and ensure blank node validity
    val parsedActual = ModelFactory.createDefaultModel().read(StringReader(actual.rdfResponse(Lang.TURTLE)), null, "TURTLE")
    val parsedExpected = ModelFactory.createDefaultModel().read(StringReader(expected.rdfResponse(Lang.TURTLE)), null, "TURTLE")

    val isIsomorphic = parsedActual.isIsomorphicWith(parsedExpected)

    if (!isIsomorphic) {
        val actualDiff = parsedActual.difference(parsedExpected).rdfResponse(Lang.TURTLE)
        val expectedDiff = parsedExpected.difference(parsedActual).rdfResponse(Lang.TURTLE)

        if (actualDiff.isNotEmpty()) {
            logger.error("non expected nodes in $name:")
            logger.error(actualDiff)
        }
        if (expectedDiff.isNotEmpty()) {
            logger.error("missing nodes in $name:")
            logger.error(expectedDiff)
        }
    }
    return isIsomorphic
}