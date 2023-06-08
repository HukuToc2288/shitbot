package ru.hukutoc2288.howtoshitbot.commands

import org.telegram.telegrambots.meta.api.objects.Message
import ru.hukutoc2288.howtoshitbot.bot
import ru.hukutoc2288.howtoshitbot.utils.CommandFunction

/**
 * Simple command which sending the same text on every execution
 */
class SimpleSendTextCommand(command: String,
                            description: String,
                            private val text: String,
                            aliases: Array<String> = arrayOf(),
                            private val isHtml: Boolean = false) : CommandFunction(command, description, aliases) {

    override val requiredFeatures: Int = Features.BASIC

    override fun execute(message: Message, argsLine: String) {
        if (isHtml) {
            bot.sendHtmlMessage(message.chatId, text, message.messageId)
        } else {
            bot.sendTextMessage(message.chatId, text, message.messageId)
        }
    }
}