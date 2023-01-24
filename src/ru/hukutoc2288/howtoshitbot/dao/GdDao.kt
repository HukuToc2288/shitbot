package ru.hukutoc2288.howtoshitbot.dao

import org.telegram.telegrambots.meta.api.objects.User
import ru.hukutoc2288.howtoshitbot.entinies.dick.DickTop
import ru.hukutoc2288.howtoshitbot.entinies.gayofday.GdChat
import ru.hukutoc2288.howtoshitbot.entinies.gayofday.GdUser
import ru.hukutoc2288.howtoshitbot.utils.GdConnectionFactory
import ru.hukutoc2288.howtoshitbot.utils.displayName
import java.io.Closeable
import java.sql.Timestamp
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

object GdDao {
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

    fun updateUserName(user: User) {
        val connection = GdConnectionFactory.getConnection()
        val statement = connection.prepareStatement(
            "INSERT INTO users(userid,displayname) VALUES (?,?)" +
                    " ON CONFLICT (userid) DO UPDATE SET displayname=excluded.displayname"
        )
            .apply {
                setLong(1, user.id)
                setString(2, user.displayName)
            }
    }

    fun getDick(chatId: Long, user: User): Pair<Timestamp, Int>? {
        updateUserName(user)
        val connection = GdConnectionFactory.getConnection()
        val result = connection.createStatement()
            .executeQuery("SELECT measuretime,dick FROM dicks WHERE chatid=$chatId AND userid=${user.id}")
        if (!result.next())
            return null
        return result.getTimestamp(1) to result.getInt(2)
    }

    fun updateDick(chatId: Long, user: User, playTime: Timestamp, dick: Int) {
        updateUserName(user)
        val connection = GdConnectionFactory.getConnection()
        val statement =
            connection.prepareStatement(
                "INSERT INTO dicks(chatid, userid, measuretime, dick) VALUES (?,?,?,?)" +
                        " ON CONFLICT (chatid,userid) DO UPDATE SET dick=excluded.dick,measuretime=excluded.measuretime"
            )
                .apply {
                    setLong(1, chatId)
                    setLong(2, user.id)
                    setTimestamp(3, playTime)
                    setInt(4, dick)
                }
        statement.executeUpdate()
    }

    fun getAverageDick(chatId: Long): Int? {
        val connection = GdConnectionFactory.getConnection()
        val result = connection.createStatement().executeQuery("select avg(dick) from dicks where chatid=$chatId")
        if (!result.next())
            return null
        val averageDick = result.getDouble(1).toInt()
        // если игроков ещё нет, то и среднего песюна нет
        return if (result.wasNull()) null else averageDick
    }

    fun getDickTop(chatId: Long, user: User): List<DickTop> {
        updateUserName(user)
        val connection = GdConnectionFactory.getConnection()
        val result = connection.createStatement()
            .executeQuery(
                "select dick,dicks.userid,displayname from dicks" +
                        " join users on dicks.userid = users.userid" +
                        " where chatid = $chatId order by dick desc "
            )
        var foundMe = false
        var currentPlace = 0
        var dickOnPlace = 0
        val dickTop = ArrayList<DickTop>()
        while (result.next() && (currentPlace < 10 || !foundMe)) {
            val dick = result.getInt(1)
            if (dick != dickOnPlace) {
                dickOnPlace = dick
                currentPlace++
            }
            val userId = result.getLong(2)
            val displayName = result.getString(3)
            if (userId == user.id) {
                // в любом случае добавляем себя
                foundMe = true
                dickTop.add(DickTop(displayName, dick, currentPlace, true))
            } else if (currentPlace < 10) {
                // добиваем до 10 мест
                dickTop.add(DickTop(displayName, dick, currentPlace))
            }
        }
        return dickTop
    }

    fun getRandomAnek(minRating: Int): Pair<Int, String>? {
        val resources = ArrayList<AutoCloseable>()
        try {
            val connection = GdConnectionFactory.getConnection().apply { resources.add(this) }
            val anekIds = ArrayList<Int>()
            val idsResult = connection.createStatement()
                .executeQuery("select id from aneks where rating>=$minRating")
                .apply { resources.add(this) }
            while (idsResult.next())
                anekIds.add(idsResult.getInt(1))
            val randomAnekId = anekIds.random()
            val result = connection.createStatement().executeQuery(
                "select id,text from aneks where id=$randomAnekId limit 1"
            ).apply { resources.add(this) }
            if (!result.next())
                return null
            return result.getInt(1) to result.getString(2)
        } finally {
            closeResources(resources)
        }
    }

    private fun closeResources(resources: Collection<AutoCloseable>) {
        for (c in resources) {
            try {
                c.close()
            } catch (_: Exception) {
                // pass
            }
        }
    }
}