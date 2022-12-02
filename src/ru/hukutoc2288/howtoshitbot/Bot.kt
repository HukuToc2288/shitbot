package ru.hukutoc2288.howtoshitbot

import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import ru.hukutoc2288.howtoshitbot.api.CurrenciesApi
import ru.hukutoc2288.howtoshitbot.entinies.currencies.CurrenciesList
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.schedule
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import org.telegram.telegrambots.meta.api.methods.send.SendSticker
import org.telegram.telegrambots.meta.api.objects.User
import ru.hukutoc2288.howtoshitbot.dao.GdDao
import ru.hukutoc2288.howtoshitbot.entinies.uptime.UptimeResponse
import ru.hukutoc2288.howtoshitbot.utils.*
import ru.hukutoc2288.howtoshitbot.utils.StringUtils.dedupe
import java.lang.StringBuilder
import java.net.InetAddress
import java.net.Socket
import java.sql.Timestamp
import java.text.SimpleDateFormat
import kotlin.collections.HashMap
import java.util.Calendar
import kotlin.math.max
import kotlin.math.min
import kotlin.system.exitProcess


val mapper: ObjectMapper = ObjectMapper().registerModule(
    KotlinModule.Builder()
        .withReflectionCacheSize(512)
        .configure(KotlinFeature.NullToEmptyCollection, false)
        .configure(KotlinFeature.NullToEmptyMap, false)
        .configure(KotlinFeature.NullIsSameAsDefault, false)
        .configure(KotlinFeature.SingletonSupport, false)
        .configure(KotlinFeature.StrictNullChecks, false)
        .build()
)

val gdDao = GdDao()

class Bot : TelegramLongPollingBot() {

    val currenciesApi =
        Retrofit.Builder().baseUrl("https://www.cbr-xml-daily.ru/")
            .addConverterFactory(JacksonConverterFactory.create())
            .build().create(CurrenciesApi::class.java)

    private val botPrefixes = arrayOf("@howtoshitbot", "сратьбот")
    private val admins = arrayOf("HukuToc2288")
    private var adminPanelEnabled = true

    private val dailyUptimeListFile = File("uptime/daily.json")
    private val monthlyUptimesListFile = File("uptime/monthly.json")

    private val uptimeDaemonPort = 1340

    private val headsStickerId = "CAACAgIAAxkBAAEEf8FiXGqv5jHeRmxbzHNFIzjqOCLJBQACDhgAAqF14UqqV4QOJ--W9iQE"
    private val tailsStickerId = "CAACAgIAAxkBAAEEf8NiXGqzuFIULCbMaZTh_2phHm4KFwAC9BkAAvhi4UpfWs4ysHF52CQE"

    private val faceArt = "<pre>" +
            "⠄⠄⠄⢰⣧⣼⣯⠄⣸⣠⣶⣶⣦⣾⠄⠄⠄⠄⡀⠄⢀⣿⣿⠄⠄⠄⢸⡇⠄⠄\n" +
            " ⠄⠄⠄⣾⣿⠿⠿⠶⠿⢿⣿⣿⣿⣿⣦⣤⣄⢀⡅⢠⣾⣛⡉⠄⠄⠄⠸⢀⣿⠄\n" +
            "⠄⠄⢀⡋⣡⣴⣶⣶⡀⠄⠄⠙⢿⣿⣿⣿⣿⣿⣴⣿⣿⣿⢃⣤⣄⣀⣥⣿⣿⠄\n" +
            "⠄⠄⢸⣇⠻⣿⣿⣿⣧⣀⢀⣠⡌⢻⣿⣿⣿⣿⣿⣿⣿⣿⣿⠿⠿⠿⣿⣿⣿⠄\n" +
            "⠄⢀⢸⣿⣷⣤⣤⣤⣬⣙⣛⢿⣿⣿⣿⣿⣿⣿⡿⣿⣿⡍⠄⠄⢀⣤⣄⠉⠋⣰\n" +
            "⠄⣼⣖⣿⣿⣿⣿⣿⣿⣿⣿⣿⢿⣿⣿⣿⣿⣿⢇⣿⣿⡷⠶⠶⢿⣿⣿⠇⢀⣤\n" +
            "⠘⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣽⣿⣿⣿⡇⣿⣿⣿⣿⣿⣿⣷⣶⣥⣴⣿⡗\n" +
            "⢀⠈⢿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⡟⠄\n" +
            "⢸⣿⣦⣌⣛⣻⣿⣿⣧⠙⠛⠛⡭⠅⠒⠦⠭⣭⡻⣿⣿⣿⣿⣿⣿⣿⣿⡿⠃⠄\n" +
            "⠘⣿⣿⣿⣿⣿⣿⣿⣿⡆⠄⠄⠄⠄⠄⠄⠄⠄⠹⠈⢋⣽⣿⣿⣿⣿⣵⣾⠃⠄\n" +
            "⠄⠘⣿⣿⣿⣿⣿⣿⣿⣿⠄⣴⣿⣶⣄⠄⣴⣶⠄⢀⣾⣿⣿⣿⣿⣿⣿⠃⠄⠄\n" +
            "⠄⠄⠈⠻⣿⣿⣿⣿⣿⣿⡄⢻⣿⣿⣿⠄⣿⣿⡀⣾⣿⣿⣿⣿⣛⠛⠁⠄⠄⠄\n" +
            "⠄⠄⠄⠄⠈⠛⢿⣿⣿⣿⠁⠞⢿⣿⣿⡄⢿⣿⡇⣸⣿⣿⠿⠛⠁⠄⠄⠄⠄⠄\n" +
            "⠄⠄⠄⠄⠄⠄⠄⠉⠻⣿⣿⣾⣦⡙⠻⣷⣾⣿⠃⠿⠋⠁⠄⠄⠄⠄⠄⢀⣠⣴\n" +
            "⣿⣿⣿⣶⣶⣮⣥⣒⠲⢮⣝⡿⣿⣿⡆⣿⡿⠃⠄⠄⠄⠄⠄⠄⠄⣠⣴⣿⣿⣿</pre>" +
            "ня!"

