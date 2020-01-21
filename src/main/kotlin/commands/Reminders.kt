package commands

import RemindersDB
import com.jessecorbett.diskord.api.rest.CreateDM
import com.jessecorbett.diskord.api.rest.CreateMessage
import com.jessecorbett.diskord.api.rest.EmbedField
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CommandSet
import com.jessecorbett.diskord.dsl.command
import com.jessecorbett.diskord.dsl.embed
import com.jessecorbett.diskord.util.Colors
import com.jessecorbett.diskord.util.authorId
import com.jessecorbett.diskord.util.sendMessage
import com.jessecorbett.diskord.util.words
import com.joestelmach.natty.Parser
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.UnstableDefault
import util.EmojiMappings
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.schedule

val isoTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
val announcementRoles = File("secrets/tokens/announcementRoles").readText().trim().split(",")
val announcementChannelData = File("secrets/tokens/announcementsChannel").readText().trim().split(",")

object Reminders : Command {
  @UnstableDefault
  override fun init(bot: Bot, prefix: CommandSet) {
    RemindersDB.getAll().forEach { it.setTimer(bot) }
    with(bot) {
      with(prefix) {
        command("reminders") {
          val embedFields = RemindersDB.getAll().filter { author.id.toLong() in it.users }
                  .map {
                    EmbedField(
                            name = "**${it.message}**",
                            value = SimpleDateFormat("dd/M/y H:mm:ss").format(isoTimeFormat.parse(it.time)),
                            inline = false
                    )
                  }
          dmUser(bot, authorId.toLong(), CreateMessage(
                  content = "",
                  embed = embed {
                    fields = embedFields as MutableList<EmbedField>
                    color = Colors.GREEN
                  }
          ))
        }
        command("remindme") {
          val query = words.drop(2).joinToString(" ")
          val date = Parser().parse(query.substringBefore("|")).firstOrNull()?.dates?.firstOrNull()
                  ?: return@command run {
                    clientStore.channels[channelId].addMessageReaction(id, EmojiMappings.cross)
                  }
          val reminder = Reminder(
                  UUID.randomUUID().toString(),
                  date,
                  query.substringAfter("|"),
                  listOf(authorId.toLong())
          )
          RemindersDB[reminder.id] = reminder
          reminder.setTimer(bot)
          clientStore.channels[channelId].addMessageReaction(id, EmojiMappings.ok)
        }

        command("announcements") {
          val embedFields = RemindersDB.getAll().filter { it.users.first() == announcementChannelData.last().toLong() }
                  .map {
                    EmbedField(
                            name = "**${it.message}**",
                            value = SimpleDateFormat("D/M/y H:mm:ss").format(isoTimeFormat.parse(it.time)),
                            inline = false
                    )
                  }
          dmUser(bot, authorId.toLong(), CreateMessage(
                  content = "",
                  embed = embed {
                    fields = embedFields as MutableList<EmbedField>
                    color = Colors.GREEN
                  }
          ))
        }

        command("announce") {
          if (clientStore.guilds[announcementChannelData.first()].getMember(authorId).roleIds
                          .intersect(announcementRoles).isEmpty()
          ) return@command run {
            clientStore.channels[channelId].addMessageReaction(id, EmojiMappings.cross)
          }

          val query = words.drop(2).joinToString(" ")
          val date = Parser().parse(query.substringBefore("|")).firstOrNull()?.dates?.firstOrNull()
                  ?: return@command run {
                    clientStore.channels[channelId].addMessageReaction(id, EmojiMappings.cross)
                  }
          val reminder = Reminder(
                  UUID.randomUUID().toString(),
                  date,
                  query.substringAfter("|"),
                  listOf(announcementChannelData.last().toLong())
          )
          RemindersDB[reminder.id] = reminder
          reminder.setTimer(bot)
          clientStore.channels[channelId].addMessageReaction(id, EmojiMappings.ok)
        }
      }
    }
  }

  suspend fun dmUser(bot: Bot, id: Long, message: CreateMessage) {
    with(bot) {
      val chatId = clientStore.discord.createDM(CreateDM(id.toString())).id
      clientStore.channels[chatId].createMessage(message)
    }
  }
}


@Serializable
data class Reminder(val id: String, val time: String, val message: String, val users: List<Long>) {
  constructor(id: String, time: Date, message: String, users: List<Long>) : this(
          id,
          isoTimeFormat.format(time),
          message,
          users
  )

  @UnstableDefault
  fun setTimer(bot: Bot) {
    Timer(UUID.randomUUID().toString(), false).schedule(
            isoTimeFormat.parse(time)
    ) {
      runBlocking {
        users.forEach {
          if (it == announcementChannelData.last().toLong()) {
            bot.clientStore.channels[announcementChannelData.last()].sendMessage("@everyone $message")
          } else {
            Reminders.dmUser(bot, it, CreateMessage(message))
          }
        }
        RemindersDB -= this@Reminder.id
      }
    }
  }
}
