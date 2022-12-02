package ru.hukutoc2288.howtoshitbot.dao

import ru.hukutoc2288.howtoshitbot.entinies.gayofday.GdChat
import ru.hukutoc2288.howtoshitbot.entinies.gayofday.GdUser
import ru.hukutoc2288.howtoshitbot.utils.GdConnectionFactory
import java.sql.Timestamp
import java.util.*
import kotlin.collections.HashSet

class GdDao {
    // TODO: 02.12.2022 так как теперь эта база не только для пидора дня, нужно поменять все названия
    fun getChatById(chatId: Long): GdChat? {
        val connection = GdConnectionFactory.getConnection()
        val statement = connection.prepareStatement("SELECT * FROM chats WHERE chatid = ?").apply {
            setLong(1, chatId)
        }
        if (!statement.execute())
            return null
        val resultSet = statement.resultSet
        if (!resultSet.next())
            return null
        return GdChat(
            resultSet.getLong("chatid"),
            resultSet.getLong("gayid"),
            resultSet.getTimestamp("lasttime")
        )
    }

    fun addChat(chatId: Long) {
        val connection = GdConnectionFactory.getConnection()
        val statement =
            connection.prepareStatement("INSERT INTO chats (chatid) VALUES (?) ON CONFLICT DO NOTHING").apply {
                setLong(1, chatId)
            }
        statement.executeUpdate()
    }

    fun getUserById(userId: Long): GdUser? {
        val connection = GdConnectionFactory.getConnection()
        val statement = connection.prepareStatement("SELECT * FROM users WHERE userid = ?").apply {
            setLong(1, userId)
        }
        if (!statement.execute())
            return null
        val resultSet = statement.resultSet
        if (!resultSet.next())
            return null
        return GdUser(
            resultSet.getLong("userid"),
            resultSet.getString("displayname")
        )
    }

    fun isUserInChat(chatId: Long, userId: Long): Boolean {
        val connection = GdConnectionFactory.getConnection()
        val statement =
            connection.prepareStatement("SELECT EXISTS(SELECT 1 FROM users_in_chats WHERE chat_id = ? AND user_id = ?)")
                .apply {
                    setLong(1, chatId)
                    setLong(2, userId)
                }
        if (!statement.execute())
            return false
        val resultSet = statement.resultSet
        if (!resultSet.next())
            return false
        return resultSet.getBoolean(1)
    }

    fun addUser(user: GdUser) {
        val connection = GdConnectionFactory.getConnection()
        val statement =
            connection.prepareStatement("INSERT INTO users values (?,?) ON CONFLICT DO NOTHING").apply {
                setLong(1, user.id)
                setString(2, user.displayName)
            }
        statement.executeUpdate()
    }

    fun addUserToChat(chatId: Long, userId: Long) {
        val connection = GdConnectionFactory.getConnection()
        val statement =
            connection.prepareStatement("INSERT INTO users_in_chats (user_id, chat_id) VALUES (?,?) ON CONFLICT DO NOTHING")
                .apply {
                    setLong(1, userId)
                    setLong(2, chatId)
                }
        statement.executeUpdate()
    }

    fun getUserIdsInChat(chatId: Long): Set<Long> {
        val connection = GdConnectionFactory.getConnection()
        val statement =
            connection.prepareStatement("SELECT user_id FROM users_in_chats WHERE chat_id = ?")
                .apply {
                    setLong(1, chatId)
                }
        val userIds = HashSet<Long>()
        if (!statement.execute())
            return userIds
        val resultSet = statement.resultSet
        while (resultSet.next()) {
            userIds.add(resultSet.getLong("user_id"))
        }
        return userIds
    }

    fun getGayInChat(chatId: Long): GdUser? {
        val connection = GdConnectionFactory.getConnection()
        val statement =
            connection.prepareStatement("SELECT * FROM users JOIN chats ON chats.gayid = users.userid WHERE chatid=?")
                .apply {
                    setLong(1, chatId)
                }
        if (!statement.execute())
            return null
        val resultSet = statement.resultSet
        if (!resultSet.next())
            return null
        return GdUser(
            resultSet.getLong("userid"),
            resultSet.getString("displayname")
        )
    }

    fun updateGayInChat(playTime: Timestamp, chatId: Long, gayId: Long) {
        val connection = GdConnectionFactory.getConnection()
        val statement =
            connection.prepareStatement("UPDATE chats SET gayid=?, lasttime=? WHERE chatid=?")
                .apply {
                    setLong(1, gayId)
                    setTimestamp(2, playTime)
                    setLong(3, chatId)
                }
        statement.executeUpdate()
    }

    // dick

    fun getDick(chatId: Long, userId: Long): Pair<Timestamp, Int>? {
        val connection = GdConnectionFactory.getConnection()
        val result = connection.createStatement()
            .executeQuery("SELECT measuretime,dick FROM dicks WHERE chatid=$chatId AND userid=$userId")
        if (!result.next())
            return null
        return result.getTimestamp(1) to result.getInt(2)
    }

    fun updateDick(chatId: Long, userId: Long, playTime: Timestamp, dick: Int) {
        val connection = GdConnectionFactory.getConnection()
        val statement =
            connection.prepareStatement(
                "INSERT INTO dicks(chatid, userid, measuretime, dick) VALUES (?,?,?,?)" +
                        " ON CONFLICT (chatid,userid) DO UPDATE SET dick=excluded.dick,measuretime=excluded.measuretime"
            )
                .apply {
                    setLong(1, chatId)
                    setLong(2, userId)
                    setTimestamp(3, playTime)
                    setInt(4, dick)
                }
        statement.executeUpdate()
    }
}