package ru.hukutoc2288.howtoshitbot.commands

import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.hukutoc2288.howtoshitbot.bot
import ru.hukutoc2288.howtoshitbot.dao.GdDao
import ru.hukutoc2288.howtoshitbot.utils.CommandFunction
import ru.hukutoc2288.howtoshitbot.utils.mention


private val gameTitle = "\"Камень, ножницы, бумага\""
object KnbCommand : CommandFunction("knb", "сыграть в игру $gameTitle") {

    val gameTitle = "\"Камень, ножницы, бумага\""
    private val choices = arrayOf("камень ✊", "ножницы ✌", "бумага \uD83E\uDD1A")
    val waitingPlayers = HashMap<Long, Pair<User, Int>>()
    val bet = 5


    override fun execute(message: Message, argsLine: String) {
        val keyboard = InlineKeyboardMarkup(listOf(arrayListOf(), arrayListOf())).apply {
            for (i in choices.indices) {
                keyboard[i / 2].add(InlineKeyboardButton(choices[i]).apply {
                    callbackData = "$command/$i"
                })
            }
            keyboard[1].add(InlineKeyboardButton("выйти ❌").apply {
                callbackData = "$command/9"
            })
        }
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
            // fuck it, cubic parabola
            val result = x * x * x - 3 * x
            val resultMessage = "Камень, ножницы, бумага, су-е-фа!\n" +
                    "${newPlayer.mention}, ты выбрал ${choices[callbackChoice]}," +
                    " твой соперник ${waitingPlayer.first.mention} выбрал ${choices[waitingPlayer.second]} и " +
                    when {
                        result > 0 -> "победил. Он забирает $bet см твоего песюна."
                        result < 0 -> "проиграл. Ты получаешь $bet см его песюна."
                        else -> "у вас ничья. Все остаются при своих песюнах."
                    } +
                    " играй снова командой /knb"
            if (result > 0){
                GdDao.getDick(chatId,newPlayer)?.let {
                    GdDao.updateDick(chatId,newPlayer,it.first,it.second-bet)
                }
                GdDao.getDick(chatId,waitingPlayer.first)?.let {
                    GdDao.updateDick(chatId,waitingPlayer.first,it.first,it.second+bet)
                }
            } else if (result < 0){
                GdDao.getDick(chatId,newPlayer)?.let {
                    GdDao.updateDick(chatId,newPlayer,it.first,it.second+bet)
                }
                GdDao.getDick(chatId,waitingPlayer.first)?.let {
                    GdDao.updateDick(chatId,waitingPlayer.first,it.first,it.second-bet)
                }
            }
            waitingPlayers.remove(chatId)
            bot.sendHtmlMessage(chatId, resultMessage)
        }
        //bot.sendTextMessage(callbackQuery.me.ssage.chatId, callbackQuery.data)
    }
}