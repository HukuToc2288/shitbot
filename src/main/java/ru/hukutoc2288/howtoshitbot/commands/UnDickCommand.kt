package ru.hukutoc2288.howtoshitbot.commands

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import org.telegram.telegrambots.meta.api.objects.Message
import ru.hukutoc2288.howtoshitbot.bot
import ru.hukutoc2288.howtoshitbot.dao.GdDao
import ru.hukutoc2288.howtoshitbot.utils.CommandFunction
import ru.hukutoc2288.howtoshitbot.utils.mention

class UnDickCommand() : CommandFunction("undick", "выйти из игры $gameName", arrayOf("непесюн")) {
    override val requiredFeatures: Int = Features.BASIC or Features.DB_RW

    override fun execute(message: Message, argsLine: String) {
        val chatId = message.chatId
        val user = message.from

        val (dick, lastUnDickDate) = GdDao.getUnDickDate(chatId, message.from)
        if (dick == 0) {
            // user not registered branch
            bot.sendHtmlMessage(
                chatId,
                "${user.mention}, ты не зарегистрирован в игре $gameName",
                message.messageId
            )
            return
        }

        val dateNow = LocalDate.now()
        val daysSinceUnDick = ChronoUnit.DAYS.between(lastUnDickDate, dateNow)
        if (daysSinceUnDick < unDickThresholdDays) {
            // wait to undick branch
            bot.sendHtmlMessage(
                chatId,
                "${user.mention}, ты уже недавно выходил из игры. Ты сможешь выйти снова не раньше чем через $unDickThresholdDays дней",
                message.messageId
            )
            return
        }

        if (argsLine != dick.toString()) {
            // verification not match
            val reason = if (argsLine.isBlank()) {
                // user just called /undick without arguments
                ""
            } else {
                // user called /undick with wrong argument
                "Ты ввёл неправльную длину для проверки! "
            }
            bot.sendHtmlMessage(
                chatId,
                "$reason${user.mention}, длина твоего песюна $dick см." +
                        " Ты действительно хочешь выйти из игры и сбросить свой результат?" +
                        " Если да, набери команду <code>/$command $dick</code>. Обрати внимание," +
                        " что после удаления песюна ты не сможешь делать это следующие $unDickThresholdDays" +
                        " дней, если снова вернёшься в игру",
                message.messageId
            )
            return
        }

        // remove dick <triple knife emoji>
        GdDao.unDick(chatId, user, dateNow)
        bot.sendHtmlMessage(
            chatId,
            "${user.mention}, теперь у тебя нет песюна в этом чате." +
                    " Если ты снова захочешь вернуться в игру, ты можешь сделать это командой /dick",
            message.messageId
        )
    }

    companion object {
        private const val unDickThresholdDays = 14L
        private const val gameName = "\"Песюн\""
    }
}