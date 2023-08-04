package no.fdk.concept_catalog.rdf

import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.ResourceFactory

class SCHEMA {
    companion object {
        const val uri = "http://schema.org/"

        val startDate: Property = ResourceFactory.createProperty( "${uri}startDate")
        val endDate: Property = ResourceFactory.createProperty( "${uri}endDate")
    }
}
