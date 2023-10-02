package no.fdk.concept_catalog.model

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import java.time.Instant

@JsonIgnoreProperties(ignoreUnknown = true)
data class Endringslogelement(
    val endretAv: String?,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "Europe/Oslo")
    @Field(type = FieldType.Date)
    val endringstidspunkt: Instant
)