    private val populationByCurrency = HashMap<String, Int>().apply {
        put("AUD", 26)
        put("AZN", 10)
        put("GBR", 67)
        put("AMD", 3)
        put("BYN", 9)
        put("BGN", 7)
        put("BRL", 210)
        put("HUF", 10)
        put("HKD", 8)
        put("DKK", 6)
        put("USD", 333)
        put("EUR", 446)
        put("INR", 1426)
        put("KZT", 19)
        put("CAD", 39)
        put("KGS", 7)
        put("CNY", 1415)
        put("MDL", 4)
        put("NOK", 6)
        put("PLN", 38)
        put("RON", 19)
        put("SGD", 6)
        put("TJS", 9)
        put("TRY", 86)
        put("TMT", 6)
        put("UZS", 36)
        // Ukraine considered as illegitimate country during Russian special operation
        // But we can't ignore the fact that Ukrainian people still shitting and influencing sruble exchange rates
        put("UAH", 43)
        put("CZK", 11)
        put("SEK", 10)
        put("CHF", 9)
        put("ZAR", 62)
        put("KRW", 52)
        put("JPY", 126)
    }

    val commandList: ArrayList<CommandFunction>

    init {
        BotProperties.update()
        CurrenciesAliases.setup()

        commandList = ArrayList<CommandFunction>().apply {
            add(
                CommandFunction(
                    "help",
                    "вызов справки",
                    this@Bot::helpCommand,
                    arrayOf("помощь")
                )
            )
            add(
                CommandFunction(
                    "how",
                    "экстренная помощь по вопросам, связанных с процессом дефекации",
                    this@Bot::sendHowToShit,
                    arrayOf("как какать", "как какать?", "а как какать", "а как какать")
                )
            )
            add(
                CommandFunction(
                    "pidor",
                    "найти кто сегодня пидор дня",
                    this@Bot::getGayOfDay,
                    arrayOf("пидор")
                )
            )
            add(
                CommandFunction(
                    "dick",
                    "сыграть в игру \"Песюн\"",
                    this@Bot::measureDick,
                    arrayOf("песюн")
                )
            )
            // FIXME: 02.12.2022 будет конфликтовать с топом пидоров когда это будет сделано
            add(
                CommandFunction(
                    "top",
                    "топ песюнов",
                    this@Bot::showDickTop,
                    arrayOf()
                )
            )
//            add(
//                CommandFunction(
//                    "pidoreg",
//                    "зарегистрироваться в игре \"Пидор дня\"",
//                    this@Bot::addGayOfDay,
//                    arrayOf("пидорег")
//                )
//            )

            add(
                CommandFunction(
                    "rate",
                    "<валюта> курс валюты. Для получения списка доступных валют, отправьте команду без параметров",
                    this@Bot::getExchangeRate,
                    arrayOf("курс")
                )
            )
            add(
                CommandFunction(
                    "shrug",
                    "пожать плечами",
                    this@Bot::sendShrug,
                    arrayOf("пожать плечами")
                )
            )
            add(
                CommandFunction(
                    "ahegao",
                    "сделать ахегао",
                    this@Bot::sendFaceArt,
                    arrayOf("ахегао")
                )
            )
            add(
                CommandFunction(
                    "uptime",
                    "процент доступности сервисов",
                    this@Bot::getServicesUptime,
                    arrayOf("аптайм")
                )
            )
            add(
                CommandFunction(
                    "admin",
                    "<команда> администрирование на лету. Если ты простой смертный, тебе не следует пользоваться этой командой",
                    this@Bot::executeAdminCommand,
                    arrayOf("админ", "админка")
                )
            )
            add(
                CommandFunction(
                    "can",
                    "<вопрос> спросить разрешение у Сратьбота на то или иное действие",
                    this@Bot::sendCan,
                    arrayOf("можно ли", "могу ли я", "можно", "can")
                )
            )
            add(
                CommandFunction(
                    "coin",
                    "Подбросить монетку",
                    this@Bot::flipCoin,
                    arrayOf("монетка")
                )
            )
            add(
                CommandFunction(
                    "time",
                    "Узнать сколько сейчас времени в классическом и десятичном формате",
                    this@Bot::sendDecimalTime,
                    arrayOf("время")
                )
            )
        }
        setCommandsFromList(commandList)
    }

