package com.junron.bot404.util

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CommandSet
import com.jessecorbett.diskord.dsl.command
import com.jessecorbett.diskord.util.authorId
import com.junron.bot404.model.Subscriber
import com.junron.pyrobase.jsoncache.Storage

object Subscriptions {
  fun <T : Subscriber> init(
          bot: Bot,
          commandSet: CommandSet,
          subscribers: Storage<T>,
          reminders: ScheduledReminders<T>,
          subscribeMessage: String = "You have subscribed.",
          unsubscribedMessage: String = "You are unsubscribed.",
          createSubscriber: (message: Message) -> T
  ) {
    with(bot) {
      with(commandSet) {
        command("subscribe") {
          if (guildId != null) return@command bot reject this
          val subscriberId = subscribers.find { it.authorId == authorId }
                  ?.id
          if (subscriberId != null) {
            reply("You have already subscribed.")
            return@command
          }
          val subscriber = createSubscriber(this)
          subscribers += subscriber
          reminders.updateSubscriptions(subscribers)
          reply(subscribeMessage)
        }
        command("unsubscribe") {
          if (guildId != null) return@command bot reject this
          val subscriberId = subscribers.find { it.authorId == authorId }
                  ?.id
          if (subscriberId == null) {
            reply(unsubscribedMessage)
            return@command
          }
          subscribers -= authorId
          reminders.updateSubscriptions(subscribers)
          reply("You have unsubscribed.")
        }
        command("reminders") {
          val subscriber = subscribers.find { it.authorId == authorId }
                  ?: return@command run {
                    reply(unsubscribedMessage)
                  }
          reply("**Reminders**\n" + subscriber.timings.joinToString("\n") {
            "${it.hour.toString().padStart(2, '0')}:${it.minute.toString().padStart(2, '0')}"
          })
        }
      }
    }
  }
}
