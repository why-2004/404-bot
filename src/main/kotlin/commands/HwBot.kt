package commands

import com.github.shyiko.skedule.Schedule
import com.hwboard.*
import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.api.rest.CreateMessage
import com.jessecorbett.diskord.api.rest.Embed
import com.jessecorbett.diskord.api.rest.EmbedField
import com.jessecorbett.diskord.api.rest.MessageEdit
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CommandSet
import com.jessecorbett.diskord.dsl.command
import com.jessecorbett.diskord.dsl.embed
import com.jessecorbett.diskord.util.Colors
import com.jessecorbett.diskord.util.authorId
import com.jessecorbett.diskord.util.sendMessage
import com.jessecorbett.diskord.util.words
import com.joestelmach.natty.Parser
import commands.Reminders.dmUser
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.list
import kotlinx.serialization.serializer
import util.EmojiMappings
import java.io.File
import java.text.SimpleDateFormat
import java.time.*
import java.util.*
import java.util.Calendar.HOUR_OF_DAY
import java.util.Date
import kotlin.concurrent.fixedRateTimer


object HwBot : Command {
  private val hwFile = File("../hwboard2/data/homework.json")
  private val subscribersFile = File("secrets/subscribers.json")
  private val permanentFile = File("secrets/permanent")
  private val subjects = listOf(
          "Math",
          "English",
          "Chinese",
          "Higher chinese",
          "CS",
          "Physics",
          "Chemistry",
          "PE"
  ).sorted()
  private val tags = listOf(
          Tag("Graded", "red"),
          Tag("Project", "#ffcc00"),
          Tag("Optional", "#4cd964")
  )
  private val tagNames = listOf("Graded", "Project", "Optional")
  private val announcementRoles = File("secrets/tokens/announcementRoles").readText().trim().split(",")
  private val state = mutableMapOf<String, HomeworkNullable>()
  @UnstableDefault
  override fun init(bot: Bot, prefix: CommandSet) {
    fixedRateTimer(
            UUID.randomUUID().toString(),
            false,
            (Schedule.at(LocalTime.of(19, 0))
                    .everyDay()
                    .next(ZonedDateTime.now())
                    .toEpochSecond() - ZonedDateTime.now().toEpochSecond()) * 1000,
            8.64e+7.toLong()
    ) {
      val dayOfWeek = ZonedDateTime.now().dayOfWeek
      val subscribers = Json.plain.parse(Long.serializer().list, subscribersFile.readText().trim())
      val homework = getHomework().filter { it.dueDate.date.toDate().isTomorrow() }
      if ((dayOfWeek == DayOfWeek.FRIDAY || dayOfWeek == DayOfWeek.SATURDAY) && homework.isEmpty()) return@fixedRateTimer
      val embed = buildHomeworkEmbeds(homework).firstOrNull() ?: embed {
        title = "There is no homework tomorrow"
        color = Colors.GREEN
      }
      subscribers.forEach {
        runBlocking {
          if (it == 282108880224256008L) {
            dmUser(bot, it, CreateMessage(content = "", embed =
            buildHomeworkEmbeds(homework.filter { "chinese" !in it.subject.name.toLowerCase() }).firstOrNull()
                    ?: embed {
                      title = "There is no homework tomorrow"
                      color = Colors.GREEN
                    }))
          } else {
            dmUser(bot, it, CreateMessage(content = "", embed = embed))
          }
        }
      }
    }
    fixedRateTimer(
            UUID.randomUUID().toString(),
            false,
            0L,
            3.6e+6.toLong()
    ) {
      if (!permanentFile.exists()) return@fixedRateTimer
      val (id, channelId) = permanentFile.readText().trim().split(",")
      val homework = getHomework()
      runBlocking {
        bot.clientStore.channels[channelId].editMessage(id, MessageEdit(
                content = "",
                embed = combineEmbeds(buildHomeworkEmbeds(homework))
        ))
      }
    }
    if (!subscribersFile.exists()) {
      subscribersFile.createNewFile()
      subscribersFile.writeText("[]")
    }
    with(bot) {
      with(prefix) {
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
          val homework = getHomework().filter { it.dueDate.date.toDate().isTomorrow() }
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
        command("add:quick") {
          if (clientStore.guilds[announcementChannelData.first()].getMember(authorId).roleIds
                          .intersect(announcementRoles).isEmpty() || guildId != null
          ) return@command bot reject this
          val query = words.drop(2).joinToString(" ").split("|").map { it.trim() }
          val subject = subjects.getOrNull(query.firstOrNull()?.toIntOrNull()
                  ?: -1) ?: return@command bot reject this

          val dueDate = query.getOrNull(1)?.let { parseDate(it) }
                  ?: return@command bot reject this

          val name = query.getOrNull(2)?.makeNullIfEmpty()
                  ?: return@command bot reject this

          val tags = query.getOrNull(3)
                  ?.split(",")
                  ?.map { it.trim().toIntOrNull() }
                  ?.filter { it in 0..2 }
                  ?.map { tags[it!!] }
                  ?: emptyList()

          val homework = Homework(
                  UUID.randomUUID().toString(),
                  Subject(subject),
                  dueDate.toDate(),
                  name,
                  tags,
                  DiscordUser(author.username, authorId, read = true, write = true),
                  Date().toDate()
          )

          addHomework(homework)

          bot accept this
        }
      }
      messageCreated { it ->
        val userState = state[it.authorId] ?: return@messageCreated
        when {
          userState.subject == null -> {
            val index = it.content.toIntOrNull() ?: return@messageCreated run {
              clientStore.channels[it.channelId].sendMessage("Please enter a number between 0 and ${subjects.lastIndex}")
            }
            if (index !in 0..(subjects.lastIndex))
              return@messageCreated run {
                clientStore.channels[it.channelId].sendMessage("Please enter a number between 0 and ${subjects.lastIndex}")
              }
            state [it.authorId] = userState.copy(subject = Subject(subjects[index]))
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
            state.remove(it.authorId)
            clientStore.channels[it.channelId].sendMessage("Homework added successfully")
          }
        }
      }
    }
  }

