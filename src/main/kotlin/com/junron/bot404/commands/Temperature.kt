package com.junron.bot404.commands

import com.github.shyiko.skedule.Schedule
import com.jessecorbett.diskord.api.rest.CreateMessage
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CommandSet
import com.jessecorbett.diskord.dsl.command
import com.jessecorbett.diskord.util.authorId
import com.junron.bot404.util.Reminders
import com.junron.bot404.util.Storage
import com.junron.bot404.util.accept
import com.junron.bot404.util.reject
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.builtins.serializer
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.*
import kotlin.concurrent.fixedRateTimer


object Temperature : Command {
  val subscribers = Storage("temp_subscribers", Long.serializer())

  override fun init(bot: Bot, prefix: CommandSet) {
    with(prefix) {
      init(bot)
      command("temperatureSubscribe") {
        if (guildId != null) return@command bot reject this
        subscribers += authorId.toLong()
        bot accept this
      }
    }
  }

  @UnstableDefault
  fun init(bot: Bot) {
    fixedRateTimer(
            UUID.randomUUID().toString(),
            false,
            (Schedule.at(LocalTime.of(8, 30))
                    .everyDay()
                    .next(ZonedDateTime.now())
                    .toEpochSecond() - ZonedDateTime.now().toEpochSecond()) * 1000,
            8.64e+7.toLong()
    ) {
      subscribers.forEach {
        runBlocking {
          Reminders.dmUser(bot, it.item.toString(), CreateMessage(content = "Please take your temperature now. https://forms.office.com/Pages/ResponsePage.aspx?id=cnEq1_jViUiahddCR1FZKi_YUnieBUBCi4vce5KjIHVUMkoxVUdBMVo2VUJTNFlSU1dFNEtNWUwxNS4u"))
        }
      }
    }
  }
}

