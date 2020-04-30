package com.junron.bot404.util

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.api.rest.EmbedField
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.embed
import com.jessecorbett.diskord.util.Colors
import com.jessecorbett.diskord.util.isFromUser
import com.jessecorbett.diskord.util.sendMessage
import com.junron.bot404.commands.Time
import com.junron.bot404.model.Homework
import com.junron.bot404.model.HomeworkNullable
import kotlinx.serialization.Serializable
import java.util.*


class Conversation<T>(
    var state: T
) {
    private lateinit var channelId: String
    private lateinit var bot: Bot
    private var questionNumber = -1
    private var active = true
        set(value) {
            field = value
            if (!value) activeConversations -= channelId
        }
    private var questions: MutableList<Question<*>> = mutableListOf()

    companion object {
        val activeConversations = mutableListOf<String>()
    }

    suspend fun init(
        bot: Bot,
        channelId: String,
        questions: List<Question<*>>
    ) {
        this.bot = bot
        this.channelId = channelId
        if (channelId in activeConversations) {
            bot.clientStore.channels[channelId].sendMessage("You already have an active conversation.\nType `!cancel` to cancel that conversation.")
            return
        }
        activeConversations += channelId
        this.questions = questions.toMutableList()
        bot.messageCreated {
            if (!it.isFromUser || it.channelId != channelId || !active) return@messageCreated
            if (it.content.trim() == "!cancel") return@messageCreated run {
                bot.clientStore.channels[channelId].sendMessage("Cancelled")
                cancel()
            }
            this.questions[questionNumber].handleMessage(it, state)
        }
        next()
    }

    suspend fun next() {
        questionNumber++
        val question = questions[questionNumber]
        question.init(bot, channelId)
        question.sendMessage()
        if (questionNumber == questions.lastIndex && question is Done) {
            question.callback(Unit)
            active = false
        }
    }

    fun addQuestion(question: Question<*>) {
        val doneIndex = with(questions.indexOfLast { it is Done }) {
            if (this == -1) questions.size
            else this
        }
        questions.add(maxOf(0, doneIndex), question)
    }

    fun cancel() {
        active = false
    }
}

abstract class Question<T> {
    abstract val message: String
    protected lateinit var bot: Bot
    protected lateinit var channelId: String
    abstract val callback: suspend (T) -> Unit
    open fun init(bot: Bot, channelId: String) {
        this.bot = bot
        this.channelId = channelId
    }

    abstract suspend fun sendMessage()
    abstract suspend fun handleMessage(message: Message, state: Any?)
}

class TextQuestion(
    override val message: String,
    override val callback: suspend (String) -> Unit
) : Question<String>() {
    override suspend fun handleMessage(message: Message, state: Any?) {
        if (message.content.isBlank()) run {
            bot.clientStore.channels[channelId].sendMessage("Text cannot be blank")
            return@handleMessage
        }
        callback(message.content.trim())
    }

    override suspend fun sendMessage() {
        bot.clientStore.channels[channelId].sendMessage(message)
    }

}

class DateQuestion(
    override val message: String,
    private val future: Boolean,
    override val callback: suspend (Date) -> Unit
) : Question<Date>() {
    override suspend fun handleMessage(message: Message, state: Any?) {
        if (message.content.trim().toLowerCase() == "next lesson") {
            val subject = when {
                state as? Homework != null -> {
                    state.subject
                }
                state as? HomeworkNullable != null -> {
                    state.subject ?: run {
                        bot.clientStore.channels[channelId].sendMessage("That doesn't seem like a valid date.")
                        return@handleMessage
                    }
                }
                else -> run {
                    bot.clientStore.channels[channelId].sendMessage("That doesn't seem like a valid date.")
                    return@handleMessage
                }
            }
            callback(Timetable.getNextLesson(subject) ?: run {
                bot.clientStore.channels[channelId].sendMessage("That doesn't seem like a valid date.")
                return@handleMessage
            })
            return
        }
        val date = parseDate(message.content) ?: run {
            bot.clientStore.channels[channelId].sendMessage("That doesn't seem like a valid date.")
            return@handleMessage
        }
        if (future) {
            if (!date.isFuture()) run {
                bot.clientStore.channels[channelId].sendMessage("Date must be in the future.")
                return@handleMessage
            }
            callback(date)
        } else {
            callback(date)
        }
    }


    override suspend fun sendMessage() {
        bot.clientStore.channels[channelId].sendMessage(message)
    }
}

