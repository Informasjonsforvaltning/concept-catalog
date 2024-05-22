package no.fdk.concept_catalog.rdf

import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.ResourceFactory
class RELATIONSHIP {
    companion object {
        const val uri = "https://data.norge.no/vocabulary/relationship-with-source-type#"

        val directFromSource: Property = ResourceFactory.createProperty( "${uri}direct-from-source")
        val derivedFromSource: Property = ResourceFactory.createProperty( "${uri}derived-from-source")
        val selfComposed: Property = ResourceFactory.createProperty( "${uri}self-composed")
    }
}


