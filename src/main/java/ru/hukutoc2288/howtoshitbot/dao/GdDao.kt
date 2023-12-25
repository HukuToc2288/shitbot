package ru.hukutoc2288.howtoshitbot.dao

import org.telegram.telegrambots.meta.api.objects.User
import ru.hukutoc2288.howtoshitbot.entinies.dick.DickTop
import ru.hukutoc2288.howtoshitbot.entinies.gayofday.GdChat
import ru.hukutoc2288.howtoshitbot.entinies.gayofday.GdUser
import ru.hukutoc2288.howtoshitbot.entinies.stories.Story
import ru.hukutoc2288.howtoshitbot.utils.GdConnectionFactory
import ru.hukutoc2288.howtoshitbot.utils.displayName
import java.sql.Connection
import java.sql.Date
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.util.SortedMap
import java.util.TreeMap
import kotlin.collections.ArrayList
import kotlin.collections.HashSet
import ru.hukutoc2288.howtoshitbot.entinies.KnbHistoryPack
import ru.hukutoc2288.howtoshitbot.entinies.dick.DisplayGender
import ru.hukutoc2288.howtoshitbot.entinies.dick.Gender
import ru.hukutoc2288.howtoshitbot.entinies.words.ChatUserDate
import ru.hukutoc2288.howtoshitbot.entinies.words.WordsInfo

object GdDao {

    private const val maxKnbHistoryPack = 20
    private var aneksCount = 0

    private val pendingWordsData = HashMap<ChatUserDate, WordsInfo>()
    private var lastWordsStoreTime = System.currentTimeMillis()
    private var currentWordsDataUpdates = 0

    private const val MAX_WORDS_DATA_UPDATES = 20
    private const val MAX_WORDS_DATA_LIFETIME = 30_000L

    init {
        DbHelper.init()
    }

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
            val dick = resultSet.getInt(2)
            if (dick == 0) {
                return null
            }
            return resultSet.getTimestamp(1) to dick
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
            resultSet = statement.executeQuery("select avg(dick) from dicks where chatid=$chatId and dick>0")
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

    fun getUnDickDate(chatId: Long, user: User): Pair<Int, LocalDate> {
        updateUserName(user)
        var connection: Connection? = null
        var statement: Statement? = null
        var resultSet: ResultSet? = null
        try {
            connection = GdConnectionFactory.getConnection()
            statement = connection.createStatement()
            resultSet =
                statement.executeQuery("SELECT dick,last_undick_time FROM dicks WHERE chatid=$chatId AND userid=${user.id}")
            if (!resultSet.next())
                return 0 to LocalDate.EPOCH
            val dick = resultSet.getInt(1)
            return dick to resultSet.getDate(2).toLocalDate()
        } finally {
            resultSet?.close()
            statement?.close()
            connection?.close()
        }
    }

