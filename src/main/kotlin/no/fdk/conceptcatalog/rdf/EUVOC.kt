package no.fdk.conceptcatalog.rdf

import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.ResourceFactory

class EUVOC {
    companion object {
        const val URI = "http://publications.europa.eu/ontology/euvoc#"

        val status: Property = ResourceFactory.createProperty("${URI}status")
        val xlDefinition: Property = ResourceFactory.createProperty("${URI}xlDefinition")
        val XlNote: Resource = ResourceFactory.createResource("${URI}XlNote")
        val startDate: Property = ResourceFactory.createProperty("${URI}startDate")
        val endDate: Property = ResourceFactory.createProperty("${URI}endDate")
    }
}
