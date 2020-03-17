package com.junron.bot404.util

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.api.rest.EmbedField
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.embed
import com.jessecorbett.diskord.util.Colors
import com.jessecorbett.diskord.util.isFromUser
import com.jessecorbett.diskord.util.sendMessage
import kotlinx.serialization.Serializable
import java.util.*


class Conversation<T>(
        var state: T
) {
  private lateinit var channelId: String
  private lateinit var bot: Bot
  private var questionNumber = -1
  private var active = true
  private var questions: List<Question<*>> = emptyList()

  suspend fun init(bot: Bot, channelId: String, questions: List<Question<*>>) {
    this.bot = bot
    this.channelId = channelId
    this.questions = questions
    bot.messageCreated {
      if (!it.isFromUser || it.channelId != channelId || !active) return@messageCreated
      if (it.content.trim() == "!cancel") return@messageCreated run {
        bot.clientStore.channels[channelId].sendMessage("Cancelled")
        cancel()
      }
      questions[questionNumber].handleMessage(it)
    }
    next()
  }

  suspend fun next() {
    questionNumber++
    val question = questions[questionNumber]
    question.init(bot, channelId)
    question.sendMessage()
    if (questionNumber == questions.lastIndex) {
      (question as? Done)?.callback?.invoke(Unit)
      active = false
    }
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
  abstract suspend fun handleMessage(message: Message)
}

class TextQuestion(override val message: String, override val callback: suspend (String) -> Unit) : Question<String>() {
  override suspend fun handleMessage(message: Message) {
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

class DateQuestion(override val message: String, private val future: Boolean, override val callback: suspend (Date) -> Unit) : Question<Date>() {
  override suspend fun handleMessage(message: Message) {
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

@Serializable
class ChoiceQuestion(override val message: String, private val options: List<String>, override val callback: suspend (String) -> Unit) : Question<String>() {
  override suspend fun handleMessage(message: Message) {
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
      fields = mutableListOf(EmbedField("Options", options.mapIndexed { index, s ->
        "`$index` | $s"
      }.joinToString("\n"), false))
    })
  }
}

class MultipleChoiceQuestion(override val message: String, private val options: List<String>, override val callback: suspend (List<String>) -> Unit) : Question<List<String>>() {
  override suspend fun handleMessage(message: Message) {
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
            .map { options[it] })

  }

  override suspend fun sendMessage() {
    bot.clientStore.channels[channelId].sendMessage(message, embed {
      color = Colors.CYAN
      fields = mutableListOf(EmbedField("Options", options.mapIndexed { index, s ->
        "`$index` | $s"
      }.joinToString("\n"), false))
    })
  }
}

class Done(override val message: String, override val callback: suspend (Unit) -> Unit) : Question<Unit>() {
  override suspend fun sendMessage() {
    bot.clientStore.channels[channelId].sendMessage(message)
  }

  override suspend fun handleMessage(message: Message) {
  }
}
