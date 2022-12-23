package ru.hukutoc2288.howtoshitbot.utils

import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.Message
import java.util.UnknownFormatConversionException
import kotlin.reflect.KFunction2

/**
 * Класс, соединяющий команды, альясы и функции, которые они выполняют
 */
abstract class CommandFunction(
    val command: String,
    val description: String,
    val aliases: Array<String> = arrayOf(),
){
    abstract fun execute(message: Message, argsLine: String)
    open fun processCallback(callbackQuery: CallbackQuery){
    }

    fun shouldProcessCallback(data: String):Boolean = data.startsWith("$command/")
}