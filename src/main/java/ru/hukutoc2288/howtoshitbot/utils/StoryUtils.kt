package ru.hukutoc2288.howtoshitbot.utils

import ru.hukutoc2288.howtoshitbot.entinies.stories.Story
import java.util.*

object StoryUtils {

    fun checkStory(story: Story): Boolean {
        return story.expireAfter.after(Date())
    }

    const val storyLifetime = 86400
}