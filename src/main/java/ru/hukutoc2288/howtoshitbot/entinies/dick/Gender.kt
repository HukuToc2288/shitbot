package ru.hukutoc2288.howtoshitbot.entinies.dick

import kotlin.math.abs

class Gender(
    val id: Int,
    val changeText: String,
    val infoText: String,
    val thingPattern: String,
) {

    private val simpleThing = thingPattern.substring(0,(thingPattern.length) / 2)
    private val maxOrder: Int
    private val sizePattern: IntArray

    init {
        var tempMaxOrder = 0
        sizePattern = run {
            val patternString = thingPattern.substring( (thingPattern.length) / 2).toCharArray()
            IntArray(patternString.size) { digit ->
                val segmentOrder = patternString[digit].toString().toInt()
                if (segmentOrder > maxOrder) {
                    tempMaxOrder = segmentOrder
                }
                segmentOrder
            }
        }
        maxOrder = tempMaxOrder
    }

    fun buildChangeText(change: Int, newLength: Int): String {
        val textParts = changeText.split("/")
        // эта шизоидная магия позволяет обрабатывать строки вида "твой песюн /вырос/скоротился/ на %s см."
        val substitutionStart = if (change > 0) 1 else 2
        var format = ""
        for (i in textParts.indices) {
            if ((i - substitutionStart) % 3 == 0 || (i % 3) == 0) {
                format += textParts[i]
            }
        }
        val scaledThing = buildScaledThing(newLength)
        return format.format(abs(change), "$scaledThing $newLength")
    }

    fun buildInfoText(length: Int): String {
        val format = infoText
        val scaledThing = buildScaledThing(length)
        return format.format("$scaledThing $length")
    }

    fun buildScaledThing(length: Int): String {
        var scaledThing = ""
        val scales = IntArray(sizePattern.size) {
            1
        }
        var remainingSegments = length / 100
        val segmentsToIncrease = ArrayList<Int>(sizePattern.size)

        // calculate scales for thing
        main@ while (remainingSegments > 0) {
            for (i in maxOrder..1) {
                for (j in sizePattern.indices) {
                    if (sizePattern[j] == i) {
                        segmentsToIncrease.add(j)
                    }
                }
                if (segmentsToIncrease.size > remainingSegments) {
                    break@main
                }
                remainingSegments -= segmentsToIncrease.size
                for (j in segmentsToIncrease) {
                    scales[j] = scales[j] + 1
                }
                segmentsToIncrease.clear()
            }
        }

        // create scaled thing
        for (i in scales.indices) {
            scaledThing += simpleThing[i].toString().repeat(scales[i])
        }
        return scaledThing
    }
}