package no.fdk.concept_catalog.utils

import no.fdk.concept_catalog.model.Begrep
import no.fdk.concept_catalog.model.BegrepDBO
import no.fdk.concept_catalog.rdf.rdfResponse
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.slf4j.Logger
import java.io.InputStreamReader
import java.io.Reader
import java.io.StringReader
import java.nio.charset.StandardCharsets

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
        internSeOgså,
        erstattesAv,
        assignedUser,
        abbreviatedLabel,
        begrepsRelasjon,
        internBegrepsRelasjon,
        interneFelt,
        internErstattesAv
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

fun checkIfIsomorphicAndPrintDiff(actual: Model, expected: Model, name: String, logger: Logger): Boolean {
    // Its necessary to parse the created models from strings to have the same base, and ensure blank node validity
    val parsedActual =
        ModelFactory.createDefaultModel().read(StringReader(actual.rdfResponse(Lang.TURTLE)), null, "TURTLE")
    val parsedExpected =
        ModelFactory.createDefaultModel().read(StringReader(expected.rdfResponse(Lang.TURTLE)), null, "TURTLE")

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
