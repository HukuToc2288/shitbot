package ru.hukutoc2288.howtoshitbot.commands

import org.telegram.telegrambots.meta.api.objects.Message
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import ru.hukutoc2288.howtoshitbot.api.CurrenciesApi
import ru.hukutoc2288.howtoshitbot.bot
import ru.hukutoc2288.howtoshitbot.entinies.currencies.CurrenciesList
import ru.hukutoc2288.howtoshitbot.utils.CommandFunction
import ru.hukutoc2288.howtoshitbot.utils.CurrenciesAliases
import java.util.*

object ExchangeRateCommand : CommandFunction(
    "rate",
    "<валюта> курс валюты. Для получения списка доступных валют, отправьте команду без параметров",
    arrayOf("курс")
) {
    private val currenciesApi =
        Retrofit.Builder().baseUrl("https://www.cbr-xml-daily.ru/")
            .addConverterFactory(JacksonConverterFactory.create())
            .build().create(CurrenciesApi::class.java)


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
    
    override fun execute(message: Message, argsLine: String) {
        val chatId = message.chatId
        val replyTo = message.messageId
        val shitAliases = arrayOf("говно", "говна")
        if (argsLine.lowercase(Locale.getDefault()) in shitAliases) {
            bot.sendTextMessage(chatId, "курс говна пашол нахуй",replyTo)
            return
        }
        currenciesApi.getRates().enqueue(object : Callback<CurrenciesList> {
            override fun onResponse(
                call: Call<CurrenciesList>,
                response: Response<CurrenciesList>
            ) {
                if (!response.isSuccessful) {
                    bot.onError(chatId, response.errorBody()!!.string())
                    return
                }
                val currenciesList = response.body()
                if (currenciesList == null) {
                    bot.onError(chatId, "Не удалось получить тело запроса: ${response.code()}")
                    return
                }
                if (argsLine.isEmpty()) {
                    val currenciesIterator = currenciesList.valute?.fields()
                    var availableCurrencies = "RUB, SRU"
                    while (currenciesIterator?.hasNext() == true) {
                        availableCurrencies += ", ${currenciesIterator.next().key}"
                    }
                    bot.sendTextMessage(
                        chatId,
                        "Доступны следующие курсы: $availableCurrencies\n" +
                                "Также доступны человеческие названия:${CurrenciesAliases.string()}",
                        replyTo
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
                    bot.sendTextMessage(
                        chatId,
                        "курс валюты $argsLine не наличествует. Доступны следующие курсы: $availableCurrencies\n" +
                                "Также доступны человеческие названия:${CurrenciesAliases.string()}",
                        replyTo
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
                bot.sendTextMessage(
                    chatId,
                    "${currency.nominal} ${currency.name} (${currency.charCode}) стоит %.4f рублей".format(currency.value),
                    replyTo
                )
            }

            override fun onFailure(call: Call<CurrenciesList>, t: Throwable) {
                t.printStackTrace()
                bot.onError(chatId, t.message)
            }

        })
    }
}