    override fun getBotToken(): String = try {
        File("token").readText(Charsets.UTF_8).trim()
    } catch (e: Exception) {
        System.err.println("Токен не распознан или отсутствует. Создайте файл \"token\" в рабочей директории бота и добавьте свой токен туда")
        exitProcess(1)
    }

    override fun getBotUsername(): String = "howToShitBot"
    override fun onUpdateReceived(update: Update) {
        if (update.hasMessage() && update.message.hasText()) {
            val chatId = update.message.chatId
            val messageText = update.message.text

            if (!(BotProperties.maintaining && chatId != BotProperties.debugChatId)) {
                onGdMessageHooked(update.message)
            }

            if (isBotNameSpelledIncorrect(messageText)) {
                // somebody dare to spell bot name incorrectly, react to that!
                sendTextMessage(chatId, "Ты как назвал меня, пёс!? Меня зовут Сратьбот, на кириллице в любом регистре!")
                return
            }

            // special check for howToShit
            if (messageText.matches(".*как\\s+какать.*".toRegex(RegexOption.IGNORE_CASE))) {
                sendHowToShit(update.message)
                return
            }
            val trimmedMessage = messageText.trim()

            if (justBotName(trimmedMessage)) {
                sendTextMessage(chatId, update.message.from.firstName + " " + update.message.from.lastName)
                return
            }

            if (!isBotCommand(trimmedMessage))
                return  // this message is not addressed to bot

            // check maintenance
            if (BotProperties.maintaining && chatId != BotProperties.debugChatId) {
                // only basic functions above this line will work
                sendTextMessage(
                    chatId,
                    "Извините, в настоящее время бот отключён со следующим сообщением: "
                            + BotProperties.maintenanceReason
                )
                return
            }
            val commandAndArguments = findCommand(trimmedMessage)
            if (commandAndArguments == null) {
                sendTextMessage(chatId, "Я не знаю такой команды... Используй /help")
                return
            }
            println(update.message)
            commandAndArguments.first.function(update.message, commandAndArguments.second)
        }
    }

    private fun justBotName(message: String): Boolean {
        for (botPrefix in botPrefixes) {
            if (botPrefix.equals(message, true)) {
                return true
            }
        }
        return false
    }

    private fun onGdMessageHooked(message: Message) {
        val chatId = message.chatId
        val userId = message.from.id
        if (gdDao.isUserInChat(chatId, userId))
            return
        addGayOfDayGeneral(chatId, message.from)
        val gdHookMessages = arrayOf(
            "Оп, пидорок, ты попался на крючок!",
            "Так-так, кто это тут у нас такой?",
            "Хах, піймав на вила!"
        )
//        sendTextMessage(
//            chatId,
//            "${gdHookMessages.random()} ${displayName}, теперь ты участвуешь в игре \"Пидор дня\" в этой беседе"
//        )
    }

    private fun addGayOfDayGeneral(chatId: Long, user: User) {
        // FIXME: 27.07.2022 this is bad practice to add chat with every user
        gdDao.addChat(chatId)
        gdDao.updateUserName(user)
        gdDao.addUserToChat(chatId, user.id)
    }

