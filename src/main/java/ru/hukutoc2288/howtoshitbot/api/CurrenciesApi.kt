package ru.hukutoc2288.howtoshitbot.api

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import ru.hukutoc2288.howtoshitbot.entinies.currencies.CurrenciesList

interface CurrenciesApi {
    @GET("/daily_json.js")
    fun getRates(): Call<CurrenciesList>
}