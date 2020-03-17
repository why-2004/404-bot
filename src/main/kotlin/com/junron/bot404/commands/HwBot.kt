package com.junron.bot404.commands

import com.hwboard.DiscordUser
import com.hwboard.HomeworkNullable
import com.hwboard.Subject
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CommandSet
import com.jessecorbett.diskord.dsl.command
import com.jessecorbett.diskord.dsl.embed
import com.jessecorbett.diskord.util.Colors
import com.jessecorbett.diskord.util.authorId
import com.jessecorbett.diskord.util.words
import com.junron.bot404.config
import com.junron.bot404.util.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.UnstableDefault
import java.util.*


object HwBot : Command {

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
          val homework = getHomework().getOrNull(index)
                  ?: return@command bot.reject(this, "$index is not a valid homework index.")
          if (words.getOrNull(3) == "--force") {
            deleteHomework(index)
            updatePermanent(bot)
            return@command bot accept this
          }
          with(Conversation(HomeworkNullable())) {
            init(bot, channelId, listOf(TextQuestion(
                    "Are you sure you want to delete '${homework.text}'? Type 'y' to confirm.") {
              if (it.toLowerCase().trim() == "y") {
                if (!deleteHomework(index)) bot reject this@command
                next()
                updatePermanent(bot)
                return@TextQuestion
              }
              bot.reject(this@command, "Cancelled")
              cancel()
            }, Done("Homework deleted") {}))
          }
        }

        command("add") {
          if (clientStore.guilds[config.guild].getMember(authorId).roleIds
                          .intersect(config.adminRoleIds).isEmpty() || guildId != null
          ) return@command bot reject this
          with(Conversation(HomeworkNullable())) {
            init(bot, channelId, listOf(
                    ChoiceQuestion("Select subject: ", config.subjects) {
                      state = state.copy(subject = Subject(it))
                      next()
                    },
                    DateQuestion("Enter due date: ", true) {
                      state = state.copy(dueDate = it.toDate())
                      next()
                    },
                    TextQuestion("Enter homework text: ") {
                      state = state.copy(text = it)
                      next()
                    },
                    MultipleChoiceQuestion("Please enter tags numbers, separated by commas. Enter '-' for no tags.", tags.map { it.name }) {
                      state = state.copy(tags = it.map { tagName ->
                        tags.find { tag -> tag.name == tagName }!!
                      })
                      next()
                    },
                    Done("Homework added successfully") {
                      state = state.copy(
                              lastEditPerson = DiscordUser(author.username, authorId, read = true, write = true),
                              lastEditTime = Date().toDate())
                      addHomework(state.toHomework())
                      updatePermanent(bot)
                    }))
          }
        }
      }
    }
  }
}

@Serializable
data class PermanentMessage(val channel: String, val messageId: String)
