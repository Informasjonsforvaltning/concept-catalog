package no.fdk.concept_catalog.rdf

import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.ResourceFactory

// Named after the RDF vocabulary it models, matching the other vocabulary
// classes in this package and Apache Jena's own (SKOS, DCAT, RDF).
@Suppress("ktlint:standard:class-naming")
class AUDIENCE_TYPE {
    companion object {
        const val URI = "https://data.norge.no/vocabulary/audience-type#"

        val public: Property = ResourceFactory.createProperty("${URI}public")
        val specialist: Property = ResourceFactory.createProperty("${URI}specialist")
    }
}
