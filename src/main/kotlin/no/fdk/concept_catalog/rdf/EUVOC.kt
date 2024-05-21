package no.fdk.concept_catalog.rdf

import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.ResourceFactory

class EUVOC {
    companion object {
        const val uri = "http://publications.europa.eu/ontology/euvoc#"

        val status: Property = ResourceFactory.createProperty( "${uri}status")
        val xlDefinition: Property = ResourceFactory.createProperty("${uri}xlDefinition")
        val xlNote: Property = ResourceFactory.createProperty("${uri}XlNote")
    }
}
