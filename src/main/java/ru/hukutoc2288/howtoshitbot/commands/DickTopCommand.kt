package ru.hukutoc2288.howtoshitbot.commands

import org.telegram.telegrambots.meta.api.objects.Message
import ru.hukutoc2288.howtoshitbot.bot
import ru.hukutoc2288.howtoshitbot.dao.GdDao
import ru.hukutoc2288.howtoshitbot.utils.CommandFunction

class DickTopCommand : CommandFunction(
        "top",
        "топ песюнов",
        arrayOf()
) {
    override val requiredFeatures: Int = Features.BASIC or Features.DB_RO

    override fun execute(message: Message, argsLine: String) {
        val dickTop = GdDao.getDickTop(message.chatId, message.from)
        if (dickTop.isEmpty()) {
            bot.sendTextMessage(
                    message.chatId,
                    "Похоже, в этом чате ещё ни у кого нет песюна. Напиши /dick, чтобы начать играть"
            )
            return
        }
        val averageDick = GdDao.getAverageDick(message.chatId)
        val dickMessage =
                "Средняя длина песюна в чате — $averageDick см\n\nТоп песюнов:\n" + dickTop.joinToString("\n") {
                    "${it.place}. ${if (it.isMe) " \uD83D\uDC49" else ""} ${it.displayName} — ${it.dickSize} см"
                }
        bot.sendHtmlMessage(message.chatId, dickMessage)
    }
}