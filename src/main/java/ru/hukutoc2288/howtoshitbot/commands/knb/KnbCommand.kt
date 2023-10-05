@file:Suppress("ConstPropertyName")

package ru.hukutoc2288.howtoshitbot.commands.knb

import java.util.TreeMap
import kotlin.math.abs
import kotlin.reflect.full.primaryConstructor
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.hukutoc2288.howtoshitbot.bot
import ru.hukutoc2288.howtoshitbot.commands.knb.anticheat.CustomKnbAntiCheat
import ru.hukutoc2288.howtoshitbot.commands.knb.anticheat.KnbAntiCheat
import ru.hukutoc2288.howtoshitbot.commands.knb.anticheat.MercifulKnbAntiCheat
import ru.hukutoc2288.howtoshitbot.dao.GdDao
import ru.hukutoc2288.howtoshitbot.utils.CommandFunction
import ru.hukutoc2288.howtoshitbot.utils.mention

private const val gameTitle = "\"Камень, ножницы, бумага\""

object KnbCommand : CommandFunction("knb", "сыграть в игру $gameTitle") {

    override val requiredFeatures: Int = Features.BASIC or Features.DB_RW

    private val choices = arrayOf("камень ✊", "ножницы ✌", "бумага \uD83E\uDD1A")
    private var antiCheat: KnbAntiCheat = try {
        // TODO: это нужно вынести в отдельную библиотеку, которая будет подключаться к проекту,
        //  но пока и так сойдёт
        Class.forName("ru.hukutoc2288.howtoshitbot.commands.knb.anticheat.CustomKnbAntiCheat").kotlin.primaryConstructor!!.call() as KnbAntiCheat
    } catch (e: Exception) {
        e.printStackTrace()
        MercifulKnbAntiCheat()
    }

    // TODO: сильная связь кнб и песюна
    val gameTitle = "\"Камень, ножницы, бумага\""
    val waitingPlayers = HashMap<Long, Pair<User, Int>>()
    const val bet = 5

    private val keyboard: InlineKeyboardMarkup by lazy {
        InlineKeyboardMarkup(listOf(arrayListOf(), arrayListOf())).apply {
            for (i in choices.indices) {
                keyboard[i / 2].add(InlineKeyboardButton(choices[i]).apply {
                    callbackData = "$command/$i"
                })
            }
            keyboard[1].add(InlineKeyboardButton("выйти ❌").apply {
                callbackData = "$command/9"
            })
        }
    }

    override fun execute(message: Message, argsLine: String) {
        val sendMessage = SendMessage()
        sendMessage.text = "Хочешь увеличить песюн без регистрации и смс? Играй в $gameTitle на" +
                " сантиметры песюна!\n" +
                "Выбери знак и жди соперника, как только соберётся 2 игрока, победитель заберёт $bet см у" +
                " проигравшего\n" +
                "Если ты уже играешь, и хочешь выйти, нажми \"выйти ❌\""
        sendMessage.chatId = message.chatId.toString()
        sendMessage.replyMarkup = keyboard
        bot.execute(sendMessage)
    }

