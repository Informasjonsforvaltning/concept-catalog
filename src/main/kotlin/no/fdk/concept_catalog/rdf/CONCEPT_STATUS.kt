package no.fdk.concept_catalog.rdf

import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.ResourceFactory

class CONCEPT_STATUS {
    companion object {
        const val uri = "http://publications.europa.eu/resource/authority/concept-status/"

        val draft: Property = ResourceFactory.createProperty( "${uri}DRAFT")
    }
}
