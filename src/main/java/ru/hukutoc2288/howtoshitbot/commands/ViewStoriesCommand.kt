package ru.hukutoc2288.howtoshitbot.commands

import org.telegram.telegrambots.meta.api.objects.Message
import ru.hukutoc2288.howtoshitbot.bot
import ru.hukutoc2288.howtoshitbot.dao.GdDao
import ru.hukutoc2288.howtoshitbot.entinies.stories.Story
import ru.hukutoc2288.howtoshitbot.utils.CommandFunction
import java.util.*
import kotlin.collections.ArrayList

class ViewStoriesCommand : CommandFunction(
    "stories",
    "просмотреть истории в этом чате",
    arrayOf("истории")
) {
    override val requiredFeatures: Int = Features.BASIC or Features.DB_RO

    override fun execute(message: Message, argsLine: String) {
        val stories = GdDao.getStoriesInChat(message.chatId)
        val actualStories = ArrayList<Story>(stories.size)
        val expiredStories = ArrayList<Story>(stories.size)
        for (story in stories) {
            if (checkStory(story))
                actualStories.add(story)
            else
                expiredStories.add(story)
        }
        // TODO: удалять истории, у которых вышло время
        if (actualStories.isEmpty()) {
            bot.sendTextMessage(message.chatId, "Сейчас в этом чате нет историй. Создай свою сейчас при помощи /story")
            return
        }
        var messageToSend = "Истории в этом чате:\n"
        for (story in actualStories) {
            messageToSend += buildStoryLine(story)
        }
        bot.sendHtmlMessage(message.chatId, messageToSend, message.messageId)
    }

    private fun checkStory(story: Story): Boolean {
        return story.expireAfter.after(Date())
    }

    private fun buildStoryLine(story: Story): String {
        with(story) {
            return "\n${user.displayName}: $message"
        }
    }
}