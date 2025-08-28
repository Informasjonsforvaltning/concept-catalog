import no.fdk.concept_catalog.model.Begrep
import no.fdk.concept_catalog.model.Status
import no.fdk.concept_catalog.model.Term
import no.fdk.concept_catalog.model.User
import no.fdk.concept_catalog.model.Virksomhet
import no.fdk.concept_catalog.utils.toDBO
import org.springframework.beans.factory.annotation.Autowired
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@DataMongoTest
open class ImportServiceRollbackTest {

    @Autowired
    lateinit var mongoOperations: MongoOperations

    @Test
    @Transactional
    open fun `transaction should work`() {
        val catalogId = "123456789"
        val conceptUri = UUID.randomUUID().toString()
        val virksomhetsUri = "http://example.com/begrep/123456789"
        val user = User(id = catalogId, name = "TEST USER", email = null)
        val begrepToSave = Begrep(
            id = conceptUri,
            status = Status.UTKAST,
            statusURI = "http://publications.europa.eu/resource/authority/concept-status/DRAFT",
            anbefaltTerm = Term(navn = mapOf("nb" to "Testnavn")),
            ansvarligVirksomhet = Virksomhet(
                uri = virksomhetsUri,
                id = catalogId
            ),
            interneFelt = null,
            internErstattesAv = null,
        )

        mongoOperations.save(begrepToSave.toDBO())
        throw RuntimeException("Force rollback")

        /*doThrow(RuntimeException("History service failed"))
            .whenever(importService)
            .saveAllConceptsDB(any())*/


        /*importService.saveAllConceptsDB(listOf(begrepToSave).map { it.toDBO() } )
        verify(conceptRepository, never()).saveAll(listOf(begrepToSave).map { it.toDBO() })*/

    }

    @Test
    fun `should have no concepts in database after rollback`() {
        val concepts = mongoOperations.findAll(Begrep::class.java)
        assert(concepts.isEmpty())
    }
}