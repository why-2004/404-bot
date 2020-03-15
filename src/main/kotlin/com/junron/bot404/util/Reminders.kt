package com.junron.bot404.util

import com.jessecorbett.diskord.api.rest.CreateDM
import com.jessecorbett.diskord.api.rest.CreateMessage
import com.jessecorbett.diskord.dsl.Bot
import com.junron.bot404.util.Reminders.remindersStorage
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.UnstableDefault
import java.util.*
import kotlin.concurrent.schedule

object Reminders {
  val remindersStorage = Storage("reminders", Reminder.serializer())

  suspend fun dmUser(bot: Bot, userId: String, message: CreateMessage) {
    with(bot) {
      val chatId = clientStore.discord.createDM(CreateDM(userId)).id
      clientStore.channels[chatId].createMessage(message)
    }
  }
}

@Serializable
data class Reminder(
        val time: String,
        val message: String,
        val users: List<String>
) {
  @UnstableDefault
  fun setTimer(bot: Bot, id: String) {
    Timer(uuid(), false).schedule(
            isoTimeFormat.parse(time)
    ) {
      runBlocking {
        users.forEach {
          Reminders.dmUser(bot, it, CreateMessage(message))
        }
        remindersStorage -= id
      }
    }
  }
}
