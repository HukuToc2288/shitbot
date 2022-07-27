package ru.hukutoc2288.howtoshitbot

import ru.hukutoc2288.howtoshitbot.dao.GdDao
fun main() {
    val gdDao = GdDao()
    //gdDao.addUserToChat(1,1)
    println(gdDao.isUserInChat(1,1))
}