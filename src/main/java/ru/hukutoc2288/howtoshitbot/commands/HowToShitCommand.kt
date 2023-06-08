package ru.hukutoc2288.howtoshitbot.commands

import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.Message
import ru.hukutoc2288.howtoshitbot.bot
import ru.hukutoc2288.howtoshitbot.utils.CommandFunction
import java.io.File

class HowToShitCommand : CommandFunction("how",
        "экстренная помощь по вопросам, связанных с процессом дефекации",
        arrayOf("как какать", "как какать?", "а как какать", "а как какать")) {

    override val requiredFeatures: Int = Features.BASIC

    override fun execute(message: Message, argsLine: String) {
        val sendPhoto = SendPhoto()
        // TODO: use file_ids instead of files
        val imageName = when ((0..9).random()) {
            0 -> "/home/huku/Pictures/howToShitStanding.jpg"
            1 -> "/home/huku/Pictures/howToShitBokom.jpg"
            else -> "/home/huku/Pictures/howToShit.jpg"
        }
        sendPhoto.photo = InputFile(File(imageName))
        sendPhoto.chatId = message.chatId.toString()
        bot.execute(sendPhoto)
    }

}