    fun unDick(chatId: Long, user: User, date: LocalDate) {
        var connection: Connection? = null
        var statement: Statement? = null
        try {
            connection = GdConnectionFactory.getConnection()
            statement =
                connection.prepareStatement("UPDATE dicks SET dick=0, last_undick_time=? WHERE chatid=? AND userid=?")
                    .apply {
                        setDate(1, Date.valueOf(date))
                        setLong(2, chatId)
                        setLong(3, user.id)
                    }
            statement.executeUpdate()
            connection.commit()
        } finally {
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
                if (dick == 0) {
                    // removed players that still kept in DB
                    continue
                }
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

    fun getSingleStory(chatId: Long, user: User): Story? {
        var connection: Connection? = null
        var statement: Statement? = null
        var resultSet: ResultSet? = null
        try {
            connection = GdConnectionFactory.getConnection()
            statement = connection.createStatement()
            resultSet = statement.executeQuery(
                "SELECT chatId,stories.userId,expireAfter,message,displayName FROM stories" +
                        " JOIN users ON stories.userId=users.userId WHERE chatId=$chatId AND stories.userId=${user.id}"
            )
            if (!resultSet.next())
                return null
            return Story(
                chatId = resultSet.getLong("chatId"),
                user = GdUser(
                    id = resultSet.getLong("userId"),
                    displayName = resultSet.getString("displayName")
                ),
                expireAfter = resultSet.getTimestamp("expireAfter"),
                message = resultSet.getString("message")
            )
        } finally {
            resultSet?.close()
            statement?.close()
            connection?.close()
        }
    }

    fun createOrUpdateStory(chatId: Long, user: User, expireAfter: Timestamp, message: String) {
        updateUserName(user)
        var connection: Connection? = null
        var statement: Statement? = null
        try {
            connection = GdConnectionFactory.getConnection()
            statement =
                connection.prepareStatement(
                    "INSERT INTO stories(chatId,userId,expireAfter,message) VALUES (?,?,?,?)" +
                            " ON CONFLICT (chatId,userId) DO UPDATE SET" +
                            " expireAfter=excluded.expireAfter, message=excluded.message"
                )
                    .apply {
                        setLong(1, chatId)
                        setLong(2, user.id)
                        setTimestamp(3, expireAfter)
                        setString(4, message)
                    }
            statement.executeUpdate()
            connection.commit()
        } finally {
            statement?.close()
            connection?.close()
        }
    }

    // 1>2
    fun getKnbHistory(chatId: Long, biggestUserId: Long, smallestUserId: Long): SortedMap<Timestamp, KnbHistoryPack> {
        assert(biggestUserId > smallestUserId)
        var connection: Connection? = null
        var statement: Statement? = null
        var resultSet: ResultSet? = null
        try {
            connection = GdConnectionFactory.getConnection()
            statement = connection.createStatement()
            resultSet = statement.executeQuery(
                "SELECT time,games,result FROM knb_results WHERE chat_id=$chatId AND biggest_user_id=$biggestUserId AND smallest_user_id=$smallestUserId"
            )
            val resultPacks = TreeMap<Timestamp, KnbHistoryPack>()
            while (resultSet.next()) {
                resultPacks[resultSet.getTimestamp("time")] = KnbHistoryPack(
                    resultSet.getInt("games"),
                    resultSet.getInt("result")
                )
            }
            return resultPacks
        } finally {
            resultSet?.close()
            statement?.close()
            connection?.close()
        }
    }

    fun clearKnbHistory(chatId: Long, biggestUserId: Long, smallestUserId: Long) {
        assert(biggestUserId > smallestUserId)
        var connection: Connection? = null
        var statement: Statement? = null

        try {
            connection = GdConnectionFactory.getConnection()
            statement =
                connection.prepareStatement(
                    "DELETE FROM knb_results WHERE chat_id=$chatId AND biggest_user_id=$biggestUserId AND smallest_user_id=$smallestUserId"
                )
            statement.executeUpdate()
            connection.commit()
        } finally {
            statement?.close()
            connection?.close()
        }
    }

    fun getLastKnbPack(chatId: Long, biggestUserId: Long, smallestUserId: Long): Pair<Timestamp, KnbHistoryPack>? {
        assert(biggestUserId > smallestUserId)
        var connection: Connection? = null
        var statement: Statement? = null
        var resultSet: ResultSet? = null
        try {
            connection = GdConnectionFactory.getConnection()
            statement = connection.createStatement()
            resultSet = statement.executeQuery(
                "SELECT time,games,result FROM knb_results" +
                        " WHERE chat_id=$chatId AND biggest_user_id=$biggestUserId AND smallest_user_id=$smallestUserId ORDER BY time LIMIT 1"
            )
            if (!resultSet.next()) {
                return null
            }
            return resultSet.getTimestamp(1) to KnbHistoryPack(resultSet.getInt(2), resultSet.getInt(3))
        } finally {
            resultSet?.close()
            statement?.close()
            connection?.close()
        }
    }

    fun storeKnbRound(chatId: Long, biggestUserId: Long, smallestUserId: Long, result: Int) {
        assert(biggestUserId > smallestUserId)
        assert(result in -1..1)
        var connection: Connection? = null
        var statement: Statement? = null

        try {
            val lastKnbPackData = getLastKnbPack(chatId, biggestUserId, smallestUserId)

            val knbPackInsertTimestamp: Timestamp
            val knbPackInsertGames: Int
            val knbPackInsertResult: Int

            if (lastKnbPackData == null || lastKnbPackData.second.currentGames == maxKnbHistoryPack) {
                // создаём новую пачку, если старая заполнена или её вообще нет

                knbPackInsertTimestamp = Timestamp.from(Instant.now())
                knbPackInsertGames = 1
                knbPackInsertResult = result
            } else {
                // будем добавлять игру в существующую
                val currentPackTimestamp = lastKnbPackData.first
                val lastKnbPack = lastKnbPackData.second

                knbPackInsertTimestamp = currentPackTimestamp
                knbPackInsertGames = lastKnbPack.currentGames + 1
                knbPackInsertResult = lastKnbPack.result + result
            }
            connection = GdConnectionFactory.getConnection()
            statement =
                connection.prepareStatement(
                    "INSERT INTO knb_results(chat_id, biggest_user_id, smallest_user_id, time, games, result) VALUES (?,?,?,?,?,?)" +
                            " ON CONFLICT (chat_id, biggest_user_id, smallest_user_id, time) DO UPDATE" +
                            " SET games=excluded.games, result=excluded.result"
                ).apply {
                    setLong(1, chatId)
                    setLong(2, biggestUserId)
                    setLong(3, smallestUserId)
                    setTimestamp(4, knbPackInsertTimestamp)
                    setInt(5, knbPackInsertGames)
                    setInt(6, knbPackInsertResult)
                }
            statement.executeUpdate()
            connection.commit()
        } finally {
            statement?.close()
            connection?.close()
        }
    }

    fun getDisplayGenders(): List<DisplayGender> {
        var connection: Connection? = null
        var statement: Statement? = null
        var resultSet: ResultSet? = null
        try {
            connection = GdConnectionFactory.getConnection()
            statement = connection.createStatement()
            resultSet =
                statement.executeQuery("SELECT id,name FROM genders ORDER BY id")
            val genders = ArrayList<DisplayGender>()
            while (resultSet.next()) {
                genders.add(
                    DisplayGender(
                        resultSet.getInt("id"),
                        resultSet.getString("name")
                    )
                )
            }
            return genders
        } finally {
            resultSet?.close()
            statement?.close()
            connection?.close()
        }
    }

    fun getGenderName(chatId: Long, user: User): String {
        var connection: Connection?
        var statement: Statement?
        var resultSet: ResultSet?
        val ac = AutoClose()
        try {
            connection = GdConnectionFactory.getConnection().autoClose(ac)
            statement = connection.createStatement().autoClose(ac)
            val query = "SELECT genders.name FROM users_genders" +
                    " INNER JOIN genders ON users_genders.gender_id = genders.id" +
                    " where chat_id=$chatId and user_id=${user.id}"
            resultSet = statement.executeQuery(query).autoClose(ac)
            if (resultSet.next()) {
                return resultSet.getString(1)
            }
            // update gender and go retry
            ac.closeResources()

            setDefaultGender(chatId, user.id)

            connection = GdConnectionFactory.getConnection().autoClose(ac)
            statement = connection.createStatement().autoClose(ac)
            resultSet = statement.executeQuery(query).autoClose(ac)
            if (!resultSet.next()) {
                throw NullPointerException("Не удалось получить гендер!")
            }
            return resultSet.getString(1)
        } finally {
            ac.closeResources()
        }
    }

    private fun setDefaultGender(chatId: Long, userId: Long) {
        val connection: Connection?
        val statement: Statement?
        val ac = AutoClose()
        try {
            connection = GdConnectionFactory.getConnection().autoClose(ac)
            statement = connection.createStatement().autoClose(ac)
            statement.execute("INSERT INTO users_genders(chat_id,user_id,gender_id) VALUES ($chatId,$userId,0) ON CONFLICT DO NOTHING")
            connection.commit()
        } finally {
            ac.closeResources()
        }
    }

    fun getGender(chatId: Long, userId: Long): Gender {
        var connection: Connection?
        var statement: Statement?
        var resultSet: ResultSet?
        val ac = AutoClose()
        try {
            connection = GdConnectionFactory.getConnection().autoClose(ac)
            statement = connection.createStatement().autoClose(ac)
            val query = "SELECT genders.id,genders.change_text,genders.info_text,genders.thing FROM users_genders" +
                    " INNER JOIN genders ON users_genders.gender_id = genders.id" +
                    " where chat_id=$chatId and user_id=${userId}"
            resultSet = statement.executeQuery(query).autoClose(ac)
            if (resultSet.next()) {
                return Gender(
                    resultSet.getInt(1),
                    resultSet.getString(2),
                    resultSet.getString(3),
                    resultSet.getString(4)
                )
            }
            // update gender and go retry
            ac.closeResources()

            setDefaultGender(chatId, userId)

            connection = GdConnectionFactory.getConnection().autoClose(ac)
            statement = connection.createStatement().autoClose(ac)
            resultSet = statement.executeQuery(query).autoClose(ac)
            if (!resultSet.next()) {
                throw NullPointerException("Не удалось получить гендер!")
            }
            return Gender(
                resultSet.getInt(1),
                resultSet.getString(2),
                resultSet.getString(3),
                resultSet.getString(4)
            )
        } finally {
            ac.closeResources()
        }
    }

    fun setGender(chatId: Long, userId: Long, genderId: Int): Boolean {
        val connection: Connection?
        val statement: Statement?
        val ac = AutoClose()
        try {
            connection = GdConnectionFactory.getConnection().autoClose(ac)
            statement = connection.createStatement().autoClose(ac)
            val result = statement.executeUpdate(
                "INSERT INTO users_genders(chat_id,user_id,gender_id) VALUES ($chatId,$userId,$genderId)" +
                        " ON CONFLICT (chat_id,user_id) DO UPDATE SET gender_id = excluded.gender_id"
            )
            connection.commit()
            return result > 0
        } finally {
            ac.closeResources()
        }
    }

    fun updateWordsCount(chatId: Long, userId: Long, date: LocalDate, wordsInfo: WordsInfo) {
        val wordsDataKey = ChatUserDate(chatId, userId, date)
        val existingData = pendingWordsData[wordsDataKey]
        if (existingData != null) {
            existingData.wordsCount += wordsInfo.wordsCount
            existingData.profanityCount += wordsInfo.profanityCount
        } else {
            pendingWordsData[wordsDataKey] = wordsInfo
        }
        currentWordsDataUpdates++
        val timeNow = System.currentTimeMillis()
        if (timeNow > lastWordsStoreTime + MAX_WORDS_DATA_LIFETIME || currentWordsDataUpdates > MAX_WORDS_DATA_UPDATES) {
            // push data to database
            flushWordsData()
        }
    }

    fun flushWordsData() {
        storeWordsDataInternal(pendingWordsData)
        pendingWordsData.clear()
        lastWordsStoreTime = System.currentTimeMillis()
        currentWordsDataUpdates = 0
    }

    private fun storeWordsDataInternal(wordsData: Map<ChatUserDate, WordsInfo>) {
        val connection: Connection?
        val statement: PreparedStatement?
        val ac = AutoClose()
        try {
            connection = GdConnectionFactory.getConnection().autoClose(ac)
            statement = connection.prepareStatement(
                "INSERT INTO words_daily" +
                        " (chat_id,user_id,info_date,total_words,profanity_words) VALUES (?,?,?,?,?)" +
                        " ON CONFLICT(chat_id,user_id,info_date) DO UPDATE SET" +
                        " total_words=words_daily.total_words+excluded.total_words," +
                        " profanity_words=words_daily.profanity_words+excluded.profanity_words"
            ).autoClose(ac)
            wordsData.forEach { (key, value) ->
                statement.setLong(1, key.chatId)
                statement.setLong(2, key.userId)
                statement.setDate(3, Date.valueOf(key.date))
                statement.setInt(4, value.wordsCount)
                statement.setInt(5, value.profanityCount)
                statement.addBatch()
            }
            statement.executeBatch()
            connection.commit()
        } finally {
            ac.closeResources()
        }
    }

    fun getWordsStats(chatId: Long, userId: Long, maxDays: Long): Map<LocalDate, WordsInfo> {
        var connection: Connection? = null
        var statement: PreparedStatement? = null
        var resultSet: ResultSet? = null
        try {
            connection = GdConnectionFactory.getConnection()
            val minDate = LocalDate.now().minusDays(maxDays)
            statement = connection.prepareStatement(
                "SELECT info_date,total_words,profanity_words FROM words_daily" +
                        " WHERE chat_id=$chatId AND user_id=$userId AND info_date >= ?"
            )
            statement.setDate(1, Date.valueOf(minDate))
            if (!statement.execute())
                return emptyMap()
            resultSet = statement.resultSet
            val map = HashMap<LocalDate, WordsInfo>()
            while (resultSet.next()) {
                map[resultSet.getDate(1).toLocalDate()] = WordsInfo(resultSet.getInt(2), resultSet.getInt(3))
            }
            return map
        } finally {
            resultSet?.close()
            statement?.close()
            connection?.close()
        }
    }
}
