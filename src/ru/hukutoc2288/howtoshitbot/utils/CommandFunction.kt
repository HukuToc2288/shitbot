package ru.hukutoc2288.howtoshitbot.utils

import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.Message

/**
 * Класс, соединяющий команды, альясы и функции, которые они выполняют
 */
abstract class CommandFunction(
    val command: String,
    val description: String,
    val aliases: Array<String> = arrayOf(),
) {
    abstract val requiredFeatures: Int

    abstract fun execute(message: Message, argsLine: String)

    open fun processCallback(callbackQuery: CallbackQuery) {
    }

    fun shouldProcessCallback(data: String): Boolean = data.startsWith("$command/")

    fun isCommandAvailable(disabledFeatures: Int): Boolean =
        (requiredFeatures and disabledFeatures.inv()) == requiredFeatures

    companion object Features {
        const val BASIC = 1 shl 0   // Нужно всегда ставить, иначе команду будет нельзя отключить
        const val DB_RO = 1 shl 1   // Только чтение с базы данных
        const val DB_RW = 1 shl 3 + DB_RO  // Чтение и запись в БД
        const val EXTERNAL_API = 1 shl 4    // Работа с внешним API (в т.ч. в локальной сети)
    }
}