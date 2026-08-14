package no.fdk.conceptcatalog.rdf

import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.ResourceFactory

// Named after the RDF vocabulary it models, matching the other vocabulary
// classes in this package and Apache Jena's own (SKOS, DCAT, RDF).
@Suppress("ktlint:standard:class-naming")
class CONCEPT_STATUS {
    companion object {
        const val URI = "http://publications.europa.eu/resource/authority/concept-status/"

        val draft: Property = ResourceFactory.createProperty("${URI}DRAFT")
    }
}