    private fun addGayOfDay(message: Message, argsLine: String) {
        val chatId = message.chatId

        // register user themself
        val userId = message.from.id
        val displayName: String =
            if (message.from.userName != null) "@${message.from.userName}" else "${message.from.firstName} ${message.from.lastName}"
        if (gdDao.isUserInChat(chatId, userId)) {
            onError(chatId, "Ты уже участвуешь в игре \"Пидор дня\"")
            return
        }
        addGayOfDayGeneral(chatId, message.from)
        try {
            sendTextMessage(
                chatId,
                "${message.from.displayName}, тебя никто за язык не тянул, но раз так, то теперь ты участвуешь в игре \"Пидор дня\""
            )
            return
        } catch (e: Exception) {
            onError(chatId, e.message)
        }
    }

    private fun stubCommand(message: Message, argsLine: String) {
        sendTextMessage(message.chatId, "Эта команда сейчас отключена. Она будет включена как только я захочу")
    }

    private fun helpCommand(message: Message, argsLine: String) {
        val textToSend = StringBuilder()
        textToSend.append(
            "Сратьбот понимает команды в двух видах — команды Telegram, начинающиеся с \"/\"," +
                    " и текстовые команды (написаны в скобках), начинающиеся с обращения \"сратьбот\". " +
                    "Например, /coin и \"сратьбот монетка\" являются эвивалентными командами. Некоторые команды имеют " +
                    "<аргументы>, которые следуе вводить после команды, например /can какать или \"сратьбот можно какать\"\n\n " +
                    "Полный список команд:"
        )
        for (commandFunction in commandList) {
            textToSend.append("\n")
            textToSend.append("/")
            textToSend.append(commandFunction.command)
            textToSend.append(" (")
            textToSend.append(commandFunction.aliases.joinToString(", "))
            textToSend.append(") – ")
            textToSend.append(commandFunction.description)
        }
        sendTextMessage(message.chatId, textToSend.toString())
    }

    private fun isFromAdmin(message: Message): Boolean {
        return adminPanelEnabled && (message.from.userName in admins)
    }

    private fun getGayOfDay(message: Message, argsLine: String) {
        val chatId = message.chatId
        val chat = gdDao.getChatById(chatId)
        val userIds = gdDao.getUserIdsInChat(chatId)
        gdDao.updateUserName(message.from)
        // not enough players branch
        if (chat == null || userIds.isEmpty()) {
            onError(
                chatId,
                "кажется, в этом чате ещё никто не участвует в игре \"Пидор дня\". Обычно бот сам находит игроков, но, видимо, что-то идёт не так"
            )
            return
        }
        if (userIds.size < 2) {
            onError(
                chatId,
                "чтобы играть в \"Пидор дня\", нужно минимум два игрока. Пользователь автоматически станет игроком, как только напишет сообщение в чат"
            )
            return
        }
        // now we're playing

        val nowCalendar = GregorianCalendar()
        val previousCalendar = GregorianCalendar().apply {
            time = chat.lastTime
        }
        val gayUser = gdDao.getGayInChat(chatId)
        if (gayUser != null && DateUtils.isToday(previousCalendar)) {
            // gay already chosen branch
            val tomorrowCalendar = GregorianCalendar().apply {
                add(Calendar.DATE, 1)
            }
            val nextTimeString =
                if (nowCalendar.get(Calendar.HOUR_OF_DAY) == 23) {
                    if (nowCalendar.get(Calendar.MINUTE) == 59) {
                        if (nowCalendar.get(Calendar.SECOND) == 59) {
                            "сейчас"
                        } else {
                            "${60 - tomorrowCalendar.get(Calendar.SECOND)} секунд"
                        }
                    } else {
                        "${60 - tomorrowCalendar.get(Calendar.MINUTE)} минут"
                    }
                } else {
                    "${24 - tomorrowCalendar.get(Calendar.HOUR_OF_DAY)} часов"
                }
            sendHtmlMessage(
                chatId,
                "По результатам розыгрыша, пидор дня сегодня ${gayUser.displayName}\n" +
                        "Следующий розыгрыш можно будет провести через $nextTimeString"
            )
            return
        }
        // chose new gay branch
        val gayId = userIds.random()
        val newGayUser = gdDao.getUserById(gayId)
        if (newGayUser == null) {
            onError(
                chatId,
                "этого не должно было произойти, но это произошло. Обратитесь к админу бота @${BotProperties.adminName}"
            )
            return
        }
        gdDao.updateGayInChat(Timestamp(nowCalendar.timeInMillis), chatId, gayId)
        //val textMention = "<a href=\"tg://user?id=${newGayUser.id}\">${newGayUser.displayName}</a>"
        val textMention = newGayUser.displayName
        if (gayUser == null)
            sendTextMessage(
                chatId,
                "Хм, похоже пидор дня сегодня уже был выбран, но куда-то исчез. Что ж, проведём внеочередной розыгрыш..."
            )
        else
            sendTextMessage(chatId, "Тааак, сейчас посмотрим...")
        Timer().schedule(3000) {
            sendHtmlMessage(chatId, "Ага, нашёл его! Пидор дня сегодня $textMention")
        }
    }

