package ru.hukutoc2288.howtoshitbot.dao

import org.telegram.telegrambots.meta.api.objects.User
import ru.hukutoc2288.howtoshitbot.entinies.dick.DickTop
import ru.hukutoc2288.howtoshitbot.entinies.gayofday.GdChat
import ru.hukutoc2288.howtoshitbot.entinies.gayofday.GdUser
import ru.hukutoc2288.howtoshitbot.entinies.stories.Story
import ru.hukutoc2288.howtoshitbot.utils.GdConnectionFactory
import ru.hukutoc2288.howtoshitbot.utils.displayName
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Timestamp
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

object GdDao {

    private var aneksCount = 0

    // TODO: 02.12.2022 так как теперь эта база не только для пидора дня, нужно поменять все названия
    fun getChatById(chatId: Long): GdChat? {
        var connection: Connection? = null
        var statement: Statement? = null
        var resultSet: ResultSet? = null
        try {
            connection = GdConnectionFactory.getConnection()
            statement = connection.prepareStatement("SELECT * FROM chats WHERE chatid = ?").apply {
                setLong(1, chatId)
            }
            if (!statement.execute())
                return null
            resultSet = statement.resultSet
            if (!resultSet.next())
                return null
            return GdChat(
                resultSet.getLong("chatid"),
                resultSet.getLong("gayid"),
                resultSet.getTimestamp("lasttime")
            )
        } finally {
            resultSet?.close()
            statement?.close()
            connection?.close()
        }

    }

    // TODO: у меня был прикольный итератор для этого в другом проекте, надо его перенести сюда
    fun getAllChatIds(): List<Long>? {
        var connection: Connection? = null
        var statement: Statement? = null
        var resultSet: ResultSet? = null
        try {
            connection = GdConnectionFactory.getConnection()
            statement = connection.prepareStatement("SELECT chatid FROM chats")
            if (!statement.execute())
                return null
            resultSet = statement.resultSet
            val list = ArrayList<Long>()
            while (resultSet.next()) {
                list.add(resultSet.getLong(1))
            }
            return list
        } finally {
            resultSet?.close()
            statement?.close()
            connection?.close()
        }
    }

    fun addChat(chatId: Long) {
        var connection: Connection? = null
        var statement: Statement? = null
        try {
            connection = GdConnectionFactory.getConnection()
            statement =
                connection.prepareStatement("INSERT INTO chats (chatid) VALUES (?) ON CONFLICT DO NOTHING").apply {
                    setLong(1, chatId)
                }
            statement.executeUpdate()
            connection.commit()
        } finally {
            statement?.close()
            connection?.close()
        }
    }

    fun getUserById(userId: Long): GdUser? {
        var connection: Connection? = null
        var statement: Statement? = null
        var resultSet: ResultSet? = null
        try {
            connection = GdConnectionFactory.getConnection()
            statement = connection.prepareStatement("SELECT * FROM users WHERE userid = ?").apply {
                setLong(1, userId)
            }
            if (!statement.execute())
                return null
            resultSet = statement.resultSet
            if (!resultSet.next())
                return null
            return GdUser(
                resultSet.getLong("userid"),
                resultSet.getString("displayname")
            )
        } finally {
            resultSet?.close()
            statement?.close()
            connection?.close()
        }
    }

    fun isUserInChat(chatId: Long, userId: Long): Boolean {
        var connection: Connection? = null
        var statement: Statement? = null
        var resultSet: ResultSet? = null
        try {
            connection = GdConnectionFactory.getConnection()
            statement =
                connection.prepareStatement("SELECT EXISTS(SELECT 1 FROM users_in_chats WHERE chat_id = ? AND user_id = ?)")
                    .apply {
                        setLong(1, chatId)
                        setLong(2, userId)
                    }
            if (!statement.execute())
                return false
            resultSet = statement.resultSet
            if (!resultSet.next())
                return false
            return resultSet.getBoolean(1)
        } finally {
            resultSet?.close()
            statement?.close()
            connection?.close()
        }
    }

    fun addUserToChat(chatId: Long, userId: Long) {
        var connection: Connection? = null
        var statement: Statement? = null
        try {
            connection = GdConnectionFactory.getConnection()
            statement =
                connection.prepareStatement("INSERT INTO users_in_chats (user_id, chat_id) VALUES (?,?) ON CONFLICT DO NOTHING")
                    .apply {
                        setLong(1, userId)
                        setLong(2, chatId)
                    }
            statement.executeUpdate()
            connection.commit()
        } finally {
            statement?.close()
            connection?.close()
        }
    }

