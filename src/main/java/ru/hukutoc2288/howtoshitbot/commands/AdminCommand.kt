package ru.hukutoc2288.howtoshitbot.commands

import org.telegram.telegrambots.meta.api.objects.Message
import ru.hukutoc2288.howtoshitbot.bot
import ru.hukutoc2288.howtoshitbot.utils.CommandFunction
import kotlin.system.exitProcess

class AdminCommand : CommandFunction( "admin",
    "служебная команда, справка для которой есть, потому что админ ленивая жопа",
    arrayOf("админ", "админка")) {
    override val requiredFeatures: Int
        get() = TODO("Not yet implemented")

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
        if (argsLine.equals("паника", true)) {
            bot.sendTextMessage(
                message.chatId,
                "На этом мои полномочия всё. Прощайте..."
            )
            exitProcess(0)
        }
    }

    private fun isFromAdmin(message: Message): Boolean {
        return adminPanelEnabled && (message.from.userName in admins)
    }

}
