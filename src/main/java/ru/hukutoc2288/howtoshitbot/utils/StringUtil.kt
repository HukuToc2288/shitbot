package ru.hukutoc2288.howtoshitbot.utils

object StringUtils {
    fun String.dedupe(): String {
        val buffer = StringBuilder()
        var prev = 0.toChar()
        for (curr in this.toCharArray()) {
            if (curr != prev) {
                buffer.append(curr)
                prev = curr
            }
        }
        return buffer.toString()
    }
}