package com.junron.bot404.commands

import com.hwboard.DiscordUser
import com.hwboard.HomeworkNullable
import com.hwboard.Subject
import com.jessecorbett.diskord.api.rest.EmbedField
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CommandSet
import com.jessecorbett.diskord.dsl.command
import com.jessecorbett.diskord.dsl.embed
import com.jessecorbett.diskord.util.Colors
import com.jessecorbett.diskord.util.authorId
import com.jessecorbett.diskord.util.sendMessage
import com.jessecorbett.diskord.util.words
import com.junron.bot404.config
import com.junron.bot404.util.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.UnstableDefault
import java.util.*


object HwBot : Command {

  private val state = mutableMapOf<String, HomeworkNullable>()

  @UnstableDefault
  override fun init(bot: Bot, prefix: CommandSet) {

    with(bot) {
      with(prefix) {
        init(bot)
        command("show") {
          val homework = getHomework()
          reply("", embed = combineEmbeds(buildHomeworkEmbeds(homework)))
        }
        command("permanent") {
          val homework = getHomework()
          val id = reply("", embed = combineEmbeds(buildHomeworkEmbeds(homework))).id
          permanentMessageStorage += PermanentMessage(channelId, id)
        }
        command("tomorrow") {
          val homework = getHomework().tomorrow
          val embed = buildHomeworkEmbeds(homework).firstOrNull() ?: embed {
            title = "There is no homework tomorrow"
            color = Colors.GREEN
          }
          reply("", embed = embed)
        }
        command("subscribe") {
          if (guildId != null) return@command bot reject this
          subscribers += authorId.toLong()
          bot accept this
        }
        command("delete") {
          if (clientStore.guilds[config.guild].getMember(authorId).roleIds
                          .intersect(config.adminRoleIds).isEmpty() || guildId != null
          ) return@command bot reject this
          val index = words.getOrNull(2)?.toIntOrNull()
                  ?: return@command bot reject this
          if (!deleteHomework(index)) return@command bot reject this
          bot accept this
          updatePermanent(bot)
        }

        command("add") {
          if (clientStore.guilds[config.guild].getMember(authorId).roleIds
                          .intersect(config.adminRoleIds).isEmpty() || guildId != null
          ) return@command bot reject this
          val processState = state[authorId]
          if (processState == null) {
            state[authorId] = HomeworkNullable(id = UUID.randomUUID().toString())
            reply("Please enter subject number: ", embed {
              color = Colors.CYAN
              fields = mutableListOf(EmbedField("Subjects", config.subjects.mapIndexed { index, s ->
                "`$index` | $s"
              }.joinToString("\n"), false))
            })
          } else {
            return@command bot reject this
          }
        }
      }
      messageCreated { it ->
        val userState = state[it.authorId] ?: return@messageCreated
        when {
          userState.subject == null -> {
            val index = it.content.toIntOrNull()
                    ?: return@messageCreated run {
                      clientStore.channels[it.channelId].sendMessage("Please enter a number between 0 and ${config.subjects.lastIndex}")
                    }
            if (index !in 0..(config.subjects.lastIndex))
              return@messageCreated run {
                clientStore.channels[it.channelId].sendMessage("Please enter a number between 0 and ${config.subjects.lastIndex}")
              }
            state[it.authorId] = userState.copy(subject = Subject(config.subjects[index]))
            clientStore.channels[it.channelId].sendMessage("Please enter due date.")
          }
          userState.dueDate == null -> {
            val date = parseDate(it.content)
                    ?: return@messageCreated run {
                      clientStore.channels[it.channelId].sendMessage("Please enter a valid due date.")
                    }
            state[it.authorId] = userState.copy(dueDate = date.toDate())
            clientStore.channels[it.channelId].sendMessage("Please enter homework text.")
          }
          userState.text.isEmpty() -> {
            val text = it.content
            state[it.authorId] = userState.copy(text = text)
            clientStore.channels[it.channelId].sendMessage("Please enter tags numbers, separated by commas. Enter '-' for no tags.", embed {
              color = Colors.CYAN
              fields = mutableListOf(EmbedField("Tags", tags.mapIndexed { index, s ->
                "`$index` | ${s.name}"
              }.joinToString("\n"), false))
            })
          }
          userState.tags == null -> {
            state[it.authorId] = userState.copy(tags = it.content.split(",")
                    .map { it.trim().toIntOrNull() }
                    .filter { it in 0..tags.lastIndex }
                    .map { tags[it!!] })
            state[it.authorId] = state[it.authorId]!!.copy(
                    lastEditPerson = DiscordUser(it.author.username, it.authorId, read = true, write = true),
                    lastEditTime = Date().toDate()
            )
            addHomework(state[it.authorId]!!.toHomework())
            updatePermanent(bot)
            state.remove(it.authorId)
            clientStore.channels[it.channelId].sendMessage("Homework added successfully")
          }
        }
      }
    }
  }
}

@Serializable
data class PermanentMessage(val channel: String, val messageId: String)
