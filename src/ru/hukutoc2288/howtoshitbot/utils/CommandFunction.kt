package ru.hukutoc2288.howtoshitbot.utils

import org.telegram.telegrambots.meta.api.objects.Message
import kotlin.reflect.KFunction2

/**
 * Класс, соединяющий команды, альясы и функции, которые они выполняют
 */
class CommandFunction(
    val command: String,
    val description: String,
    val function: KFunction2<Message, String, Unit>,
    val aliases: Array<String> = arrayOf(),
)