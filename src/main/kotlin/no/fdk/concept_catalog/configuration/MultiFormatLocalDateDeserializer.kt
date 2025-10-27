import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class MultiFormatLocalDateDeserializer : JsonDeserializer<LocalDate>() {
    private val formatters = listOf(
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("dd.MM.yyyy")
    )

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): LocalDate? {
        val value = p.text
        for (formatter in formatters) {
            try {
                return LocalDate.parse(value, formatter)
            } catch (e: DateTimeParseException) {
                // Try next format
            }
        }
        throw DateTimeParseException("Unparseable date: $value", value, 0)
    }
}