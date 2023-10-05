package ru.hukutoc2288.howtoshitbot.commands

import org.telegram.telegrambots.meta.api.objects.Message
import ru.hukutoc2288.howtoshitbot.bot
import ru.hukutoc2288.howtoshitbot.dao.GdDao
import ru.hukutoc2288.howtoshitbot.utils.CommandFunction
import ru.hukutoc2288.howtoshitbot.utils.DateUtils
import java.sql.Timestamp
import java.util.*
import kotlin.math.max
import ru.hukutoc2288.howtoshitbot.commands.knb.KnbCommand
import ru.hukutoc2288.howtoshitbot.utils.pluralize

object DickCommand : CommandFunction("dick", "сыграть в игру \"Песюн\"", arrayOf("песюн")) {

    override val requiredFeatures: Int = Features.BASIC or Features.DB_RW

    override fun execute(message: Message, argsLine: String) {
        val chatId = message.chatId
        val user = message.from
        val mention = if (user.userName != null) {
            "@${user.userName}"
        } else if (user.lastName != null) {
            "<a href=\"tg://user?id=${user.id}\">${user.firstName} ${user.lastName}</a>"
        } else {
            "<a href=\"tg://user?id=${user.id}\">${user.firstName}</a>"
        }
        val dickInfo = GdDao.getDick(chatId, message.from)

        val nowCalendar = GregorianCalendar()
        val tomorrowCalendar = GregorianCalendar().apply {
            add(Calendar.DATE, 1)
        }
        val nextTimeString =
            if (nowCalendar.get(Calendar.HOUR_OF_DAY) == 23) {
                if (nowCalendar.get(Calendar.MINUTE) == 59) {
                    if (nowCalendar.get(Calendar.SECOND) == 59) {
                        "сейчас"
                    } else {
                        (60 - tomorrowCalendar.get(Calendar.SECOND)).pluralize("секунду","секунды","секунд")
                    }
                } else {
                    (60 - tomorrowCalendar.get(Calendar.MINUTE)).pluralize("минуту","минуты","минут")
                }
            } else {
                    (24 - tomorrowCalendar.get(Calendar.HOUR_OF_DAY)).pluralize("час","часа","часов")
            }

        if (dickInfo == null) {
            // no dick branch
            val dickSize = GdDao.getAverageDick(message.chatId) ?: (1..10).random()
            bot.sendHtmlMessage(
                chatId,
                "$mention, теперь у тебя есть песюн в этом чате, и его длина $dickSize см. Продолжай играть через $nextTimeString",
                message.messageId
            )
            GdDao.updateDick(chatId, user, Timestamp(nowCalendar.timeInMillis), dickSize)
            return
        }
        if (DateUtils.isToday(dickInfo.first)) {
            // already measured branch
            bot.sendHtmlMessage(
                chatId,
                "$mention, ты сегодня уже играл, и длина твоего песюна ${buildTextDick(dickInfo.second)}. Продолжай играть через $nextTimeString",
                message.messageId
            )
            return
        }

        // play branch
        // песюн не может быть менее 1 см (а почему??)
        val dickChange = (max(-5, -dickInfo.second + 1)..9).random().let {
            // ага, 0 это 10
            if (it == 0)
                10
            else
                it
        }
        bot.sendHtmlMessage(
            chatId,
            "$mention, твой песюн ${if (dickChange > 0) "вырос на $dickChange" else "скоротился на ${-dickChange}"} см.\n" +
                    "Теперь его длина ${buildTextDick(dickInfo.second + dickChange)}. Продолжай играть через $nextTimeString" +
                    if (dickInfo.second + dickChange <= KnbCommand.bet &&
                        KnbCommand.waitingPlayers[chatId]?.first?.id == user.id && KnbCommand.waitingPlayers.remove(
                            chatId
                        ) != null
                    ) {
                        "\n\nДлина твоего песюна стала меньше ${KnbCommand.bet + 1} см, поэтому ты исключён из игры ${KnbCommand.gameTitle}"
                    } else {
                        ""
                    },
            message.messageId
        )
        GdDao.updateDick(chatId, user, Timestamp(nowCalendar.timeInMillis), dickInfo.second + dickChange)
        return
    }

    private fun buildTextDick(dickLength: Int): String{
        val segments = (dickLength)/100+1
        return "8${"=".repeat(segments)}D $dickLength см"
    }
}