    override fun processCallback(callbackQuery: CallbackQuery) {
        if (!shouldProcessCallback(callbackQuery.data))
            return
        val callbackChoice = callbackQuery.data.substring(command.length + 1).toInt()
        val chatId = callbackQuery.message.chatId
        val newPlayer = callbackQuery.from
        val waitingPlayer = waitingPlayers[chatId]
        if (callbackChoice == 9) {
            val answerText = if (waitingPlayer?.first?.id == newPlayer.id) {
                // playing now
                waitingPlayers.remove(chatId)
                "Ты вышел из игры $gameTitle"
            } else {
                // not playing now
                "Ты сейчас не играешь в $gameTitle"
            }
            bot.execute(AnswerCallbackQuery().apply {
                callbackQueryId = callbackQuery.id
                showAlert = false
                text = answerText
            })
            return
        }
        val dickSize = GdDao.getDick(chatId, newPlayer)?.second
        if (dickSize == null) {
            bot.execute(AnswerCallbackQuery().apply {
                callbackQueryId = callbackQuery.id
                showAlert = true
                text = "Ты не можешь играть, так как у тебя нет песюна. Получить песюн можно командой /dick"
            })
            return
        }
        if (dickSize <= bet) {
            bot.execute(AnswerCallbackQuery().apply {
                callbackQueryId = callbackQuery.id
                showAlert = true
                text =
                    "Рановато тебе ещё играть, игралка не выросла. Чтобы играть в $gameTitle, тебе нужно иметь песюн" +
                            " хотя бы ${bet + 1} см, а у тебя всего $dickSize"
            })
            return
        }
        if (waitingPlayer == null || waitingPlayer.first.id == newPlayer.id) {
            // set current player in chat
            waitingPlayers[chatId] = newPlayer to callbackChoice
            bot.execute(AnswerCallbackQuery().apply {
                callbackQueryId = callbackQuery.id
                showAlert = false
                text = "Ты выбрал ${choices[callbackChoice]}, теперь ждём второго игрока"
            })
        } else {
            // another player found, let's play
            val x = waitingPlayer.second - callbackChoice
            val result = (x * x * x - 3 * x) / 2  // fuck it, cubic parabola

            val resultMessage = "Камень, ножницы, бумага, су-е-фа!\n" +
                    "${newPlayer.mention}, ты выбрал ${choices[callbackChoice]}," +
                    " твой соперник ${waitingPlayer.first.mention} выбрал ${choices[waitingPlayer.second]} и " +
                    when {
                        result > 0 -> "победил. Он забирает $bet см твоего песюна."
                        result < 0 -> "проиграл. Ты получаешь $bet см его песюна."
                        else -> "у вас ничья. Все остаются при своих песюнах."
                    } +
                    " Никто ничё не получит, пока не будет античита. Играй снова командой /knb"
            if (antiCheat == null) {
                // todo
            } else if (result > 0) {
                GdDao.getDick(chatId, newPlayer)?.let {
                    GdDao.updateDick(chatId, newPlayer, it.first, it.second - bet)
                }
                GdDao.getDick(chatId, waitingPlayer.first)?.let {
                    GdDao.updateDick(chatId, waitingPlayer.first, it.first, it.second + bet)
                }
            } else if (result < 0) {
                GdDao.getDick(chatId, newPlayer)?.let {
                    GdDao.updateDick(chatId, newPlayer, it.first, it.second + bet)
                }
                GdDao.getDick(chatId, waitingPlayer.first)?.let {
                    GdDao.updateDick(chatId, waitingPlayer.first, it.first, it.second - bet)
                }
            }
            waitingPlayers.remove(chatId)
            storeResult(chatId, waitingPlayer.first.id, newPlayer.id, result)
            bot.sendHtmlMessage(chatId, resultMessage)

            val biggestUser: User
            val smallestUser: User
            if (waitingPlayer.first.id > newPlayer.id) {
                biggestUser = waitingPlayer.first
                smallestUser = newPlayer
            } else {
                biggestUser = newPlayer
                smallestUser = waitingPlayer.first
            }

            val history = GdDao.getKnbHistory(chatId, biggestUser.id, smallestUser.id)
            val historyPacks = TreeMap<Int, Int>()
            for (pack in history) {
                historyPacks[pack.value.currentGames] = pack.value.result
            }
            val abusedGamesCount = antiCheat.checkCheating(historyPacks)
            if (abusedGamesCount != 0) {
                val cheater = if (abusedGamesCount > 0) biggestUser else smallestUser
                val abusedSize = abs(abusedGamesCount) * bet
                GdDao.getDick(chatId, cheater)?.let {
                    GdDao.updateDick(chatId, cheater, it.first, it.second - abusedSize)
                }
                GdDao.clearKnbHistory(chatId, biggestUser.id, smallestUser.id)
                bot.sendHtmlMessage(
                    chatId, "Обнаружена попытка намеренной передачи песюна!" +
                            " Ну и ну, ${cheater.mention}, вы разочаровываете партию!" +
                            " Такие преступления наказываются очень серьёзно! " +
                            " Твой песюн уменьшен на $abusedSize см!"
                )
            }
        }
    }

    private fun storeResult(chatId: Long, userId1: Long, userId2: Long, result: Int) {
        if (userId1 > userId2) {
            // правильный порядок игроков, сохраняем как есть
            GdDao.storeKnbRound(chatId, userId1, userId2, result)
        } else {
            // "неправильный" порядок, меняем игроков и инвертируем результат
            GdDao.storeKnbRound(chatId, userId2, userId1, -result)
        }
    }
}