    fun getUserIdsInChat(chatId: Long): Set<Long> {
        var connection: Connection? = null
        var statement: Statement? = null
        var resultSet: ResultSet? = null
        try {
            connection = GdConnectionFactory.getConnection()
            statement = connection.prepareStatement("SELECT user_id FROM users_in_chats WHERE chat_id = ?").apply {
                setLong(1, chatId)
            }
            val userIds = HashSet<Long>()
            if (!statement.execute())
                return userIds
            resultSet = statement.resultSet
            while (resultSet.next()) {
                userIds.add(resultSet.getLong("user_id"))
            }
            return userIds
        } finally {
            resultSet?.close()
            statement?.close()
            connection?.close()
        }
    }

    fun getGayInChat(chatId: Long): GdUser? {
        var connection: Connection? = null
        var statement: Statement? = null
        var resultSet: ResultSet? = null
        try {
            connection = GdConnectionFactory.getConnection()
            statement =
                connection.prepareStatement("SELECT * FROM users JOIN chats ON chats.gayid = users.userid WHERE chatid=?")
                    .apply {
                        setLong(1, chatId)
                    }
            if (!statement.execute())
                return null
            resultSet = statement.resultSet
            if (!resultSet.next())
                return null
            return GdUser(
                resultSet.getLong("userid"),
                resultSet.getString("displayname")
            )
        } finally {
            resultSet?.close()
            statement?.close()
            connection?.close()
        }
    }

    fun updateGayInChat(playTime: Timestamp, chatId: Long, gayId: Long) {

        var connection: Connection? = null
        var statement: Statement? = null
        try {
            connection = GdConnectionFactory.getConnection()
            statement =
                connection.prepareStatement("UPDATE chats SET gayid=?, lasttime=? WHERE chatid=?")
                    .apply {
                        setLong(1, gayId)
                        setTimestamp(2, playTime)
                        setLong(3, chatId)
                    }
            statement.executeUpdate()
        } finally {
            statement?.close()
            connection?.close()
        }
    }

    // dick

    fun updateUserName(user: User) {
        var connection: Connection? = null
        var statement: Statement? = null
        try {
            connection = GdConnectionFactory.getConnection()
            statement = connection.prepareStatement(
                "INSERT INTO users(userid,displayname) VALUES (?,?)" +
                        " ON CONFLICT (userid) DO UPDATE SET displayname=excluded.displayname"
            )
                .apply {
                    setLong(1, user.id)
                    setString(2, user.displayName)
                }
            statement.executeUpdate()
            connection.commit()
        } finally {
            statement?.close()
            connection?.close()
        }
    }

    fun getDick(chatId: Long, user: User): Pair<Timestamp, Int>? {
        updateUserName(user)
        var connection: Connection? = null
        var statement: Statement? = null
        var resultSet: ResultSet? = null
        try {
            connection = GdConnectionFactory.getConnection()
            statement = connection.createStatement()
            resultSet =
                statement.executeQuery("SELECT measuretime,dick FROM dicks WHERE chatid=$chatId AND userid=${user.id}")
            if (!resultSet.next())
                return null
            return resultSet.getTimestamp(1) to resultSet.getInt(2)
        } finally {
            resultSet?.close()
            statement?.close()
            connection?.close()
        }
    }

    fun updateDick(chatId: Long, user: User, playTime: Timestamp, dick: Int) {
        updateUserName(user)
        var connection: Connection? = null
        var statement: Statement? = null
        try {
            connection = GdConnectionFactory.getConnection()
            statement =
                connection.prepareStatement(
                    "INSERT INTO dicks(chatid, userid, measuretime, dick) VALUES (?,?,?,?)" +
                            " ON CONFLICT (chatid,userid) DO UPDATE" +
                            " SET dick=excluded.dick,measuretime=excluded.measuretime"
                )
                    .apply {
                        setLong(1, chatId)
                        setLong(2, user.id)
                        setTimestamp(3, playTime)
                        setInt(4, dick)
                    }
            statement.executeUpdate()
            connection.commit()
        } finally {
            statement?.close()
            connection?.close()
        }
    }