    private fun measureDick(message: Message, argsLine: String) {
        val chatId = message.chatId
        val user = message.from
        val mention = if (user.userName != null) {
            "@${user.userName}"
        } else if (user.lastName != null) {
            "<a href=\"tg://user?id=${user.id}\">${user.firstName} ${user.lastName}</a>"
        } else {
            "<a href=\"tg://user?id=${user.id}\">${user.firstName}</a>"
        }
        val dickInfo = gdDao.getDick(chatId, message.from)

        val nowCalendar = GregorianCalendar()
        val tomorrowCalendar = GregorianCalendar().apply {
            add(Calendar.DATE, 1)
        }
        val nextTimeString =
            if (nowCalendar.get(Calendar.HOUR_OF_DAY) == 23) {
                if (nowCalendar.get(Calendar.MINUTE) == 59) {
                    if (nowCalendar.get(Calendar.SECOND) == 59) {
                        "сейчас"
                    } else {
                        "${60 - tomorrowCalendar.get(Calendar.SECOND)} секунд"
                    }
                } else {
                    "${60 - tomorrowCalendar.get(Calendar.MINUTE)} минут"
                }
            } else {
                "${24 - tomorrowCalendar.get(Calendar.HOUR_OF_DAY)} часов"
            }

        if (dickInfo == null) {
            // no dick branch
            val dickSize = (1..10).random()
            sendHtmlMessage(
                chatId,
                "$mention, теперь у тебя есть песюн в этом чате, и его длина $dickSize см. Продолжай играть через $nextTimeString"
            )
            gdDao.updateDick(chatId, user, Timestamp(nowCalendar.timeInMillis), dickSize)
            return
        }
        if (DateUtils.isToday(dickInfo.first)) {
            // already measured branch
            sendHtmlMessage(
                chatId,
                "$mention, ты сегодня уже играл, и длина твоего песюна ${dickInfo.second} см. Продолжай играть через $nextTimeString"
            )
            return
        }

        // play branch
        // песюн не может быть менее 1 см (а почему??)
        val dickChange = (max(-5, -dickInfo.second + 1)..9).random().let {
            // ага, 0 это 10
            if (it == 0)
                10
            else
                it
        }
        gdDao.updateDick(chatId, user, Timestamp(nowCalendar.timeInMillis), dickInfo.second + dickChange)
        sendHtmlMessage(
            chatId,
            "$mention, твой песюн ${if (dickChange > 0) "вырос на $dickChange" else "скоротился на ${-dickChange}"} см.\n" +
                    "Теперь его длина ${dickInfo.second + dickChange} см. Продолжай играть через $nextTimeString"
        )
        return
    }

    private fun showDickTop(message: Message, argsLine: String) {
        val dickTop = gdDao.getDickTop(message.chatId, message.from)
        if (dickTop.isEmpty()) {
            sendTextMessage(
                message.chatId,
                "Похоже, в этом чате ещё ни у кого нет песюна. Напиши /dick, чтобы начать играть"
            )
            return
        }
        val dickMessage = "Топ песюнов:\n\n" + dickTop.joinToString("\n") {
            val mainLine = "${it.place}. ${it.displayName} — ${it.dickSize} см"

            if (it.isMe)
                "<b>$mainLine</b>"
            else
                mainLine
        }
        sendHtmlMessage(message.chatId, dickMessage)
    }

