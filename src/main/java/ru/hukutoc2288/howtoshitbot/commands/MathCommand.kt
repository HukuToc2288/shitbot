package ru.hukutoc2288.howtoshitbot.commands

import java.util.Timer
import java.util.TimerTask
import org.mariuszgromada.math.mxparser.Expression
import org.mariuszgromada.math.mxparser.License
import org.mariuszgromada.math.mxparser.mXparser
import org.telegram.telegrambots.meta.api.objects.Message
import ru.hukutoc2288.howtoshitbot.bot
import ru.hukutoc2288.howtoshitbot.utils.CommandFunction

class MathCommand : CommandFunction(
    "math",
    "вычисление математических выражений (использует mXparser)",
    arrayOf("математика", "посчитай", "сколько будет", "считай", "вычисли")
) {
    override val requiredFeatures: Int = Features.BASIC



    init {
        if (!License.checkIfUseTypeConfirmed()) {
            License.iConfirmNonCommercialUse("t.me/howToShitBot")
        }
    }

    override fun execute(message: Message, argsLine: String) {
        val lock = Any()
        var shouldPrintMessage = true

        val expression = Expression(argsLine)
        val cancelTimer = Timer()
        cancelTimer.schedule(object : TimerTask(){
            override fun run() {
                synchronized(lock) {
                    if (!shouldPrintMessage){
                        return
                    }
                    shouldPrintMessage = false
                    mXparser.cancelCurrentCalculation()
                    bot.sendTextMessage(
                        message.chatId,
                        "Это выражение слишком сложное. Не выпендривайся - будь проще",
                        message.messageId
                    )
                }
            }

        },3000)

        val result = expression.calculate()

        synchronized(lock) {
            if (!shouldPrintMessage) {
                mXparser.resetCancelCurrentCalculationFlag()
                return
            }
            shouldPrintMessage = false
            cancelTimer.cancel()

            val answerString = if (result.isNaN()) {
                "Не могу вычислить значение. Проверь правильность выражения. Также некоторые операции могут не поддерживаться"
            } else if (result % 1.0 == 0.0) {
                "%d".format(result.toInt())
            } else {
                "%.4f".format(result)
            }
            bot.sendTextMessage(
                message.chatId,
                answerString,
                message.messageId
            )
        }
    }
}