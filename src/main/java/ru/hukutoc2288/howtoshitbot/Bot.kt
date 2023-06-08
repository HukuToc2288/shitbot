package ru.hukutoc2288.howtoshitbot

import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import org.telegram.telegrambots.meta.api.objects.User
import ru.hukutoc2288.howtoshitbot.commands.*
import ru.hukutoc2288.howtoshitbot.dao.GdDao
import ru.hukutoc2288.howtoshitbot.utils.*
import ru.hukutoc2288.howtoshitbot.utils.StringUtils.dedupe
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


    private val botPrefixes = arrayOf("@howtoshitbot", "сратьбот")

    private val dailyUptimeListFile = File("uptime/daily.json")
    private val monthlyUptimesListFile = File("uptime/monthly.json")

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

    private val commandList: ArrayList<CommandFunction>
    private val howToShitCommand: CommandFunction = HowToShitCommand()

    init {
        BotProperties.update()

        commandList = ArrayList<CommandFunction>().apply {
            add(HelpCommand(this))
            add(howToShitCommand)
            add(PidorCommand())
            add(DickCommand)
            // FIXME: 02.12.2022 будет конфликтовать с топом пидоров когда это будет сделано
            add(DickTopCommand())
            add(KnbCommand)
            add(AnekCommand)
//            add(
//                object : CommandFunction(
//                    "pidoreg",
//                    "зарегистрироваться в игре \"Пидор дня\"",
//                    this@Bot::addGayOfDay,
//                    arrayOf("пидорег")
//                )
//            )

            add(ExchangeRateCommand)
            add(
                SimpleSendTextCommand(
                    "shrug",
                    "пожать плечами",
                    "¯\\_(ツ)_/¯",
                    arrayOf("пожать плечами")
                )
            )
            add(SimpleSendTextCommand(
                "ahegao",
                "сделать ахегао",
                faceArt,
                arrayOf("ахегао"),
                true
            ))
            add(UptimeCommand())
            add(AdminCommand())
            add(CanCommand())
            add(FlipCoinCommand())
            add(TimeCommand())
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
        try {
            if (update.hasMessage() && update.message.hasText()) {
                val chatId = update.message.chatId
                val messageText = update.message.text

                if (!(BotProperties.maintaining && chatId != BotProperties.debugChatId)) {
                    onGdMessageHooked(update.message)
                }

                if (isBotNameSpelledIncorrect(messageText)) {
                    // somebody dare to spell bot name incorrectly, react to that!
                    sendTextMessage(
                        chatId,
                        "Ты как назвал меня, пёс!? Меня зовут Сратьбот, на кириллице в любом регистре!"
                    )
                    return
                }

                // special check for howToShit
                if (messageText.matches(".*как\\s+какать.*".toRegex(RegexOption.IGNORE_CASE))) {
                    howToShitCommand.execute(update.message, "")
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
                commandAndArguments.first.execute(update.message, commandAndArguments.second)
            } else if (update.hasCallbackQuery()) {
                // играем в КНБ
                KnbCommand.processCallback(update.callbackQuery)
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
        if (GdDao.isUserInChat(chatId, userId))
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
        GdDao.addChat(chatId)
        GdDao.updateUserName(user)
        GdDao.addUserToChat(chatId, user.id)
    }

    private fun addGayOfDay(message: Message, argsLine: String) {
        val chatId = message.chatId

        // register user themself
        val userId = message.from.id
        val displayName: String =
            if (message.from.userName != null) "@${message.from.userName}" else "${message.from.firstName} ${message.from.lastName}"
        if (GdDao.isUserInChat(chatId, userId)) {
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

    fun onError(chatId: Long, text: String? = "неизвестный бибиб") {
        sendTextMessage(chatId, "Случился бибиб: $text")
    }

    fun sendTextMessage(chatId: Long, text: String, replyId: Int = 0) {
        val message = SendMessage()
        message.text = text
        message.chatId = chatId.toString()
        if (replyId != 0)
            message.replyToMessageId = replyId
        execute(message)
    }

    fun sendHtmlMessage(chatId: Long, text: String, replyId: Int = 0) {
        val message = SendMessage()
        message.text = text
        message.parseMode = ParseMode.HTML
        message.chatId = chatId.toString()
        if (replyId != 0)
            message.replyToMessageId = replyId
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