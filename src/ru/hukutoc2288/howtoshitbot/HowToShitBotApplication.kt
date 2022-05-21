package ru.hukutoc2288.howtoshitbot


import org.telegram.telegrambots.updatesreceivers.DefaultBotSession

import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException

fun main(args: Array<String>) {
    val telegramBotsApi = TelegramBotsApi(DefaultBotSession::class.java)

    try {
        telegramBotsApi.registerBot(Bot())
    } catch (e: TelegramApiRequestException) {
        e.printStackTrace()
    }
}

