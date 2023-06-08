package ru.hukutoc2288.howtoshitbot.commands

import org.telegram.telegrambots.meta.api.objects.Message
import ru.hukutoc2288.howtoshitbot.bot
import ru.hukutoc2288.howtoshitbot.utils.CommandFunction
import java.text.SimpleDateFormat
import java.util.*

class TimeCommand : CommandFunction(
    "time",
    "Узнать сколько сейчас времени в классическом и десятичном формате",
    arrayOf("время")
) {
    override val requiredFeatures: Int = Features.BASIC or Features.EXTERNAL_API

    override fun execute(message: Message, argsLine: String) {
        val calendarNow = GregorianCalendar.getInstance()

        val calendarMidnight = GregorianCalendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val decimalSeconds: Long = (calendarNow.timeInMillis - calendarMidnight.timeInMillis) / 864
        val decimalTime = "${(decimalSeconds / 10000).toString().padStart(1, '0')}:" +
                "${(decimalSeconds / 100 % 100).toString().padStart(2, '0')}:" +
                "${(decimalSeconds % 100).toString().padStart(2, '0')}"

        val sdf = SimpleDateFormat("HH:mm:ss")
        val classicTime = sdf.format(calendarNow.time)

        bot.sendTextMessage(
            message.chatId,
            "Время на сервере (МСК):\n" +
                    "Десятичное — $decimalTime\n" +
                    "Классические — $classicTime"
        )
    }

}
