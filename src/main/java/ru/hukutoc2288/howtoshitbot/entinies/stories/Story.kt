package ru.hukutoc2288.howtoshitbot.entinies.stories

import ru.hukutoc2288.howtoshitbot.entinies.gayofday.GdUser
import java.sql.Timestamp

data class Story(
    val chatId: Long,
    val user: GdUser,
    val expireAfter: Timestamp,
    val message: String
)