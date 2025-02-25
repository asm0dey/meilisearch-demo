package com.github.asm0dey.meilisearchdemo.meilisearchdemo

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.meilisearch.sdk.Client
import com.meilisearch.sdk.SearchRequest
import de.siegmar.fastcsv.reader.CsvReader
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.nio.file.Paths
import kotlin.time.measureTime

@RestController
class Controller(val meiliseachClient: Client, val objectMapper: ObjectMapper) {
    @PostMapping("/load")
    fun loadCSV(): String {
        val booksCsv = Paths.get("books.csv")
        val index = meiliseachClient.index("books")
        val books = CsvReader.builder()
            .ignoreDifferentFieldCount(true)
            .fieldSeparator(';')
            .quoteCharacter('"')
            .ofNamedCsvRecord(booksCsv).use {
                it.mapNotNull { record ->
                    try {
                        Book(
                            record.getField(ISBN),
                            record.getField(BOOK_TITLE),
                            record.getField(YEAR_OF_PUBLICATION).toInt(),
                            record.getField(PUBLISHER),
                            record.getField(IMAGE_URL_S),
                            record.getField(IMAGE_URL_M),
                            record.getField(IMAGE_URL_L)
                        )

                    } catch (e: Exception) {
                        null
                    }
                }
            }
        val inWholeMilliseconds = measureTime {
            index.addDocumentsInBatches(objectMapper.writeValueAsString(books), 10000, ISBN)
        }.inWholeMilliseconds
        return "Saved ${books.size} book in $inWholeMilliseconds ms"
    }

    @GetMapping("/search")
    fun findBooks(@RequestParam("q") query: String): List<Book> =
        meiliseachClient.index("books").search(SearchRequest(query).setHitsPerPage(50))
            .hits
            .map { objectMapper.writeValueAsString(it) }
            .map { objectMapper.readValue<Book>(it) }
}

private const val ISBN = "ISBN"
private const val BOOK_TITLE = "Book-Title"
private const val YEAR_OF_PUBLICATION = "Year-Of-Publication"
private const val PUBLISHER = "Publisher"
private const val IMAGE_URL_S = "Image-URL-S"
private const val IMAGE_URL_M = "Image-URL-M"
private const val IMAGE_URL_L = "Image-URL-L"

data class Book(
    @JsonProperty(ISBN) val isbn: String,
    @JsonProperty(BOOK_TITLE) val bookTitle: String,
    @JsonProperty(YEAR_OF_PUBLICATION) val yearOfPublication: Int,
    @JsonProperty(PUBLISHER) val publisher: String,
    @JsonProperty(IMAGE_URL_S) val imageUrlS: String,
    @JsonProperty(IMAGE_URL_M) val imageUrlM: String,
    @JsonProperty(IMAGE_URL_L) val imageUrlL: String
)