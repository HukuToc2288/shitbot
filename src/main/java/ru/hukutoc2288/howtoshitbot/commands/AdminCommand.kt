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

    var sudoMessageCallback: ((Message) -> Unit)? = null

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
                var succeedChats = 0
                var failedChats = 0
                for (chatId in allChats) {
                    try {
                        bot.sendTextMessage(chatId, argument)
                        succeedChats++
                    } catch (e: Exception) {
                        failedChats++
                    }
                }
                bot.sendTextMessage(
                    message.chatId,
                    "Сообщение разослано в $succeedChats частов\nНе удалось отправить в $failedChats чатов",
                    message.messageId
                )
            }
            "судо" -> {
                // TODO: это явно можно сделать красивее
                if (argument == null) {
                    bot.sendTextMessage(message.chatId, "Данная команда требует аргумент", message.messageId)
                    return
                }
                val (chatId, argument2) = extractCommand(argument)
                if (argument2 == null) {
                    bot.sendTextMessage(message.chatId, "Данная команда требует аргумент", message.messageId)
                    return
                }
                val (userId, argument3) = extractCommand(argument2)
                if (argument3 == null) {
                    bot.sendTextMessage(message.chatId, "Данная команда требует аргумент", message.messageId)
                    return
                }
                val chat = GdDao.getChatById(chatId.toLongOrNull() ?: 0)
                if (chat == null){
                    bot.sendTextMessage(message.chatId, "Чата нет в базе данных бота", message.messageId)
                    return
                }
                val userIds = GdDao.getUserIdsInChat(chat.id)
                if (!userIds.contains(userId.toLongOrNull())){
                    bot.sendTextMessage(message.chatId, "Пользователь не найден в данном чате", message.messageId)
                    return
                }
                val newUser = GdDao.getUserById(userId.toLong())
                if (newUser == null){
                    bot.sendTextMessage(message.chatId, "Пользователя нет в базе данных бота", message.messageId)
                    return
                }

                // change message sender, chat and text
                // although, it is definitely broken, that should be enough to send message as user
                message.chat.id = chatId.toLongOrNull() ?: 0
                message.from.id = newUser.id
                message.from.firstName = newUser.displayName
                message.from.userName = newUser.displayName
                message.from.lastName = null
                message.text = argument3

                // process as usual message
                val cb = sudoMessageCallback
                if (cb == null){
                    bot.sendTextMessage(message.chatId, "Отсутствует callback обработчика", message.messageId)
                } else {
                    cb.invoke(message)
                    bot.sendTextMessage(message.chatId, "Выполнено", message.messageId)
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
