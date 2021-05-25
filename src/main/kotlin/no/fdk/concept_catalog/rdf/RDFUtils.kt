package no.fdk.concept_catalog.rdf

import org.apache.jena.rdf.model.Model
import org.apache.jena.riot.Lang
import java.io.StringWriter

fun Model.turtleResponse(): String =
    StringWriter().use{ out ->
        write(out, Lang.TURTLE.name)
        out.toString()
    }
