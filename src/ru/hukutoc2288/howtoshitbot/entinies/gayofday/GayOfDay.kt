package ru.hukutoc2288.howtoshitbot.entinies.gayofday

data class GayOfDay(
    var chats: ArrayList<GdChat>? = null
) {
    fun getOrCreate(id: Long): GdChat {
        val chatOrNull = getOrNull(id)
        if (chatOrNull == null) {
            val newChat = GdChat(id, ArrayList())
            chats!!.add(newChat)
            return newChat
        }
        return chatOrNull
    }

    fun getOrNull(id: Long): GdChat? {
        val immutableChats = chats ?: ArrayList<GdChat>().also {
            chats = it
        }
        for (chat in immutableChats) {
            if (chat.id == id)
                return chat
        }
        return null
    }
}