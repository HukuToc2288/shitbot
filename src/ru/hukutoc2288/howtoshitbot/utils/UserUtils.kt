package ru.hukutoc2288.howtoshitbot.utils

import org.telegram.telegrambots.meta.api.objects.User
import java.util.StringJoiner

val User.displayName: String
    get() {
        return when {
            userName != null -> userName
            lastName != null -> "${firstName} ${lastName}"
            else -> firstName
        }
    }

val User.mention: String
    get() {
        return if (userName != null) {
            "@${userName}"
        } else if (lastName != null) {
            "<a href=\"tg://user?id=${id}\">${firstName} ${lastName}</a>"
        } else {
            "<a href=\"tg://user?id=${id}\">${firstName}</a>"
        }
    }