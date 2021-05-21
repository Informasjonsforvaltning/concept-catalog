package no.fdk.concept_catalog.rdf

import org.apache.jena.rdf.model.Model
import java.io.ByteArrayOutputStream

fun Model.turtleResponse(): String =
    ByteArrayOutputStream().use{ out ->
        write(out, "TURTLE")
        out.flush()
        out.toString("UTF-8")
    }
