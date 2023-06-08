package ru.hukutoc2288.howtoshitbot.commands

import org.telegram.telegrambots.meta.api.objects.Message
import ru.hukutoc2288.howtoshitbot.bot
import ru.hukutoc2288.howtoshitbot.utils.CommandFunction
import java.lang.StringBuilder

class HelpCommand(private val commandList: List<CommandFunction>) :
        CommandFunction("help", "вызов справки", arrayOf("помощь")) {
    override val requiredFeatures: Int = Features.BASIC

    override fun execute(message: Message, argsLine: String) {
        val textToSend = StringBuilder()
        textToSend.append(
                "Сратьбот понимает команды в двух видах — команды Telegram, начинающиеся с \"/\"," +
                        " и текстовые команды (написаны в скобках), начинающиеся с обращения \"сратьбот\". " +
                        "Например, /coin и \"сратьбот монетка\" являются эвивалентными командами. Некоторые команды имеют " +
                        "<аргументы>, которые следуе вводить после команды, например /can какать или \"сратьбот можно какать\"\n\n " +
                        "Полный список команд:"
        )
        for (commandFunction in commandList) {
            textToSend.append("\n")
            textToSend.append("/")
            textToSend.append(commandFunction.command)
            textToSend.append(" (")
            textToSend.append(commandFunction.aliases.joinToString(", "))
            textToSend.append(") – ")
            textToSend.append(commandFunction.description)
        }
        bot.sendTextMessage(message.chatId, textToSend.toString())
    }
}