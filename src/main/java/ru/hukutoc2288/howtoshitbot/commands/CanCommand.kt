package ru.hukutoc2288.howtoshitbot.commands

import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message
import ru.hukutoc2288.howtoshitbot.bot
import ru.hukutoc2288.howtoshitbot.utils.CommandFunction
import java.text.SimpleDateFormat
import java.util.*

class CanCommand : CommandFunction(  "can",
    "<вопрос> спросить разрешение у Сратьбота на то или иное действие",
    arrayOf("можно ли", "могу ли я", "можно", "can")) {
    override val requiredFeatures: Int = Features.BASIC

    override fun execute(message: Message, argsLine: String) {
        val canMessage = SendMessage()
        val questionToProcess = argsLine
            .trim()
            .replace("\\s+".toRegex(), " ")
            .lowercase(Locale.getDefault())
            .replace("\\?+$".toRegex(), "")
        val questionToReply = argsLine.replace("\\?+$".toRegex(), "")
        val textToSend: String = if (questionToProcess.isBlank()) {
            "Можно что? После команды следует задать вопрос, например \"Сратьбот можно какать?\""
        } else {
            val benediction = (Integer.bitCount((questionToProcess + message.from.id.toString() +
                        SimpleDateFormat("dd-MM-YYYY").format(Date())).hashCode())  % 2 == 0) ||
                    (message.from.firstName == "Владимир" && message.from.lastName == "Путин")
            val userName = message.from.firstName +
                    if (message.from.lastName != null) " ${message.from.lastName}" else ""
            if (benediction) {
                "$userName, сегодня тебе МОЖНО $questionToReply! Так сделай же это!"
            } else {
                "$userName, сегодня тебе НЕЛЬЗЯ $questionToReply! Отложи это до завтра"
            }
        }
        canMessage.text = textToSend
        canMessage.chatId = message.chatId.toString()
        //canMessage.replyToMessageId = message.messageId
        bot.execute(canMessage)
    }

}
