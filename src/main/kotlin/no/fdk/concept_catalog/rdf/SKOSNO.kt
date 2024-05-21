package no.fdk.concept_catalog.rdf

import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.ResourceFactory

class SKOSNO {
    companion object {
        const val uri = "https://data.norge.no/vocabulary/skosno#"

        val Definisjon: Resource = ResourceFactory.createResource("${uri}Definisjon")
        val AlternativFormulering: Resource = ResourceFactory.createResource("${uri}AlternativFormulering")
        val AssosiativRelasjon: Resource = ResourceFactory.createResource("${uri}AssosiativRelasjon")
        val GeneriskRelasjon: Resource = ResourceFactory.createResource("${uri}GeneriskRelasjon")
        val PartitivRelasjon: Resource = ResourceFactory.createResource("${uri}PartitivRelasjon")

        val definisjon: Property = ResourceFactory.createProperty( "${uri}definisjon")
        val relationRole: Property = ResourceFactory.createProperty("${uri}relationRole")
        val alternativFormulering: Property = ResourceFactory.createProperty( "${uri}alternativFormulering")
        val bruksområde: Property = ResourceFactory.createProperty( "${uri}bruksområde")
        val relationshipWithSource: Property = ResourceFactory.createProperty( "${uri}relationshipWithSource")
        val valueRange: Property = ResourceFactory.createProperty( "${uri}valueRange")
        val datastrukturterm: Property = ResourceFactory.createProperty( "${uri}datastrukturterm")
        val assosiativRelasjon: Property = ResourceFactory.createProperty( "${uri}assosiativRelasjon")
        val partitivRelasjon: Property = ResourceFactory.createProperty( "${uri}partitivRelasjon")
        val generiskRelasjon: Property = ResourceFactory.createProperty( "${uri}generiskRelasjon")
        val inndelingskriterium: Property = ResourceFactory.createProperty( "${uri}inndelingskriterium")

        val allmennheten: Property = ResourceFactory.createProperty( "${uri}allmennheten")
        val fagspesialist: Property = ResourceFactory.createProperty( "${uri}fagspesialist")
    }
}
