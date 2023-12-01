package ru.hukutoc2288.howtoshitbot.entinies.words

import java.time.LocalDate

data class ChatUserDate(
    val chatId: Long,
    val userId: Long,
    val date: LocalDate
)