package ru.hukutoc2288.howtoshitbot.utils.words

import java.time.LocalDate
import java.util.Locale
import ru.hukutoc2288.howtoshitbot.dao.GdDao
import ru.hukutoc2288.howtoshitbot.entinies.words.WordsInfo

class WordsCounter {

    fun countWords(chatId: Long, userId: Long, date: LocalDate, message: String): WordsInfo {
        val wordsInfo = countWordsInMessage(message)
        GdDao.updateWordsCount(chatId, userId, date, wordsInfo)
        return wordsInfo
    }

    fun countWordsInMessage(message: String): WordsInfo {
        val allWords = message.split("""((\s+|\p{Punct})|(\r\n|\r|\n))""".toRegex())
        var actualWordsCount = 0
        var profanityCount = 0
        for (word in allWords) {
            if (word.isEmpty()) {
                continue
            }
            actualWordsCount++
            if (isWordProfanity(word)) {
                profanityCount++
            }
        }
        return WordsInfo(actualWordsCount, profanityCount)
    }

    fun isWordProfanity(word: String): Boolean {
        if (word.length <= 2) {
            return false
        }
        val lowerCaseWord = word.lowercase(Locale.getDefault())
        if (lowerCaseWord.contains("ху[йиеюёя]".toRegex())){
            return true
        }
        if (lowerCaseWord.contains("пизд")) {
            return true
        }
        if (lowerCaseWord.contains("бля") && lowerCaseWord != "корабля") {
            return true
        }
        // this one is hard to filter
        if (lowerCaseWord.contains("[ъьйаяоёуюэеиы][её]б".toRegex())) {
            return true
        }
        return false
    }
}