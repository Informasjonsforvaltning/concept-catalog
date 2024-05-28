package no.fdk.concept_catalog.rdf

import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.ResourceFactory

class EUVOC {
    companion object {
        const val uri = "http://publications.europa.eu/ontology/euvoc#"

        val status: Property = ResourceFactory.createProperty( "${uri}status")
        val xlDefinition: Property = ResourceFactory.createProperty("${uri}xlDefinition")
        val XlNote: Resource = ResourceFactory.createResource("${uri}XlNote")
        val startDate: Property = ResourceFactory.createProperty("${uri}startDate")
        val endDate: Property = ResourceFactory.createProperty("${uri}endDate")
    }
}