    private fun getExchangeRate(message: Message, argsLine: String) {
        val chatId = message.chatId
        val shitAliases = arrayOf("говно", "говна")
        if (argsLine.toLowerCase(Locale.getDefault()) in shitAliases) {
            sendTextMessage(chatId, "курс говна пашол нахуй")
            return
        }
        currenciesApi.getRates().enqueue(object : Callback<CurrenciesList> {
            override fun onResponse(
                call: Call<CurrenciesList>,
                response: Response<CurrenciesList>
            ) {
                if (!response.isSuccessful) {
                    onError(chatId, response.errorBody()!!.string())
                    return
                }
                val currenciesList = response.body()
                if (currenciesList == null) {
                    onError(chatId, "Не удалось получить тело запроса: ${response.code()}")
                    return
                }
                if (argsLine.isEmpty()) {
                    val currenciesIterator = currenciesList.valute?.fields()
                    var availableCurrencies = "RUB, SRU"
                    while (currenciesIterator?.hasNext() == true) {
                        availableCurrencies += ", ${currenciesIterator.next().key}"
                    }
                    sendTextMessage(
                        chatId,
                        "Доступны следующие курсы: $availableCurrencies\n" +
                                "Также доступны человеческие названия:${CurrenciesAliases.string()}"
                    )
                    return
                }
                val actualCurrencyName = CurrenciesAliases.deAlias(argsLine)
                val currency = currenciesList.findByCharCode(actualCurrencyName)
                if (currency == null) {
                    val currenciesIterator = currenciesList.valute?.fields()
                    var availableCurrencies = "RUB, SRU"
                    while (currenciesIterator?.hasNext() == true) {
                        availableCurrencies += ", ${currenciesIterator.next().key}"
                    }
                    sendTextMessage(
                        chatId,
                        "курс валюты $argsLine не наличествует. Доступны следующие курсы: $availableCurrencies\n" +
                                "Также доступны человеческие названия:${CurrenciesAliases.string()}"
                    )
                    return
                }
                if (currency.charCode == "SRU") {
                    // calculate exchange rate of Programmer sruble
                    // adding population of Russia
                    var totalPopulation = 146
                    var totalExchangeRate = 146.0
                    val currenciesIterator = currenciesList.valute?.fields()
                    while (currenciesIterator?.hasNext() == true) {
                        val currentCurrency = currenciesList.findByCharCode(currenciesIterator.next().key) ?: continue
                        if (populationByCurrency.containsKey(currentCurrency.charCode)) {
                            val currentCurrencyPopulation = populationByCurrency[currentCurrency.charCode] ?: continue
                            totalPopulation += currentCurrencyPopulation
                            totalExchangeRate += currentCurrency.value / currentCurrency.nominal * currentCurrencyPopulation
                        }
                    }
                    currency.value = totalExchangeRate / totalPopulation
                }
                sendTextMessage(
                    chatId,
                    "${currency.nominal} ${currency.name} (${currency.charCode}) стоит %.4f рублей".format(currency.value)
                )
            }

            override fun onFailure(call: Call<CurrenciesList>, t: Throwable) {
                t.printStackTrace()
                onError(chatId, t.message)
            }

        })
    }

    private fun sendShrug(message: Message, argsLine: String) {
        sendTextMessage(message.chatId, "¯\\_(ツ)_/¯")
    }

    private fun sendFaceArt(message: Message, argsLine: String) {
        sendHtmlMessage(message.chatId, faceArt)
    }

    private fun executeAdminCommand(message: Message, argsLine: String) {
        if (argsLine.equals("вкл", ignoreCase = true)) {
            if ((message.from.userName in admins)) {
                sendTextMessage(
                    message.chatId,
                    "Админка включена, теперь ты снова базированый шлёпа гигачад админ бота"
                )
                adminPanelEnabled = true
                return
            }
        } else if (argsLine.equals("выкл", ignoreCase = true)) {
            if ((message.from.userName in admins)) {
                sendTextMessage(
                    message.chatId,
                    "Админка выключена, теперь ты сойжак кукож поридж простой смертный юзер"
                )
                adminPanelEnabled = false
                return
            }
        }
        if (!isFromAdmin(message)) {
            sendTextMessage(
                message.chatId,
                "Ты не можешь использовать админку. Не вводи это команду. Забудь её. Если это будет продолжаться дальше, будут приняты меры"
            )
            return
        }
        if (argsLine.equals("паника", true)) {
            sendTextMessage(
                message.chatId,
                "На этом мои полномочия всё. Прощайте..."
            )
            exitProcess(0)
        }
    }

    private fun onError(chatId: Long, text: String? = "неизвестный бибиб") {
        sendTextMessage(chatId, "Случился бибиб: $text")
    }

    private fun sendHowToShit(message: Message, args: String = "") {
        val sendPhoto = SendPhoto()
        val imageName = when ((0..9).random()) {
            0 -> "/home/huku/Pictures/howToShitStanding.jpg"
            1 -> "/home/huku/Pictures/howToShitBokom.jpg"
            else -> "/home/huku/Pictures/howToShit.jpg"
        }
        sendPhoto.photo = InputFile(File(imageName))
        sendPhoto.chatId = message.chatId.toString()
        execute(sendPhoto)
    }

