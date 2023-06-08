package ru.hukutoc2288.howtoshitbot.commands

import org.telegram.telegrambots.meta.api.objects.Message
import ru.hukutoc2288.howtoshitbot.bot
import ru.hukutoc2288.howtoshitbot.dao.GdDao
import ru.hukutoc2288.howtoshitbot.utils.BotProperties
import ru.hukutoc2288.howtoshitbot.utils.CommandFunction
import ru.hukutoc2288.howtoshitbot.utils.DateUtils
import java.sql.Timestamp
import java.util.*
import kotlin.concurrent.schedule

class PidorCommand: CommandFunction(
        "pidor",
        "найти кто сегодня пидор дня",
        arrayOf("пидор")
) {
    override val requiredFeatures: Int = Features.BASIC or Features.DB_RW

    override fun execute(message: Message, argsLine: String) {
        val chatId = message.chatId
        val chat = GdDao.getChatById(chatId)
        val userIds = GdDao.getUserIdsInChat(chatId)
        GdDao.updateUserName(message.from)
        // not enough players branch
        if (chat == null || userIds.isEmpty()) {
            bot.onError(
                    chatId,
                    "кажется, в этом чате ещё никто не участвует в игре \"Пидор дня\". Обычно бот сам находит игроков, но, видимо, что-то идёт не так"
            )
            return
        }
        if (userIds.size < 2) {
            bot.onError(
                    chatId,
                    "чтобы играть в \"Пидор дня\", нужно минимум два игрока. Пользователь автоматически станет игроком, как только напишет сообщение в чат"
            )
            return
        }
        // now we're playing

        val nowCalendar = GregorianCalendar()
        val previousCalendar = GregorianCalendar().apply {
            time = chat.lastTime
        }
        val gayUser = GdDao.getGayInChat(chatId)
        if (gayUser != null && DateUtils.isToday(previousCalendar)) {
            // gay already chosen branch
            val tomorrowCalendar = GregorianCalendar().apply {
                add(Calendar.DATE, 1)
            }
            val nextTimeString =
                    if (nowCalendar.get(Calendar.HOUR_OF_DAY) == 23) {
                        if (nowCalendar.get(Calendar.MINUTE) == 59) {
                            if (nowCalendar.get(Calendar.SECOND) == 59) {
                                "сейчас"
                            } else {
                                "${60 - tomorrowCalendar.get(Calendar.SECOND)} секунд"
                            }
                        } else {
                            "${60 - tomorrowCalendar.get(Calendar.MINUTE)} минут"
                        }
                    } else {
                        "${24 - tomorrowCalendar.get(Calendar.HOUR_OF_DAY)} часов"
                    }
            bot.sendHtmlMessage(
                    chatId,
                    "По результатам розыгрыша, пидор дня сегодня ${gayUser.displayName}\n" +
                            "Следующий розыгрыш можно будет провести через $nextTimeString", message.messageId
            )
            return
        }
        // chose new gay branch
        val gayId = userIds.random()
        val newGayUser = GdDao.getUserById(gayId)
        if (newGayUser == null) {
            bot.onError(
                    chatId,
                    "этого не должно было произойти, но это произошло. Обратитесь к админу бота @${BotProperties.adminName}"
            )
            return
        }
        GdDao.updateGayInChat(Timestamp(nowCalendar.timeInMillis), chatId, gayId)
        //val textMention = "<a href=\"tg://user?id=${newGayUser.id}\">${newGayUser.displayName}</a>"
        val textMention = newGayUser.displayName
        if (gayUser == null)
            bot.sendTextMessage(
                    chatId,
                    "Хм, похоже пидор дня сегодня уже был выбран, но куда-то исчез. Что ж, проведём внеочередной розыгрыш...",
                    message.messageId
            )
        else
            bot.sendTextMessage(chatId, "Тааак, сейчас посмотрим...", message.messageId)
        Timer().schedule(3000) {
            bot.sendHtmlMessage(chatId, "Ага, нашёл его! Пидор дня сегодня $textMention", message.messageId)
        }
    }

}