package com.junron.bot404.commands

import com.github.shyiko.skedule.Schedule
import com.jessecorbett.diskord.api.rest.CreateMessage
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CommandSet
import com.jessecorbett.diskord.dsl.command
import com.jessecorbett.diskord.util.authorId
import com.junron.bot404.util.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.UnstableDefault
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.*
import kotlin.concurrent.fixedRateTimer

@Serializable
data class Time(val hour: Int, val minute: Int)

@Serializable
data class Subscriber(val userName: String, val authorId: String, val timings: List<Time>)

object Temperature : Command {
  private val subscribers = Storage("temp_subscribers", Subscriber.serializer())
  lateinit var timers: List<Timer>
  override fun init(bot: Bot, prefix: CommandSet) {
    with(bot) {
      with(prefix) {
        init(bot)
        command("subscribe") {
          println("ok")
          if (guildId != null) return@command bot reject this
          subscribers += Subscriber(author.username, authorId, listOf(Time(8, 0)))
          reply("""You will be reminded to submit your temperature at 8am.
            `!temperature config` to change reminder time
            `!temperature reminders` to list reminders""".trimIndent())
        }
        command("config") {
          if (guildId != null) return@command bot reject this
          val subscriberId = subscribers.find { it.item.authorId == authorId }
                  ?.id
                  ?: return@command run {
                    reply("You are not subscribed. Send `!temperature subscribe` to subscribe.")
                  }
          with(Conversation(listOf<Time>())) {
            val question = TimeQuestion("Please enter reminder time (24 hour hh:mm)") {
              state += it
              next()
            }
            lateinit var hasNext: BooleanQuestion
            hasNext = BooleanQuestion("Add another reminder? ") {
              if(it){
                addQuestion(question)
                addQuestion(hasNext)
              }
              next()
            }
            init(bot, channelId, listOf(question, hasNext,
                    Done("Reminders updated!") {
                      subscribers -= subscriberId
                      subscribers += Subscriber(author.username, authorId, state)
                      reply("**Reminders**\n" + state.joinToString("\n") {
                        "${it.hour.toString().padStart(2, '0')}:${it.minute.toString().padStart(2, '0')}"
                      })
                    }))
          }
        }
        command("reminders") {
          val subscriber = subscribers.find { it.item.authorId == authorId }
                  ?: return@command run {
                    reply("You are not subscribed. Send `!temperature subscribe` to subscribe.")
                  }
          reply("**Reminders**\n" + subscriber.item.timings.joinToString("\n") {
            "${it.hour.toString().padStart(2, '0')}:${it.minute.toString().padStart(2, '0')}"
          })
        }
      }
    }
  }

  @UnstableDefault
  fun init(bot: Bot) {
    if (::timers.isInitialized) timers.forEach { it.cancel() }
    val timings = mutableMapOf<Time, MutableList<String>>()
    subscribers.forEach { (_, subscriber) ->
      subscriber.timings.forEach {
        if (timings.containsKey(it)) {
          timings[it]?.plusAssign(subscriber.authorId)
        } else {
          timings[it] = mutableListOf(subscriber.authorId)
        }
      }
    }
    timers = timings.map { (time, subscribers) ->
      fixedRateTimer(
              UUID.randomUUID().toString(),
              false,
              (Schedule.at(LocalTime.of(time.hour, time.minute))
                      .everyDay()
                      .next(ZonedDateTime.now())
                      .toEpochSecond() - ZonedDateTime.now().toEpochSecond()) * 1000,
              8.64e+7.toLong()
      ) {
        subscribers
                .forEach {
                  runBlocking {
                    Reminders.dmUser(bot, it, CreateMessage(content = "Please take your temperature. https://forms.office.com/Pages/ResponsePage.aspx?id=cnEq1_jViUiahddCR1FZKi_YUnieBUBCi4vce5KjIHVUMkoxVUdBMVo2VUJTNFlSU1dFNEtNWUwxNS4u"))
                  }
                }
      }
    }
  }
}

