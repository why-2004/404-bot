package com.junron.bot404.commands

import com.jessecorbett.diskord.dsl.*
import com.jessecorbett.diskord.util.Colors
import com.jessecorbett.diskord.util.authorId
import com.jessecorbett.diskord.util.words
import com.junron.bot404.config
import com.junron.bot404.model.HomeworkNullable
import com.junron.bot404.util.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.UnstableDefault
import java.util.*


object HwBot : Command {

  @ExperimentalStdlibApi
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
          val homework = getHomework().sortedBy { it.dueDate }.getOrNull(index)
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

        command("info") {
          val index = words.getOrNull(2)?.toIntOrNull()
                  ?: return@command bot reject this
          val homework = getHomework().sortedBy { it.dueDate }.getOrNull(index)
                  ?: return@command bot.reject(this, "$index is not a valid homework index.")
          reply("", embed = homework.generateEmbed())
        }

        command("edit") {
          if (clientStore.guilds[config.guild].getMember(authorId).roleIds
                          .intersect(config.adminRoleIds).isEmpty() || guildId != null
          ) return@command bot reject this
          val index = words.getOrNull(2)?.toIntOrNull()
                  ?: return@command bot reject this
          val homework = getHomework().sortedBy { it.dueDate }.getOrNull(index)
                  ?: return@command bot.reject(this, "$index is not a valid homework index.")
          val fields = listOf("Subject", "Text", "Due date", "Tags")
          reply("Editing ${homework.text}")
          with(Conversation(homework)) {
            init(bot, channelId, listOf(MultipleChoiceQuestion("Select fields you want to edit, separated by commas.", fields) { selectedFields ->
              if ("Subject" in selectedFields) addQuestion(ChoiceQuestion("Select subject: ", config.subjects) {
                state = state.copy(subject = it)
                next()
              })
              if ("Text" in selectedFields) addQuestion(TextQuestion("Enter text: ") {
                state = state.copy(text = it)
                next()
              })
              if ("Due date" in selectedFields) addQuestion(DateQuestion("Enter due date: ", true) {
                state = state.copy(dueDate = it.toDateString())
                next()
              })
              if ("Tags" in selectedFields) addQuestion(MultipleChoiceQuestion("Please enter tags numbers, separated by commas. Enter '-' for no tags.", tags) {
                state = state.copy(tags = it)
                next()
              })
              addQuestion(Done("Homework edited successfully") {
                state = state.copy(
                        lastEditPerson = author.username,
                        lastEditTime = Date().toDateString())
                reply("",embed = state.generateEmbed())
                editHomework(state)
                updatePermanent(bot)
              })
              next()
            }))
          }
        }
        command("add") {
          if (clientStore.guilds[config.guild].getMember(authorId).roleIds
                          .intersect(config.adminRoleIds).isEmpty() || guildId != null
          ) return@command bot reject this
          with(Conversation(HomeworkNullable(id = uuid()))) {
            init(bot, channelId, listOf(
                    ChoiceQuestion("Select subject: ", config.subjects) {
                      state = state.copy(subject = it)
                      next()
                    },
                    DateQuestion("Enter due date: ", true) {
                      state = state.copy(dueDate = it)
                      next()
                    },
                    TextQuestion("Enter homework text: ") {
                      state = state.copy(text = it)
                      next()
                    },
                    MultipleChoiceQuestion("Please enter tags numbers, separated by commas. Enter '-' for no tags.", tags) {
                      state = state.copy(tags = it)
                      next()
                    },
                    Done("Homework added successfully") {
                      state = state.copy(
                              lastEditPerson = author.username,
                              lastEditTime = Date())
                      reply("",embed = state.toHomework().generateEmbed())
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
