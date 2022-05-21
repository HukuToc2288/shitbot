package ru.hukutoc2288.howtoshitbot.entinies.gayofday

data class GdChat(
    var id: Long = 0,
    var users: ArrayList<GdUser>? = null,
    var lastTime: Long = 0,
    var gayIndex: Int = 0
) {
    fun getOrNull(displayName: String, id: Long?): GdUser? {
        val immutableUsers = users ?: ArrayList<GdUser>().also {
            users = it
        }
        if (id != null) {
            // we definitely should search by id if we can
            for (user in immutableUsers) {
                if (user.id == id) {
                    return user
                }
            }
        }

        // hence try to find by displayName
        for (user in immutableUsers) {
            if (user.displayName == displayName) {

                return user
            }
        }
        return null
    }

    fun getOrCreate(displayName: String, id: Long?): GdUser {
        val userOrNull = getOrNull(displayName, id)
        if (userOrNull == null) {
            val newUser = GdUser(displayName, id)
            users!!.add(newUser)
            return newUser
        }
        return userOrNull
    }
}