  @UnstableDefault
  private fun addHomework(homework: Homework) {
    val homeworkList = Json.indented.parse(Homework.serializer().list, hwFile.readText())
    hwFile.writeText(
            Json.indented.stringify(Homework.serializer().list, homeworkList + homework)
    )
  }

  private fun parseDate(date: String) = Parser().parse(date)
          ?.firstOrNull()
          ?.dates
          ?.firstOrNull()
          ?.let {
            if (it.isFuture()) it else null
          }
          ?.run {
            val cal = GregorianCalendar()
            cal.time = this
            cal[HOUR_OF_DAY] = 22
            cal[Calendar.MINUTE] = 59
            cal[Calendar.SECOND] = 59
            cal[Calendar.MILLISECOND] = 999
            cal.time
          }

  private suspend infix fun Bot.reject(message: Message) =
          clientStore.channels[message.channelId].addMessageReaction(message.id, EmojiMappings.cross)

  private suspend infix fun Bot.accept(message: Message) =
          clientStore.channels[message.channelId].addMessageReaction(message.id, EmojiMappings.ok)

  private fun String.makeNullIfEmpty() = if (isEmpty()) null else this

  @UnstableDefault
  private fun getHomework() =
          Json.indented.parse(Homework.serializer().list, hwFile.readText())
                  .filter { it.dueDate.date.toDate().isFuture() }

  private fun buildHomeworkEmbeds(homework: List<Homework>) =
          homework.sortedBy { it.dueDate.date }
                  .groupBy { it.dueDate.date.substringBefore("T") }
                  .map {
                    embed {
                      title = "Homework due on " + it.key
                      color = Colors.GREEN
                      fields = it.value.map {
                        EmbedField(
                                "**${it.text}**",
                                "${it.subject.name}\n" +
                                        if (it.tags.isNotEmpty())
                                          "(${it.tags.joinToString(", ") { tag -> tag.name }})"
                                        else "",
                                inline = false
                        )
                      } as MutableList<EmbedField>
                    }
                  }

  private fun combineEmbeds(embeds: List<Embed>) = embed {
    color = Colors.GREEN
    title = "Homework"
    fields = embeds.map {
      val fields = mutableListOf<EmbedField>()
      val title = it.title!!
      fields += EmbedField("-------------------------\n", "__**${title.substringAfter("on ")}**__", false)
      fields += it.fields
      fields
    }.flatten() as MutableList<EmbedField>
  }

  private fun String.toDate() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'").parse(this)
  private fun Date.toDate() = com.hwboard.Date(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'").format(this))
  private fun Date.isFuture() = this.after(Date.from(Instant.now()))
  private fun Date.isTomorrow() = this.toInstant()
          .atZone(ZoneId.systemDefault())
          .toLocalDate()
          .isEqual(LocalDate.now().plusDays(1))
}
