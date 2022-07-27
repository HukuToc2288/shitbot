package ru.hukutoc2288.howtoshitbot.utils

import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.sql.Connection
import java.sql.DriverManager
import java.util.*

object GdConnectionFactory {
    private val dbUrl: String
    private val dbUser: String
    private val dbPassword: String

    init {
        val dbProperties = Properties().apply {
            load(InputStreamReader(FileInputStream("db.properties"),StandardCharsets.UTF_8))
        }
        dbUrl = dbProperties["url"] as String
        dbUser = dbProperties["user"] as String
        dbPassword = dbProperties["password"] as String
        Class.forName("org.postgresql.Driver")
    }

    fun getConnection(): Connection{
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword)
    }
}