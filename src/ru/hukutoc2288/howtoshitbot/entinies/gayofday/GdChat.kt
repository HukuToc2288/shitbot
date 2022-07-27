package ru.hukutoc2288.howtoshitbot.entinies.gayofday

import java.sql.Timestamp

data class GdChat(
    var id: Long,
    var gayId: Long = 0,
    var lastTime: Timestamp = Timestamp(0),
)