package ru.hukutoc2288.howtoshitbot.utils

import com.zaxxer.hikari.HikariDataSource
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
            load(InputStreamReader(FileInputStream("db.properties"), StandardCharsets.UTF_8))
        }
        dbUrl = dbProperties["url"] as String
        dbUser = dbProperties["user"] as String
        dbPassword = dbProperties["password"] as String
        Class.forName("org.postgresql.Driver")
    }

    private val pool = HikariDataSource().apply {
        jdbcUrl = "${dbUrl}"
        password = dbPassword
        username = dbUser
        addDataSourceProperty("cachePrepStmts", "true");
        addDataSourceProperty("prepStmtCacheSize", "250");
        addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        maximumPoolSize = 1
        isAutoCommit = false
        idleTimeout = 60_000

    }

    fun getConnection(): Connection {
        return pool.connection
    }
}