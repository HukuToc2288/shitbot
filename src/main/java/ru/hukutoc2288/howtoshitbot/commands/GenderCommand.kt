package ru.hukutoc2288.howtoshitbot.commands

import java.util.Locale
import java.util.TreeMap
import kotlin.math.abs
import kotlin.reflect.full.primaryConstructor
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.hukutoc2288.howtoshitbot.bot
import ru.hukutoc2288.howtoshitbot.dao.GdDao
import ru.hukutoc2288.howtoshitbot.utils.CommandFunction
import ru.hukutoc2288.howtoshitbot.utils.displayName
import ru.hukutoc2288.howtoshitbot.utils.mention

class GenderCommand : CommandFunction("gender", "посмотреть или сменить гендер") {

    override val requiredFeatures: Int = Features.BASIC or Features.DB_RW


    private val keyboard: InlineKeyboardMarkup
        get() = run {
            val genders = GdDao.getDisplayGenders()
            InlineKeyboardMarkup(List(genders.size / 2 + genders.size % 2) {
                arrayListOf()
            }).apply {
                for (i in genders.indices) {
                    val gender = genders[i]
                    keyboard[i / 2].add(InlineKeyboardButton(gender.name.replaceFirstChar {
                        if (it.isLowerCase()) {
                            it.titlecase(Locale.ROOT)
                        } else {
                            it.toString()
                        }
                    }).apply {
                        callbackData = "$command/${gender.name}/${gender.id}"
                    })
                }
            }
        }

    override fun execute(message: Message, argsLine: String) {
        val sendMessage = SendMessage()
        val user = message.from
        val gender = GdDao.getGenderName(message.chatId, user)
        sendMessage.text = "${message.from.mention}, ты – $gender\n\nА ниже ты можешь сменить пол"
        sendMessage.chatId = message.chatId.toString()
        sendMessage.replyMarkup = keyboard
        sendMessage.replyToMessageId = message.messageId
        bot.execute(sendMessage)
    }

    override fun processCallback(callbackQuery: CallbackQuery) {
        if (!shouldProcessCallback(callbackQuery.data))
            return
        val callbackData = callbackQuery.data.split("/")
        val isGenderUpdated =
            GdDao.setGender(callbackQuery.message.chatId, callbackQuery.from.id, callbackData[2].toInt())
        val response = if (isGenderUpdated) {
            "Теперь ты ${callbackData[1]}"
        } else {
            "Не удалось обновить гендер. Возможно, что-то поменялся их список - попробуй вызвать /$command заново"
        }
        bot.execute(AnswerCallbackQuery().apply {
            callbackQueryId = callbackQuery.id
            showAlert = true
            text = response
        })
    }
}