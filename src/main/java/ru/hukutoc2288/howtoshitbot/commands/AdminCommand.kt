package ru.hukutoc2288.howtoshitbot.commands

import org.telegram.telegrambots.meta.api.objects.Message
import ru.hukutoc2288.howtoshitbot.bot
import ru.hukutoc2288.howtoshitbot.dao.GdDao
import ru.hukutoc2288.howtoshitbot.utils.CommandFunction
import java.util.*
import kotlin.system.exitProcess

class AdminCommand : CommandFunction(
    "admin",
    "служебная команда, справка для которой есть, потому что админ ленивая жопа",
    arrayOf("админ", "админка")
) {

    // С одной стороны, если админка вызвана обычным пользователем, она не использует никакие внешние ресурсы
    // С другой стороны, если админка вызвана админом, она может использовать что угодно
    override val requiredFeatures: Int = Features.BASIC

    private val admins = arrayOf("HukuToc2288")
    private var adminPanelEnabled = true

    override fun execute(message: Message, argsLine: String) {
        if (argsLine.equals("вкл", ignoreCase = true)) {
            if ((message.from.userName in admins)) {
                bot.sendTextMessage(
                    message.chatId,
                    "Админка включена, теперь ты снова базированый шлёпа гигачад админ бота"
                )
                adminPanelEnabled = true
                return
            }
        } else if (argsLine.equals("выкл", ignoreCase = true)) {
            if ((message.from.userName in admins)) {
                bot.sendTextMessage(
                    message.chatId,
                    "Админка выключена, теперь ты сойжак кукож поридж простой смертный юзер"
                )
                adminPanelEnabled = false
                return
            }
        }
        if (!isFromAdmin(message)) {
            bot.sendTextMessage(
                message.chatId,
                "Ты не можешь использовать админку. Не вводи это команду. Забудь её. Если это будет продолжаться дальше, будут приняты меры"
            )
            return
        }
        val (command, argument) = extractCommand(argsLine)
        when (command.lowercase(Locale.getDefault())) {
            "паника" -> {
                bot.sendTextMessage(
                    message.chatId,
                    "На этом мои полномочия всё. Прощайте...",
                    message.messageId
                )
                exitProcess(0)
            }

            "бродкаст" -> {
                val allChats = GdDao.getAllChatIds()
                if (argument == null) {
                    bot.sendTextMessage(message.chatId, "Данная команда требует аргумент", message.messageId)
                    return
                }
                if (allChats == null) {
                    bot.sendTextMessage(message.chatId, "Не удалось получить список чатов", message.messageId)
                    return
                }
                for (chatId in allChats) {
                    bot.sendTextMessage(chatId, argument)
                }
            }
        }
    }

    private fun extractCommand(argsLine: String): Pair<String, String?> {
        val commandAndNewArgsLine = argsLine.split(' ', limit = 2)
        return commandAndNewArgsLine[0] to commandAndNewArgsLine.getOrNull(1)
    }

    private fun isFromAdmin(message: Message): Boolean {
        return adminPanelEnabled && (message.from.userName in admins)
    }

}
