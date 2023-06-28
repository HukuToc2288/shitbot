package ru.hukutoc2288.howtoshitbot.commands

import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.User
import ru.hukutoc2288.howtoshitbot.bot
import ru.hukutoc2288.howtoshitbot.dao.GdDao
import ru.hukutoc2288.howtoshitbot.entinies.stories.Story
import ru.hukutoc2288.howtoshitbot.utils.CommandFunction
import ru.hukutoc2288.howtoshitbot.utils.StoryUtils
import ru.hukutoc2288.howtoshitbot.utils.mention
import java.sql.Timestamp
import java.util.Calendar
import java.util.GregorianCalendar

class CreateStoryCommand : CommandFunction(
    "story",
    "Создать новую историю",
    arrayOf("создать историю")
) {
    override val requiredFeatures: Int = Features.BASIC or Features.DB_RW

    override fun execute(message: Message, argsLine: String) {
        if (argsLine.isBlank()) {
            bot.sendTextMessage(
                message.chatId,
                "Чтобы создать историю, используйте /story <текст>." +
                        " Во избежании спама, единовременно пользователь может иметь только одну историю в чате." +
                        " Истории живут 24 часа. Прочесть истории можно при помощи /stories",
                message.messageId
            )
            return
        }
        val storyExists: Boolean = GdDao.getSingleStory(message.chatId, message.from)?.let {
            StoryUtils.checkStory(it)
        } ?: false

        val expireTimestamp = Timestamp(GregorianCalendar().apply {
            add(Calendar.SECOND, StoryUtils.storyLifetime)
        }.timeInMillis)
        GdDao.createOrUpdateStory(message.chatId, message.from, expireTimestamp, argsLine)
        val messageToSend = "${message.from.mention}, твоя история успешно ${
            if (storyExists)
                "обновлена"
            else
                "добавлена"
        }! Чтобы читать свою и другие истории, используй /stories"
        bot.sendTextMessage(message.chatId, messageToSend,message.messageId)
    }
}