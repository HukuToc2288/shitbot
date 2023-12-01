package ru.hukutoc2288.howtoshitbot.dao

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.Statement
import ru.hukutoc2288.howtoshitbot.utils.GdConnectionFactory

object DbHelper {

    fun init() {
        createTables()
        addGenders()
    }

    private fun createTables() {
        val connection: Connection?
        val statement: Statement?
        val ac = AutoClose()
        try {
            connection = GdConnectionFactory.getConnection().autoClose(ac)
            statement = connection.createStatement().autoClose(ac)
            // TODO: create all tables here
            statement.addBatch(
                "CREATE TABLE IF NOT EXISTS genders (" +
                        "id SMALLINT PRIMARY KEY NOT NULL," +
                        "name VARCHAR(31) NOT NULL," +
                        "change_text VARCHAR(255) NOT NULL," +
                        "info_text VARCHAR(255) NOT NULL," +
                        "thing VARCHAR(31) NOT NULL)"
            )
            statement.addBatch(
                "CREATE TABLE IF NOT EXISTS users_genders (" +
                        "chat_id BIGINT NOT NULL," +
                        "user_id BIGINT NOT NULL," +
                        "gender_id SMALLINT NOT NULL," +
                        "PRIMARY KEY (chat_id,user_id))"
            )
            statement.executeBatch()
            connection.commit()
        } finally {
            ac.closeResources()
        }
    }

    private fun addGenders() {
        // we need at least one gender to not crash
        val connection: Connection?
        val statement: PreparedStatement?
        val ac = AutoClose()
        try {
            connection = GdConnectionFactory.getConnection().autoClose(ac)
            statement =
                connection.prepareStatement("INSERT INTO genders (id,name,change_text,info_text,thing) VALUES (?,?,?,?,?) ON CONFLICT DO NOTHING")
                    .autoClose(ac)
            statement.setInt(1, 0)
            statement.setString(2, "мужчина")
            statement.setString(3, "твой песюн /вырос/скоротился/ на %s см. Теперь его длина %s см.")
            statement.setString(4, "ты сегодня уже играл, и длина твоего песюна %s см.")
            statement.setString(5, "8=D010")
            statement.addBatch()

            statement.setInt(1, 1)
            statement.setString(2, "женщина")
            statement.setString(3, "твоя вагина /выросла/уменьшилась/ на %s см. Теперь её глубина %s см.")
            statement.setString(4, "ты сегодня уже играла, и глубина твоей вагины %s см.")
            statement.setString(5, "(.)101")
            statement.addBatch()

            statement.setInt(1, 3)
            statement.setString(2, "женщина с членом")
            statement.setString(3, "твой женский песюн /вырос/скоротился/ на %s см. Теперь его длина %s см.")
            statement.setString(4, "ты сегодня уже играла, и длина твоего женского песюна %s см.")
            statement.setString(5, "8=D010")
            statement.addBatch()

            statement.setInt(1, 2)
            statement.setString(2, "мужчина без члена")
            statement.setString(3, "твоя мужская вагина /выросла/уменьшилась/ на %s см. Теперь её глубина %s см.")
            statement.setString(4, "ты сегодня уже играл, и глубина твоей мужской вагины %s см.")
            statement.setString(5, "(.)101")
            statement.addBatch()


            statement.setInt(1, 4)
            statement.setString(2, "боевой вертолёт")
            statement.setString(3, "размах твоих лопастей /увеличился/уменьшился/ на %s см. Теперь их размах %s см.")
            statement.setString(4, "ты сегодня уже играл, и размах твоих лопастей %s см.")
            statement.setString(5, "=-0-=21012")
            statement.addBatch()

            statement.setInt(1, 5)
            statement.setString(2, "иное")
            statement.setString(3, "твой иной половой орган /увеличился/уменьшился/ на %s см. Теперь его размер %s см.")
            statement.setString(4, "ты сегодня уже играло, и размер твоего иного полового органа %s см.")
            statement.setString(5, "¿?11")
            statement.addBatch()

            statement.executeBatch()
            connection.commit()
        } finally {
            ac.closeResources()
        }
    }
}