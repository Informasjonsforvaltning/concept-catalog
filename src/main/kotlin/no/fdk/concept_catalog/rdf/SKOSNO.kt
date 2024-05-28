package no.fdk.concept_catalog.rdf

import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.ResourceFactory

class SKOSNO {
    companion object {
        const val uri = "https://data.norge.no/vocabulary/skosno#"

        val AssociativeConceptRelation: Resource = ResourceFactory.createResource("${uri}AssociativeConceptRelation")
        val GenericConceptRelation: Resource = ResourceFactory.createResource("${uri}GenericConceptRelation")
        val PartitiveConceptRelation: Resource = ResourceFactory.createResource("${uri}PartitiveConceptRelation")

        val relationRole: Property = ResourceFactory.createProperty("${uri}relationRole")
        val relationshipWithSource: Property = ResourceFactory.createProperty( "${uri}relationshipWithSource")
        val valueRange: Property = ResourceFactory.createProperty( "${uri}valueRange")
        val isFromConceptIn: Property = ResourceFactory.createProperty( "${uri}isFromConceptIn")
        val hasPartitiveConceptRelation: Property = ResourceFactory.createProperty( "${uri}hasPartitiveConceptRelation")
        val hasGenericConceptRelation: Property = ResourceFactory.createProperty( "${uri}hasGenericConceptRelation")
        val hasPartitiveConcept: Property = ResourceFactory.createProperty("${uri}hasPartitiveConcept")
        val hasComprehensiveConcept: Property = ResourceFactory.createProperty("${uri}hasComprehensiveConcept")
        val hasToConcept: Property = ResourceFactory.createProperty("${uri}hasToConcept")
        val hasSpecificConcept: Property = ResourceFactory.createProperty("${uri}hasSpecificConcept")
        val hasGenericConcept: Property = ResourceFactory.createProperty("${uri}hasGenericConcept")
    }
}
