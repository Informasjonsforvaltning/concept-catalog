package no.fdk.concept_catalog.rdf

import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.ResourceFactory

class AUDIENCE_TYPE {
    companion object {
        const val uri = "https://data.norge.no/vocabulary/audience-type#"

        val public: Property = ResourceFactory.createProperty( "${uri}public")
        val specialist: Property = ResourceFactory.createProperty( "${uri}specialist")
    }
}