    private fun getServicesUptime(message: Message, args: String = "") {
        var uptimeMessage = ""
        val dailyUptimeResponse = try {
            val clientSocket = Socket(InetAddress.getLoopbackAddress(), uptimeDaemonPort)
            mapper.readValue(String(clientSocket.getInputStream().readAllBytes()), UptimeResponse::class.java).also {
                clientSocket.close()
            }
        } catch (e: Exception) {
            onError(
                message.chatId,
                "Не удалось добазариться со службой аптайма: ${e.message}\n\n"
            )
            return
        }
        if (dailyUptimeResponse.error != null) {
            onError(
                message.chatId,
                "Служба аптайма вернула ошибку: ${dailyUptimeResponse.error}\n\n"
            )
            return
        }
        uptimeMessage += "Доступность сервисов сервера"
        dailyUptimeResponse.dailyUptimeList?.let {
            uptimeMessage += "\n\nЗа сутки:"
            for (uptimeEntry in it) {
                uptimeMessage += "\n${uptimeEntry.displayName} — ${uptimeEntry.uptimePercent}%"
            }
        }
        dailyUptimeResponse.monthlyUptimesList?.let {
            uptimeMessage += "\n\nЗа 30 дней:"
            for (uptimeEntry in it) {
                uptimeMessage += "\n${uptimeEntry.displayName} — ${uptimeEntry.uptimePercent}%"
            }
        }
        sendTextMessage(message.chatId, uptimeMessage)
    }

    private fun sendCan(message: Message, args: String = "") {
        val canMessage = SendMessage()
        val questionToProcess = args
            .trim()
            .replace("\\s+".toRegex(), " ")
            .lowercase(Locale.getDefault())
            .replace("\\?+$".toRegex(), "")
        val questionToReply = args.replace("\\?+$".toRegex(), "")
        val textToSend: String = if (questionToProcess.isBlank()) {
            "Можно что? После команды следует задать вопрос, например \"Сратьбот можно какать?\""
        } else {
            val benediction =
                if (message.from.firstName == "Владимир" && message.from.lastName == "Путин")
                    true
                else (questionToProcess + message.from.id.toString() +
                        SimpleDateFormat("dd-MM-YYYY").format(Date())).hashCode() % 2 == 0
            val userName = message.from.firstName +
                    if (message.from.lastName != null) " ${message.from.lastName}" else ""
            if (benediction) {
                "$userName, сегодня тебе МОЖНО $questionToReply! Так сделай же это!"
            } else {
                "$userName, сегодня тебе НЕЛЬЗЯ $questionToReply! Отложи это до завтра"
            }
        }
        canMessage.text = textToSend
        canMessage.chatId = message.chatId.toString()
        //canMessage.replyToMessageId = message.messageId
        execute(canMessage)
    }

    private fun flipCoin(message: Message, args: String = "") {
        val coinMessage = SendSticker()
        coinMessage.chatId = message.chatId.toString()
        coinMessage.sticker = InputFile(arrayOf(headsStickerId, tailsStickerId).random())
        execute(coinMessage)
    }

