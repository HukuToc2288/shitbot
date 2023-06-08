package ru.hukutoc2288.howtoshitbot.entinies.currencies

import com.fasterxml.jackson.annotation.JsonProperty

data class Currency(
    @JsonProperty("ID")
    var id: String? = null,

    @JsonProperty("NumCode")
    var numCode: String? = null,

    @JsonProperty("CharCode")
    var charCode: String? = null,

    @JsonProperty("Nominal")
    var nominal: Int = 0,

    @JsonProperty("Name")
    var name: String? = null,

    @JsonProperty("Value")
    var value: Double = 0.0,

    @JsonProperty("Previous")
    var previous: Double = 0.0
) {
}