package ru.hukutoc2288.howtoshitbot.utils

fun Int.pluralize(single: String, some: String, many: String): String {
    val preLastDigit: Int = this % 100 / 10

    return "$this " +if (preLastDigit == 1) {
        many
    } else when (this % 10) {
        1 -> single
        2, 3, 4 -> some
        else -> many
    }

}