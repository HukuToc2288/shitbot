package ru.hukutoc2288.howtoshitbot.utils

class CurrenciesAliases {
    companion object {
        val aliases = HashMap<Array<String>, String>()

        fun setup() {
            aliases.apply {
                put(arrayOf("рубль", "рубля"), "RUB")
                put(arrayOf("срубль","срубля"), "SRU")
                put(arrayOf("фунт", "фунта"), "GBP")
                put(arrayOf("доллар", "доллара"), "USD")
                put(arrayOf("евро"), "EUR")
                put(arrayOf("рупия", "рупии"), "INR")
                put(arrayOf("тенге"), "KZT")
                put(arrayOf("юань", "юаня"), "CNY")
                put(arrayOf("злотый", "злотого","злотувка", "злотувечка", "злотувэчка", "злоты", "злотэ", "злотувки", "злоте"), "PLN")
                put(arrayOf("гривна", "гривны", "гривни"), "UAH")
                put(arrayOf("франк", "франка"), "CHF")
                put(arrayOf("иена", "иены"), "JPY")
            }
        }

        fun deAlias(alias: String): String {
            val iterator = aliases.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                for (key in entry.key) {
                    if (key.equals(alias, ignoreCase = true))
                        return entry.value
                }
            }
            // return itself if unable to find something
            return alias
        }

        fun string(): String {
            var string = ""
            val iterator = aliases.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                string += "\n"
                for (key in entry.key) {
                    string += key
                    if (key != entry.key.last())
                        string += ", "
                }
                string += " -> ${entry.value}"
            }
            return string
        }
    }
}