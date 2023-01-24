package ru.hukutoc2288.howtoshitbot.commands

import org.telegram.telegrambots.meta.api.objects.Message
import ru.hukutoc2288.howtoshitbot.bot
import ru.hukutoc2288.howtoshitbot.dao.GdDao
import ru.hukutoc2288.howtoshitbot.utils.CommandFunction

object AnekCommand : CommandFunction("anek", "рассказать анек", arrayOf("анек", "анекдот", "шутка")) {

    override fun execute(message: Message, argsLine: String) {
        val anek = GdDao.getRandomAnek(50)
        bot.sendTextMessage(message.chatId,anek?.second.toString())
        //bot.sendHtmlMessage(message.chatId, "${anek?.second}\n\n<a href='https://www.anekdot.ru/id/${anek?.first}/'>Источник</a>")
    }
}