class TimeQuestion(
    override val message: String,
    override val callback: suspend (Time) -> Unit
) : Question<Time>() {
    override suspend fun handleMessage(message: Message, state: Any?) {
        val parts = message.content.split(":").mapNotNull {
            it.trim().toIntOrNull()
        }
        if (parts.size != 2 || parts[0] !in 0..23 || parts[1] !in 0..59) run {
            bot.clientStore.channels[channelId].sendMessage("Please enter time in 24 hour hh:mm format.")
            return@handleMessage
        }
        callback(Time(parts[0], parts[1]))
    }

    override suspend fun sendMessage() {
        bot.clientStore.channels[channelId].sendMessage(message)
    }

}

class BooleanQuestion(
    override val message: String,
    private val default: Boolean = false,
    override val callback: suspend (Boolean) -> Unit
) : Question<Boolean>() {
    override suspend fun handleMessage(message: Message, state: Any?) {
        val content = message.content.toLowerCase()
        if (content == "y" || (default && content != "n")) return callback(true)
        callback(false)
    }

    override suspend fun sendMessage() {
        bot.clientStore.channels[channelId].sendMessage(message + if (default) "[Y/n]" else "[N/y]")
    }

}

@Serializable
class ChoiceQuestion(
    override val message: String,
    private val options: List<String>,
    override val callback: suspend (String) -> Unit
) : Question<String>() {
    override suspend fun handleMessage(message: Message, state: Any?) {
        callback(message.content.trim().toIntOrNull()?.let { index ->
            options.getOrNull(index)
        } ?: run {
            bot.clientStore.channels[channelId].sendMessage("'${message.content}' is not a valid option number.")
            return@handleMessage
        })
    }


    override suspend fun sendMessage() {
        bot.clientStore.channels[channelId].sendMessage(message, embed {
            color = Colors.CYAN
            fields = mutableListOf(
                EmbedField(
                    "Options",
                    options.mapIndexed { index, s ->
                        "`$index` | $s"
                    }.joinToString("\n"),
                    false
                )
            )
        })
    }
}

class MultipleChoiceQuestion(
    override val message: String,
    private val options: List<String>,
    override val callback: suspend (List<String>) -> Unit
) : Question<List<String>>() {
    override suspend fun handleMessage(message: Message, state: Any?) {
        if (message.content.trim() == "-") return callback(emptyList())
        val selected = message.content.split(",")
        callback(selected
            .map { it.trim().toIntOrNull() }
            .mapIndexed { index, it ->
                if (it !in 0..options.lastIndex) {
                    bot.clientStore.channels[channelId].sendMessage("'${selected[index]}' is not a valid option number.")
                    return@handleMessage
                }
                it!!
            }
            .distinct()
            .map { options[it] })

    }

    override suspend fun sendMessage() {
        bot.clientStore.channels[channelId].sendMessage(message, embed {
            color = Colors.CYAN
            fields = mutableListOf(
                EmbedField(
                    "Options",
                    options.mapIndexed { index, s ->
                        "`$index` | $s"
                    }.joinToString("\n"),
                    false
                )
            )
        })
    }
}

class Done(
    override val message: String,
    override val callback: suspend (Unit) -> Unit
) : Question<Unit>() {
    override suspend fun sendMessage() {
        bot.clientStore.channels[channelId].sendMessage(message)
    }

    override suspend fun handleMessage(message: Message, state: Any?) {
    }
}
