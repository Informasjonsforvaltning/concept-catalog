package no.fdk.concept_catalog.rdf

import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.ResourceFactory
import org.apache.jena.vocabulary.SKOS

class ExtendedSKOS {
    companion object {
        const val uri = SKOS.uri

        val isFromConceptIn: Property = ResourceFactory.createProperty( "${uri}isFromConceptIn")
        val hasPartitiveConceptRelation: Property = ResourceFactory.createProperty( "${uri}hasPartitiveConceptRelation")
        val hasGenericConceptRelation: Property = ResourceFactory.createProperty( "${uri}hasGenericConceptRelation")
    }
}