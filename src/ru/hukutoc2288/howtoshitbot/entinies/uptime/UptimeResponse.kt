package ru.hukutoc2288.howtoshitbot.entinies.uptime

import ru.hukutoc2288.howtoshitbot.entinies.uptime.UptimeEntry

data class UptimeResponse(
    var dailyUptimeList: List<UptimeEntry>? = null,
    var monthlyUptimesList: List<UptimeEntry>? = null,
    var error: String? = null
)
