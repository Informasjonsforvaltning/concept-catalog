package no.fdk.concept_catalog.rdf

import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.ResourceFactory

class SKOSNO {
    companion object {
        const val URI = "https://data.norge.no/vocabulary/skosno#"

        val AssociativeConceptRelation: Resource = ResourceFactory.createResource("${URI}AssociativeConceptRelation")
        val GenericConceptRelation: Resource = ResourceFactory.createResource("${URI}GenericConceptRelation")
        val PartitiveConceptRelation: Resource = ResourceFactory.createResource("${URI}PartitiveConceptRelation")

        val relationRole: Property = ResourceFactory.createProperty("${URI}relationRole")
        val relationshipWithSource: Property = ResourceFactory.createProperty("${URI}relationshipWithSource")
        val valueRange: Property = ResourceFactory.createProperty("${URI}valueRange")
        val isFromConceptIn: Property = ResourceFactory.createProperty("${URI}isFromConceptIn")
        val hasPartitiveConceptRelation: Property = ResourceFactory.createProperty("${URI}hasPartitiveConceptRelation")
        val hasGenericConceptRelation: Property = ResourceFactory.createProperty("${URI}hasGenericConceptRelation")
        val hasPartitiveConcept: Property = ResourceFactory.createProperty("${URI}hasPartitiveConcept")
        val hasComprehensiveConcept: Property = ResourceFactory.createProperty("${URI}hasComprehensiveConcept")
        val hasToConcept: Property = ResourceFactory.createProperty("${URI}hasToConcept")
        val hasSpecificConcept: Property = ResourceFactory.createProperty("${URI}hasSpecificConcept")
        val hasGenericConcept: Property = ResourceFactory.createProperty("${URI}hasGenericConcept")
    }
}
