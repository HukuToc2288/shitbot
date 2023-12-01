package ru.hukutoc2288.howtoshitbot.commands

import java.time.LocalDate
import org.telegram.telegrambots.meta.api.objects.Message
import ru.hukutoc2288.howtoshitbot.bot
import ru.hukutoc2288.howtoshitbot.dao.GdDao
import ru.hukutoc2288.howtoshitbot.entinies.words.WordsInfo
import ru.hukutoc2288.howtoshitbot.utils.CommandFunction
import ru.hukutoc2288.howtoshitbot.utils.displayName
import ru.hukutoc2288.howtoshitbot.utils.mention
import ru.hukutoc2288.howtoshitbot.utils.pluralize

class WordsStatsCommand : CommandFunction("stat", "моя статистика слов и матов", arrayOf("моя стата")) {
    override val requiredFeatures: Int = Features.BASIC or Features.DB_RW

    private val maxDays = 14L

    override fun execute(message: Message, argsLine: String) {
        GdDao.flushWordsData()

        val chatId = message.chatId
        val replyUser = message.replyToMessage?.from
        val userId = message.from.id
        val replyId = message.messageId
        val mention = message.from.mention

        val wordsInfos = GdDao.getWordsStats(chatId, replyUser?.id ?: userId, maxDays)

        val wordsInfoToday = wordsInfos[LocalDate.now()]
        if (wordsInfoToday == null) {
            bot.sendTextMessage(
                chatId,
                if (replyUser != null) {
                    "На участника ${replyUser.displayName} пока нет статы. Если это его первое сообщение, скорее всего она появится в течение минуты"
                } else {
                    "$mention, на тебя пока нет статы. Если это твоё первое сообщение, скорее всего она появится в течение минуты"
                },

            )
            return
        }
        val wordsInfoYesterday = wordsInfos.get(LocalDate.now().minusDays(1))
        val wordsInfoTotal = WordsInfo(0, 0)
        for (info in wordsInfos.values) {
            wordsInfoTotal.wordsCount = info.wordsCount
            wordsInfoTotal.profanityCount = info.profanityCount
        }
        bot.sendTextMessage(
            chatId,
            if (replyUser != null) {
                "Вот стата количества слов в чате участника ${replyUser.displayName}:\n"
            } else {
                "$mention, вот твоя стата количества слов в чате:\n"
            } +
                    "сегодня: ${createWordsInfoText(wordsInfoToday)}\n" +
                    "вчера: ${createWordsInfoText(wordsInfoYesterday)}\n" +
                    "за ${wordsInfos.size.pluralize("день", "дня", "дней")}: ${createWordsInfoText(wordsInfoTotal)}",
            replyId
        )
    }

    private fun createWordsInfoText(wordsInfo: WordsInfo?): String {
        wordsInfo ?: return "нет данных"
        return "${wordsInfo.wordsCount} (${wordsInfo.profanityCount.pluralize("мат", "мата", "матов")})"
    }
}