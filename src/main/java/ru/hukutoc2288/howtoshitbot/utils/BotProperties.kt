package ru.hukutoc2288.howtoshitbot.utils

import java.io.*
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.system.exitProcess

object BotProperties {
    lateinit var properties: Properties

    private val adminNameKey = "adminName"
    private val debugChatIdKey = "debugChatId"
    private val maintenanceReasonKey = "maintenanceReason"

    val adminName: String get() = properties[adminNameKey] as String
    val debugChatId: Long get() = (properties[debugChatIdKey] as String).toLong()
    val maintenanceReason: String get() = properties[maintenanceReasonKey] as String
    val maintaining: Boolean get() = maintenanceReason.isNotEmpty()

    fun update() {
        properties = initDefaults()
        try {
            properties.load(InputStreamReader(FileInputStream("shitbot.properties"),StandardCharsets.UTF_8))
        } catch (e: FileNotFoundException) {
            write(properties)
        } catch (e: Exception) {
            System.err.println("Файл shitbot.properties испорчен. Исправьте ошибки в файле или удалите его, чтобы пересоздать")
            exitProcess(1)
        }
    }

    private fun initDefaults(): Properties {
        val properties = Properties()
        properties[adminNameKey] = "HukuToc2288"
        properties[debugChatIdKey] = (-1001288811816).toString()
        properties[maintenanceReasonKey] = ""
        return properties
    }

    private fun write(properties: Properties) {
        try {
            properties.store(OutputStreamWriter(FileOutputStream("shitbot.properties"),StandardCharsets.UTF_8), null)
        } catch (e: Exception) {
            System.err.println("Файл shitbot.properties отсутствует и не может быть создан. Предоставьте право на запись или создайте файл вручную")
            exitProcess(1)
        }
    }
}