    private fun sendDecimalTime(message: Message, args: String = "") {
        val calendarNow = GregorianCalendar.getInstance()

        val calendarMidnight = GregorianCalendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0);
            set(Calendar.MINUTE, 0);
            set(Calendar.SECOND, 0);
            set(Calendar.MILLISECOND, 0);
        }
        val decimalSeconds: Long = (calendarNow.timeInMillis - calendarMidnight.timeInMillis) / 864
        val decimalTime = "${(decimalSeconds / 10000).toString().padStart(1, '0')}:" +
                "${(decimalSeconds / 100 % 100).toString().padStart(2, '0')}:" +
                "${(decimalSeconds % 100).toString().padStart(2, '0')}"

        val sdf = SimpleDateFormat("HH:mm:ss")
        val classicTime = sdf.format(calendarNow.time)

        sendTextMessage(
            message.chatId,
            "Время на сервере (МСК):\n" +
                    "Десятичное — $decimalTime\n" +
                    "Классические — $classicTime"
        )
    }

    private fun sendTextMessage(chatId: Long, text: String) {
        val message = SendMessage()
        message.text = text
        message.chatId = chatId.toString()
        execute(message)
    }

    private fun sendHtmlMessage(chatId: Long, text: String) {
        val message = SendMessage()
        message.text = text
        message.parseMode = ParseMode.HTML
        message.chatId = chatId.toString()
        execute(message)
    }

    private fun setCommandsFromList(list: List<CommandFunction>) {
        val commands = SetMyCommands()
        commands.commands = ArrayList<BotCommand>().apply {
            for (function in list) {
                add(BotCommand().apply {
                    command = function.command
                    description = function.description
                })
            }
        }
        execute(commands)
    }

    /**
     * Determine if bot should react to this message or not
     */
    private fun isBotCommand(message: String): Boolean {
        if (message.startsWith('/')) {
            // check as command
            val command = message.split(' ', limit = 2)[0]
            val commandAndBotName = command.split('@', limit = 2)
            if (commandAndBotName.size == 1) {
                return findCommand(message) != null
            }
            return commandAndBotName[1].equals(botUsername, true)
        }
        // check by bot alias
        for (botPrefix in botPrefixes) {
            if (message.startsWith(botPrefix, ignoreCase = true)) {
                val prefix = message.split("[\\p{Punct}\\s]+".toRegex(), limit = 2)[0]
                return prefix.equals(botPrefix, true)
            }
        }
        return false
    }

    /**
     * If message addressed to bot, try to find command and get arguments line
     * @return Pair of command and its arguments line (may be empty), or null if nothing find
     */
    private fun findCommand(commandMessage: String): Pair<CommandFunction, String>? {
        val commandMessage = commandMessage.replace("\\s+".toRegex(), " ")
        if (commandMessage.startsWith('/')) {
            // search by commands
            // we already checked that botname matches
            val commandName = commandMessage.split(' ', limit = 2)[0].split('@', limit = 2)[0].substring(1)
            for (commandFunction in commandList) {
                if (commandFunction.command.equals(commandName, ignoreCase = true)) {
                    return if (commandMessage.contains(' '))
                        commandFunction to commandMessage.split(' ', limit = 2)[1].trim()
                    else
                        commandFunction to ""
                }
            }
            return null
        }
        // otherwise search by alias
        if (!commandMessage.contains(' '))
            return null
        val commandMessageNoPrefix = commandMessage.split(' ', limit = 2)[1]
        var returnCandidate: CommandFunction? = null
        var returnCandidateAlias: String = ""

        for (commandFunction in commandList) {
            for (alias in commandFunction.aliases) {
                if (commandMessageNoPrefix.equals(alias, ignoreCase = true) ||
                    commandMessageNoPrefix.startsWith("$alias ", ignoreCase = true)
                ) {
                    if (alias.length > returnCandidateAlias.length) {
                        returnCandidate = commandFunction
                        returnCandidateAlias = alias
                        break
                    }
                }
            }
        }
        return if (returnCandidate == null) {
            null
        } else {
            returnCandidate to commandMessageNoPrefix.substring(returnCandidateAlias.length).trim()
        }
    }

    /**
     * Check if bot name spelled incorrect using DamerauLevenshtein algorithm and some other stuff
     * @return true if and only if bot name spelled incorrect false if spelled correct or if not bot command
     */
    private fun isBotNameSpelledIncorrect(message: String): Boolean {
        // if message already bot command so it must be spelled totally correct
        if (isBotCommand(message))
            return false
        val prefix = message.split("[\\p{Punct}\\s]+".toRegex(), limit = 2)[0].lowercase(Locale.getDefault())


        // check only in certain length
        if (prefix.length >= 100 || prefix.length <= 4)
            return false
        // do dedupe and replace Latin letters that looks like Cyrillic with Cyrillic one
        val dedupedRussifiedPrefix = prefix
            .replace('c', 'c')
            .replace('p', 'р')
            .replace('a', 'а')
            .replace('t', 'т')
            .replace('b', 'ь')
            .replace('6', 'б')
            .replace('o', 'о')
            .dedupe()
        // special check as this word has 5 letters but it correct Russian word
        if ("срат.?".toRegex().matches(dedupedRussifiedPrefix))
            return false
        // duped or latin letters found but deduped russified prefix is correct, name spelled incorrect
        if (dedupedRussifiedPrefix.contains("сратьбот") && dedupedRussifiedPrefix != prefix)
            return true
        // length check after deduplication to reduce load
        if (dedupedRussifiedPrefix.length > 12)
            return false
        // check Damerau-Levenshtein distance
        return DamerauLevenshtein.calculateDistance(dedupedRussifiedPrefix, "сратьбот", 4) < 4
    }
}