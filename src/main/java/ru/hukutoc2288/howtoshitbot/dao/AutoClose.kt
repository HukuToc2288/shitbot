package ru.hukutoc2288.howtoshitbot.dao

// Yes, I know about try-with-resources. I simply don't like it
class AutoClose {

    private val resources = ArrayList<AutoCloseable?>()

    fun <T : AutoCloseable?> use(closeable: T): T {
        resources.add(closeable)
        return closeable
    }

    fun closeResources() {
        resources.asReversed().forEach { resource ->
            resource?.let {
                try {
                    it.close()
                } catch (e: Exception) {
                    // and what?
                }
            }
        }
        resources.clear()
    }
}

fun<T: AutoCloseable?> T.autoClose(ac: AutoClose): T {
    ac.use(this)
    return this
}