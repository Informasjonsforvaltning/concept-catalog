package no.fdk.concept_catalog.rdf

import org.apache.jena.rdf.model.Model
import org.apache.jena.riot.Lang
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpServerErrorException
import java.io.StringWriter

fun Model.rdfResponse(lang: Lang): String =
    StringWriter().use { out ->
        write(out, lang.name)
        out.toString()
    }

fun jenaLangFromHeader(header: String?): Lang =
    when {
        header == null -> throw HttpServerErrorException(HttpStatus.NOT_ACCEPTABLE)
        header.contains(Lang.TURTLE.headerString) -> Lang.TURTLE
        header.contains(Lang.RDFXML.headerString) -> Lang.RDFXML
        header.contains(Lang.RDFJSON.headerString) -> Lang.RDFJSON
        header.contains(Lang.JSONLD.headerString) -> Lang.JSONLD
        header.contains(Lang.NTRIPLES.headerString) -> Lang.NTRIPLES
        header.contains("text/n3") -> Lang.N3
        header.contains(Lang.NQUADS.headerString) -> Lang.NQUADS
        header.contains(Lang.TRIG.headerString) -> Lang.TRIG
        header.contains(Lang.TRIX.headerString) -> Lang.TRIX
        else -> throw HttpServerErrorException(HttpStatus.NOT_ACCEPTABLE)
    }
