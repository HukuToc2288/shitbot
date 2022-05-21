package ru.hukutoc2288.howtoshitbot.entinies.uptime

import java.math.BigDecimal

data class UptimeEntry(
    var code: String,
    var displayName: String,
    var uptimePercent: BigDecimal
)