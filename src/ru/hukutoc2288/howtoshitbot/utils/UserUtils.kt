package ru.hukutoc2288.howtoshitbot.utils

import org.telegram.telegrambots.meta.api.objects.User

val User.displayName: String
    get() {
        return when {
            userName != null -> userName
            lastName != null -> "${firstName} ${lastName}"
            else -> firstName
        }
    }