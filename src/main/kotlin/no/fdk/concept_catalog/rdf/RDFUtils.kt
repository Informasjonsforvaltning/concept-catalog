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
        accept.contains(Lang.TURTLE.headerString) -> Lang.TURTLE
        accept.contains(Lang.RDFXML.headerString) -> Lang.RDFXML
        accept.contains(Lang.RDFJSON.headerString) -> Lang.RDFJSON
        accept.contains(Lang.JSONLD.headerString) -> Lang.JSONLD
        accept.contains(Lang.NTRIPLES.headerString) -> Lang.NTRIPLES
        accept.contains("text/n3") -> Lang.N3
        accept.contains(Lang.NQUADS.headerString) -> Lang.NQUADS
        accept.contains(Lang.TRIG.headerString) -> Lang.TRIG
        accept.contains(Lang.TRIX.headerString) -> Lang.TRIX
        else -> throw HttpServerErrorException(HttpStatus.NOT_ACCEPTABLE)
    }
