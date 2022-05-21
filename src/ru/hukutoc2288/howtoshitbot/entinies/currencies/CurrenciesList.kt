package ru.hukutoc2288.howtoshitbot.entinies.currencies

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import ru.hukutoc2288.howtoshitbot.mapper
import java.util.*


data class CurrenciesList(
    @JsonProperty("Date")
    var date: Date? = null,

    @JsonProperty("PreviousDate")
    var previousDate: Date? = null,

    @JsonProperty("PreviousURL")
    var previousURL: String? = null,

    @JsonProperty("Timestamp")
    var timestamp: Date? = null,

    @JsonProperty("Valute")
    var valute: JsonNode? = null
) {
    fun findByCharCode(charCodeNoCase: String): Currency? {
        val charCode = charCodeNoCase.uppercase(Locale.getDefault())
        if (charCode == "RUB") {
            // easter egg that returns ruble to ruble exchange rates which exactly equal to 1 ruble
            return Currency("stub", "0", "RUB", 1, "Российский рубль", 1.0, 1.0)
        }
        if (charCode == "SRU") {
            // exchange rate for Programmer Rouble
            return Currency("stub", "0", "SRU", 1, "Прогерский срубль", 1.0, 1.0)
        }
        valute?.let {
            val currency = it.get(charCode) ?: return null
            return mapper.treeToValue(currency, Currency::class.java)
        }
        return null
    }
}

