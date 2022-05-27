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
import ru.hukutoc2288.howtoshitbot.entinies.gayofday.GayOfDay
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.schedule
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import org.telegram.telegrambots.meta.api.methods.send.SendSticker
import org.telegram.telegrambots.meta.api.objects.stickers.Sticker
import ru.hukutoc2288.howtoshitbot.entinies.uptime.UptimeResponse
import ru.hukutoc2288.howtoshitbot.utils.*
import ru.hukutoc2288.howtoshitbot.utils.StringUtils.dedupe
import java.net.InetAddress
import java.net.Socket
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.temporal.ChronoField
import kotlin.collections.HashMap
import java.util.Calendar
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
        CurrenciesAliases.setup()

        commandList = ArrayList<CommandFunction>().apply {
            add(
                CommandFunction(
                    "help",
                    "тут ничего нет",
                    this@Bot::stubCommand,
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
                    "pidoreg",
                    "зарегистрироваться в игре \"Пидор дня\"",
                    this@Bot::addGayOfDay,
                    arrayOf("пидорег")
                )
            )

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

            if (isBotNameSpelledIncorrect(messageText)) {
                // somebody dare to spell bot name incorrectly, react to that!
                sendTextMessage(chatId, "Ты как назвал меня, пёс!? Меня зовут Сратьбот, на кириллице в любом регистре!")
                return
            }

            // special check for howToShit
            if (messageText.replace("\\s+".toRegex(), " ")
                    .lowercase(Locale.getDefault())
                    .matches(".*как какать.*".toRegex())
            ) {
                sendHowToShit(update.message)
                return
            }

            if (!isBotCommand(messageText))
                return  // this message is not addressed to bot
            val commandAndArguments = findCommand(messageText)
            if (commandAndArguments == null) {
                sendTextMessage(chatId, "Я не знаю такой команды... Используй /help")
                return
            }
            println(update.message)
            commandAndArguments.first.function(update.message, commandAndArguments.second)
        }
    }

    private fun addGayOfDay(message: Message, argsLine: String) {
        val chatId = message.chatId
        if (argsLine.isNotEmpty()) {
            // register another user as admin
            if (!isFromAdmin(message)) {
                onError(
                    chatId,
                    "регистрировать других может только админ! Если хочешь зарегистриророваться сам, напиши просто /pidoreg@HowToShitBot без параметров"
                )
                return
            }
            for (mention in message.entities) {
                if (mention.type == "mention") {
                    // store only username
                    addGayOfDayAdmin(
                        chatId,
                        message.text.substring(mention.offset, mention.offset + mention.length)
                    )
                } else if (mention.type == "text_mention") {
                    // store display name and user id
                    addGayOfDayAdmin(
                        chatId,
                        message.text.substring(mention.offset, mention.offset + mention.length),
                        mention.user.id
                    )
                }
            }
            return
        }
        // register user themself
        val userId = message.from.id
        val displayName: String =
            if (message.from.userName != null) "@${message.from.userName}" else "${message.from.firstName} ${message.from.lastName}"

        val chats = readOrCreateGdFile()
        val chat = chats.getOrCreate(chatId)
        val existingUser = chat.getOrNull(displayName, userId)
        if (existingUser != null) {
            onError(chatId, "Ты уже участвуешь в игре \"Пидор дня\"")
            if (existingUser.id == null) {
                // add id if user haven't it previously
                existingUser.id = userId
                try {
                    updateGdFile(chats)
                } catch (e: Exception) {
                    // shouldn't do anything as it's not critical and also it shouldn't normally happen
                    e.printStackTrace()
                }
            }
            return
        }
        val user = chat.getOrCreate(displayName, userId)
        try {
            updateGdFile(chats)
            val textMention = if (user.displayName.startsWith('@')) {
                //remove @ symbol
                user.displayName.substring(1)
            } else {
                //   "<a href=\"tg://user?id=${user.id}\">inline mention of a user</a>"
                user.displayName
            }
            sendTextMessage(
                chatId,
                "$textMention, тебя никто за язык не тянул, но раз так, то теперь ты участвуешь в игре \"Пидор дня\""
            )
            return
        } catch (e: Exception) {
            onError(chatId, e.message)
        }
        onError(chatId)
    }

    private fun stubCommand(message: Message, argsLine: String) {
        sendTextMessage(message.chatId, "Эта команда сейчас отключена. Она будет включена как только я захочу")
    }

    private fun isFromAdmin(message: Message): Boolean {
        return adminPanelEnabled && (message.from.userName in admins)
    }

    private fun addGayOfDayAdmin(chatId: Long, displayName: String, userId: Long? = null) {
        val chats = readOrCreateGdFile()
        val chat = chats.getOrCreate(chatId)
        if (chat.getOrNull(displayName, userId) != null) {
            onError(chatId, "Этот пользователь уже участвует в игре \"Пидор дня\"")
            return
        }
        val user = chat.getOrCreate(displayName, userId)
        try {
            updateGdFile(chats)
            val textMention = if (user.displayName.startsWith('@')) {
                //remove @ symbol
                user.displayName.substring(1)
            } else {
                //   "<a href=\"tg://user?id=${user.id}\">inline mention of a user</a>"
                user.displayName
            }
            sendHtmlMessage(
                chatId,
                "$textMention, по велению админа бота теперь ты теперь участвуешь в игре \"Пидор дня\""
            )
            return
        } catch (e: Exception) {
            onError(chatId, e.message)
        }
        onError(chatId)
    }

    private fun getGayOfDay(message: Message, argsLine: String) {
        val chatId = message.chatId
        val chats = readOrCreateGdFile()
        val chat = chats.getOrNull(chatId)
        // not enough players branch
        if (chat == null || chat.users.isNullOrEmpty()) {
            onError(
                chatId,
                "кажется, в этом чате ещё никто не участвует в игре \"Пидор дня\". Чтобы принять участие, напиши \"сратьбот пидорег\""
            )
            return
        }
        chat.users?.let {
            if (it.size < 2) {
                onError(
                    chatId,
                    "чтобы играть в \"Пидор дня\", нужно как минимум два игрока. Прими участие, написав \"сратьбот пидорег\""
                )
                return
            }
            val nowCalendar = GregorianCalendar()
            val previousCalendar = GregorianCalendar().apply {
                timeInMillis = chat.lastTime
            }
            if (DateUtils.isToday(previousCalendar)) {
                // gay already chosen branch
                val gayUser = it[chat.gayIndex]
                val textMention = if (gayUser.id != null) {
                    "<a href=\"tg://user?id=${gayUser.id}\">${gayUser.displayName}</a>"
                } else {
                    gayUser.displayName
                }
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
                    "По результатам розыгрыша, пидор дня сегодня $textMention\n" +
                            "Следующий розыгрыш можно будет провести через $nextTimeString"
                )
                return
            }
            // chose new gay branch
            chat.gayIndex = it.indices.random()
            chat.lastTime = nowCalendar.timeInMillis
            updateGdFile(chats)
            val gayUser = it[chat.gayIndex]
            val textMention = if (gayUser.id != null) {
                "<a href=\"tg://user?id=${gayUser.id}\">${gayUser.displayName}</a>"
            } else {
                gayUser.displayName
            }
            sendTextMessage(chatId, "Тааак, сейчас посмотрим...")
            Timer().schedule(3000) {
                sendHtmlMessage(chatId, "Ага, нашёл его! Пидор дня сегодня $textMention")
            }
        }
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
    }

    private fun readOrCreateGdFile(): GayOfDay {
        val gayOfDayFile = File("./gayOfDay.json")
        if (!gayOfDayFile.exists()) {
            gayOfDayFile.createNewFile()
        }
        return try {
            mapper.readValue(gayOfDayFile, GayOfDay::class.java)
        } catch (e: Exception) {
            GayOfDay()
        }
    }

    @Throws(Exception::class)
    private fun updateGdFile(chats: GayOfDay) {
        val gayOfDayFile = File("./gayOfDay.json")
        mapper.writeValue(gayOfDayFile, chats)
    }

    private fun onError(chatId: Long, text: String? = "неизвестный бибиб") {
        sendTextMessage(chatId, "Случился бибиб: $text")
    }

    private fun sendHowToShit(message: Message, args: String = "") {
        val sendPhoto = SendPhoto()
        val imageName = if ((0..9).random() > 0) "/home/huku/Pictures/howToShit.jpg"
        else "/home/huku/Pictures/howToShitStanding.jpg"
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
            val command = message.split(' ', limit = 2)[0]
            return command.endsWith("@$botUsername", ignoreCase = true)
        }
        val prefix = message.split("[\\p{Punct}\\s]+".toRegex(), limit = 2)[0]
        for (botPrefix in botPrefixes) {
            if (prefix.equals(botPrefix, ignoreCase = true))
                return true
        }
        return false
    }

    /**
     * If message addressed to bot, try to find command and get arguments line
     * @return Pair of command and its arguments line (may be empty), or null if nothing fing
     */
    private fun findCommand(commandMessage: String): Pair<CommandFunction, String>? {
        val commandMessage = commandMessage.replace("\\s+".toRegex(), " ")
        if (commandMessage.startsWith('/')) {
            // search by command
            val commandName = commandMessage.substring(1, commandMessage.indexOf('@'))
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