    fun getAverageDick(chatId: Long): Int? {
        var connection: Connection? = null
        var statement: Statement? = null
        var resultSet: ResultSet? = null
        try {
            connection = GdConnectionFactory.getConnection()
            statement = connection.createStatement()
            resultSet = statement.executeQuery("select avg(dick) from dicks where chatid=$chatId")
            if (!resultSet.next())
                return null
            val averageDick = resultSet.getDouble(1).toInt()
            // если игроков ещё нет, то и среднего песюна нет
            return if (resultSet.wasNull()) null else averageDick
        } finally {
            resultSet?.close()
            statement?.close()
            connection?.close()
        }
    }

    fun getDickTop(chatId: Long, user: User): List<DickTop> {
        updateUserName(user)
        var connection: Connection? = null
        var statement: Statement? = null
        var resultSet: ResultSet? = null
        try {
            connection = GdConnectionFactory.getConnection()
            statement = connection.createStatement()
            resultSet = statement.executeQuery(
                "select dick,dicks.userid,displayname from dicks" +
                        " join users on dicks.userid = users.userid" +
                        " where chatid = $chatId order by dick desc "
            )
            var foundMe = false
            var currentPlace = 0
            var dickOnPlace = 0
            val dickTop = ArrayList<DickTop>()
            while (resultSet.next() && (currentPlace <= 10 || !foundMe)) {
                val dick = resultSet.getInt(1)
                if (dick != dickOnPlace) {
                    dickOnPlace = dick
                    currentPlace++
                }
                val userId = resultSet.getLong(2)
                val displayName = resultSet.getString(3)
                if (userId == user.id) {
                    // в любом случае добавляем себя
                    foundMe = true
                    dickTop.add(DickTop(displayName, dick, currentPlace, true))
                } else if (currentPlace <= 10) {
                    // добиваем до 10 мест
                    dickTop.add(DickTop(displayName, dick, currentPlace))
                }
            }
            return dickTop
        } finally {
            resultSet?.close()
            statement?.close()
            connection?.close()
        }
    }

    fun prepareAneksCache(minRating: Int) {
        var connection: Connection? = null
        var statement: Statement? = null
        var resultSet: ResultSet? = null
        try {
            connection = GdConnectionFactory.getConnection()
            statement = connection.createStatement()
            statement.execute("DROP TABLE IF EXISTS  desired_aneks_ids")
            statement.execute("CREATE TABLE desired_aneks_ids (id INT, index SERIAL)")
            statement.execute("INSERT INTO desired_aneks_ids(id) SELECT id FROM aneks WHERE rating>=$minRating")
            statement.execute("ALTER TABLE desired_aneks_ids ADD PRIMARY KEY (id)")
            statement.execute("CREATE INDEX desired_aneks_index_index ON desired_aneks_ids(index)")
            connection.commit()
            // get total aneks count
            resultSet = statement.executeQuery("SELECT count(*) FROM desired_aneks_ids")
            resultSet.next()
            aneksCount = resultSet.getInt(1)
        } finally {
            resultSet?.close()
            statement?.close()
            connection?.close()
        }
    }

    fun getRandomAnek(): Pair<Int, String>? {
        var connection: Connection? = null
        var statement: Statement? = null
        var resultSet: ResultSet? = null
        try {
            connection = GdConnectionFactory.getConnection()
            val randomAnekIndex = (1..aneksCount).random()
            statement = connection.createStatement()
            resultSet = statement.executeQuery(
                "select id,text from aneks where id = (select id from desired_aneks_ids where index = $randomAnekIndex);"
            )
            if (!resultSet.next())
                return null
            return resultSet.getInt(1) to resultSet.getString(2)
        } finally {
            resultSet?.close()
            statement?.close()
            connection?.close()
        }
    }

    // stories

    fun getStoriesInChat(chatId: Long): List<Story> {
        var connection: Connection? = null
        var statement: Statement? = null
        var resultSet: ResultSet? = null
        try {
            connection = GdConnectionFactory.getConnection()
            statement = connection.createStatement()
            resultSet = statement.executeQuery(
                "SELECT chatId,stories.userId,expireAfter,message,displayName FROM stories" +
                        " JOIN users ON stories.userId=users.userId WHERE chatId=$chatId"
            )
            val storiesList = ArrayList<Story>()
            while (resultSet.next()) {
                storiesList.add(
                    Story(
                        chatId = resultSet.getLong("chatId"),
                        user = GdUser(
                            id = resultSet.getLong("userId"),
                            displayName = resultSet.getString("displayName")
                        ),
                        expireAfter = resultSet.getTimestamp("expireAfter"),
                        message = resultSet.getString("message")
                    )
                )
            }
            return storiesList
        } finally {
            resultSet?.close()
            statement?.close()
            connection?.close()
        }
    }
}