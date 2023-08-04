package no.fdk.concept_catalog.rdf

import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.ResourceFactory

class XKOS {
    companion object {
        const val uri = "http://rdf-vocabulary.ddialliance.org/xkos#"

        val generalizes: Property = ResourceFactory.createProperty( "${uri}generalizes")
        val specializes: Property = ResourceFactory.createProperty( "${uri}specializes")
    }
}
