package commands

import com.hwboard.DiscordUser
import com.hwboard.HomeworkNullable
import com.hwboard.Subject
import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.api.rest.EmbedField
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CommandSet
import com.jessecorbett.diskord.dsl.command
import com.jessecorbett.diskord.dsl.embed
import com.jessecorbett.diskord.util.Colors
import com.jessecorbett.diskord.util.authorId
import com.jessecorbett.diskord.util.sendMessage
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.list
import kotlinx.serialization.serializer
import util.*
import java.io.File
import java.util.*


object HwBot : Command {

  val announcementRoles = File("secrets/tokens/announcementRoles").readText().trim().split(",")
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
          if (permanentFile.exists()) return@command bot reject this
          val homework = getHomework()
          val id = reply("", embed = combineEmbeds(buildHomeworkEmbeds(homework))).id
          bot accept this
          permanentFile.createNewFile()
          permanentFile.writeText("$id,$channelId")
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
          val subscribers = Json.plain.parse(Long.serializer().list, subscribersFile.readText().trim())
          val newSubscribers = (subscribers + (authorId.toLong())).distinct()
          subscribersFile.writeText(Json.plain.stringify(Long.serializer().list, newSubscribers))
          bot accept this
        }

        command("add") {
          if (clientStore.guilds[announcementChannelData.first()].getMember(authorId).roleIds
                          .intersect(announcementRoles).isEmpty() || guildId != null
          ) return@command bot reject this
          val processState = state[authorId]
          if (processState == null) {
            state[authorId] = HomeworkNullable(id = UUID.randomUUID().toString())
            reply("Please enter subject number: ", embed {
              color = Colors.CYAN
              fields = mutableListOf(EmbedField("Subjects", subjects.mapIndexed { index, s ->
                "$index | $s"
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
                      clientStore.channels[it.channelId].sendMessage("Please enter a number between 0 and ${subjects.lastIndex}")
                    }
            if (index !in 0..(subjects.lastIndex))
              return@messageCreated run {
                clientStore.channels[it.channelId].sendMessage("Please enter a number between 0 and ${subjects.lastIndex}")
              }
            state[it.authorId] = userState.copy(subject = Subject(subjects[index]))
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
                "$index | ${s.name}"
              }.joinToString("\n"), false))
            })
          }
          userState.tags == null -> {
            state[it.authorId] = userState.copy(tags = it.content.split(",")
                    .map { it.trim().toIntOrNull() }
                    .filter { it in 0..2 }
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

  suspend infix fun Bot.reject(message: Message) =
          clientStore.channels[message.channelId].addMessageReaction(message.id, EmojiMappings.cross)

  suspend infix fun Bot.accept(message: Message) =
          clientStore.channels[message.channelId].addMessageReaction(message.id, EmojiMappings.ok)

}
