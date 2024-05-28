package no.fdk.concept_catalog.rdf

import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.ResourceFactory

class SKOSNO {
    companion object {
        const val uri = "https://data.norge.no/vocabulary/skosno#"

        val Definisjon: Resource = ResourceFactory.createResource("${uri}Definisjon")
        val AlternativFormulering: Resource = ResourceFactory.createResource("${uri}AlternativFormulering")
        val AssociativeConceptRelation: Resource = ResourceFactory.createResource("${uri}AssociativeConceptRelation")
        val GenericConceptRelation: Resource = ResourceFactory.createResource("${uri}GenericConceptRelation")
        val PartitiveConceptRelation: Resource = ResourceFactory.createResource("${uri}PartitiveConceptRelation")

        val definisjon: Property = ResourceFactory.createProperty( "${uri}definisjon")
        val relationRole: Property = ResourceFactory.createProperty("${uri}relationRole")
        val alternativFormulering: Property = ResourceFactory.createProperty( "${uri}alternativFormulering")
        val bruksområde: Property = ResourceFactory.createProperty( "${uri}bruksområde")
        val relationshipWithSource: Property = ResourceFactory.createProperty( "${uri}relationshipWithSource")
        val valueRange: Property = ResourceFactory.createProperty( "${uri}valueRange")
        val datastrukturterm: Property = ResourceFactory.createProperty( "${uri}datastrukturterm")
        val isFromConceptIn: Property = ResourceFactory.createProperty( "${uri}isFromConceptIn")
        val hasPartitiveConceptRelation: Property = ResourceFactory.createProperty( "${uri}hasPartitiveConceptRelation")
        val hasGenericConceptRelation: Property = ResourceFactory.createProperty( "${uri}hasGenericConceptRelation")
        val inndelingskriterium: Property = ResourceFactory.createProperty( "${uri}inndelingskriterium")
        val hasPartitiveConcept: Property = ResourceFactory.createProperty("${uri}hasPartitiveConcept")
        val hasComprehensiveConcept: Property = ResourceFactory.createProperty("${uri}hasComprehensiveConcept")
        val hasToConcept: Property = ResourceFactory.createProperty("${uri}hasToConcept")
        val hasSpecificConcept: Property = ResourceFactory.createProperty("${uri}hasSpecificConcept")
        val hasGenericConcept: Property = ResourceFactory.createProperty("${uri}hasGenericConcept")


        val allmennheten: Property = ResourceFactory.createProperty( "${uri}allmennheten")
        val fagspesialist: Property = ResourceFactory.createProperty( "${uri}fagspesialist")
    }
}
