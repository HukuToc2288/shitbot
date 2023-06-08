package ru.hukutoc2288.howtoshitbot.commands

import org.telegram.telegrambots.meta.api.methods.send.SendSticker
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.Message
import ru.hukutoc2288.howtoshitbot.bot
import ru.hukutoc2288.howtoshitbot.utils.CommandFunction

class FlipCoinCommand : CommandFunction(
    "coin",
    "Подбросить монетку",
    arrayOf("монетка")
) {
    override val requiredFeatures: Int = Features.BASIC

    private val headsStickerId = "CAACAgIAAxkBAAEEf8FiXGqv5jHeRmxbzHNFIzjqOCLJBQACDhgAAqF14UqqV4QOJ--W9iQE"
    private val tailsStickerId = "CAACAgIAAxkBAAEEf8NiXGqzuFIULCbMaZTh_2phHm4KFwAC9BkAAvhi4UpfWs4ysHF52CQE"

    override fun execute(message: Message, argsLine: String) {
        val coinMessage = SendSticker()
        coinMessage.chatId = message.chatId.toString()
        coinMessage.sticker = InputFile(arrayOf(headsStickerId, tailsStickerId).random())
        bot.execute(coinMessage)
    }

}
