package ru.hukutoc2288.howtoshitbot.utils

import java.lang.IllegalArgumentException

fun main() {
    println("final distance: "+DamerauLevenshtein.calculateDistance("сратьбот", "клавабот",7))
}

object DamerauLevenshtein {
    /**
     * @author https://github.com/crwohlfeil/damerau-levenshtein
     *
     * Calculates the string distance between source and target strings using
     * the Damerau-Levenshtein algorithm. The distance is case-sensitive.
     *
     * @param source The source String.
     * @param target The target String.
     * @param
     * @return The distance between source and target strings.
     * @throws IllegalArgumentException If either source or target is null.
     */
    fun calculateDistance(source: CharSequence, target: CharSequence, limit: Int = Int.MAX_VALUE): Int {
        val sourceLength = source.length
        val targetLength = target.length
        if (sourceLength == 0) return targetLength
        if (targetLength == 0) return sourceLength
        val dist = Array(sourceLength + 1) { IntArray(targetLength + 1) }
        for (i in 0 until sourceLength + 1) {
            dist[i][0] = i
        }
        for (j in 0..targetLength) {
            dist[0][j] = j
        }
        for (i in 1..sourceLength) {
            for (j in 1..targetLength) {
                val cost = if (source[i - 1] == target[j - 1]) 0 else 1
                dist[i][j] = Math.min(Math.min(dist[i - 1][j] + 1, dist[i][j - 1] + 1), dist[i - 1][j - 1] + cost)
                if (i > 1 && j > 1 && source[i - 1] == target[j - 2] && source[i - 2] == target[j - 1]) {
                    dist[i][j] = Math.min(dist[i][j], dist[i - 2][j - 2] + cost)
                }
                if (dist[i][j] > limit)
                    continue
            }
        }
        return dist[sourceLength][targetLength]
    }
}