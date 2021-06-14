package no.fdk.concept_catalog.rdf

import org.apache.jena.rdf.model.Model
import org.apache.jena.riot.Lang
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpServerErrorException
import java.io.StringWriter

fun Model.rdfResponse(lang: Lang): String =
    StringWriter().use{ out ->
        write(out, lang.name)
        out.toString()
    }

fun jenaLangFromAcceptHeader(accept: String?): Lang =
    when {
        accept == null -> throw HttpServerErrorException(HttpStatus.NOT_ACCEPTABLE)
        accept.contains("text/turtle") -> Lang.TURTLE
        accept.contains("application/rdf+xml") -> Lang.RDFXML
        accept.contains("application/rdf+json") -> Lang.RDFJSON
        accept.contains("application/ld+json") -> Lang.JSONLD
        accept.contains("application/n-triples") -> Lang.NTRIPLES
        accept.contains("text/n3") -> Lang.N3
        else -> throw HttpServerErrorException(HttpStatus.NOT_ACCEPTABLE)
    }
