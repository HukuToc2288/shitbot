package ru.hukutoc2288.howtoshitbot.commands

import org.telegram.telegrambots.meta.api.objects.Message
import ru.hukutoc2288.howtoshitbot.bot
import ru.hukutoc2288.howtoshitbot.entinies.uptime.UptimeResponse
import ru.hukutoc2288.howtoshitbot.mapper
import ru.hukutoc2288.howtoshitbot.utils.CommandFunction
import java.net.InetAddress
import java.net.Socket

class UptimeCommand : CommandFunction(
    "uptime",
    "процент доступности сервисов",
    arrayOf("аптайм")
) {

    override val requiredFeatures: Int = Features.BASIC or Features.EXTERNAL_API

    private val uptimeDaemonPort = 1340

    override fun execute(message: Message, argsLine: String) {
        var uptimeMessage = ""
        val dailyUptimeResponse = try {
            val clientSocket = Socket(InetAddress.getLoopbackAddress(), uptimeDaemonPort)
            mapper.readValue(String(clientSocket.getInputStream().readAllBytes()), UptimeResponse::class.java).also {
                clientSocket.close()
            }
        } catch (e: Exception) {
            bot.onError(
                message.chatId,
                "Не удалось добазариться со службой аптайма: ${e.message}\n\n"
            )
            return
        }
        if (dailyUptimeResponse.error != null) {
            bot.onError(
                message.chatId,
                "Служба аптайма вернула ошибку: ${dailyUptimeResponse.error}\n\n"
            )
            return
        }
        uptimeMessage += "Доступность сервисов сервера"
        dailyUptimeResponse.dailyUptimeList?.let {
            uptimeMessage += "\n\nЗа сутки:"
            for (uptimeEntry in it) {
                uptimeMessage += "\n${uptimeEntry.displayName} — ${uptimeEntry.uptimePercent}%"
            }
        }
        dailyUptimeResponse.monthlyUptimesList?.let {
            uptimeMessage += "\n\nЗа 30 дней:"
            for (uptimeEntry in it) {
                uptimeMessage += "\n${uptimeEntry.displayName} — ${uptimeEntry.uptimePercent}%"
            }
        }
        bot.sendTextMessage(message.chatId, uptimeMessage)

    }

}
