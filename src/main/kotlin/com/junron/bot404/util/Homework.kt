package com.junron.bot404.util

import com.github.shyiko.skedule.Schedule
import com.hwboard.Homework
import com.hwboard.Tag
import com.jessecorbett.diskord.api.rest.CreateMessage
import com.jessecorbett.diskord.api.rest.Embed
import com.jessecorbett.diskord.api.rest.EmbedField
import com.jessecorbett.diskord.api.rest.MessageEdit
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.embed
import com.jessecorbett.diskord.util.Colors
import com.joestelmach.natty.Parser
import com.junron.bot404.commands.PermanentMessage
import com.junron.bot404.config
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.list
import kotlinx.serialization.serializer
import java.io.File
import java.text.SimpleDateFormat
import java.time.*
import java.time.temporal.TemporalAdjusters
import java.util.*
import kotlin.concurrent.fixedRateTimer

private val hwFile = File(config.homeworkFile)
val permanentMessageStorage = Storage("permanentMessages", PermanentMessage.serializer())
val subscribers = Storage("subscribers", Long.serializer())
val tags = listOf(
        Tag("Graded", "red"),
        Tag("Project", "#ffcc00"),
        Tag("Optional", "#4cd964"),
        Tag("Assessment", "#f18e33")
)

@UnstableDefault
fun getHomework() =
        Json.indented.parse(Homework.serializer().list, hwFile.readText())
                .filter { it.dueDate.date.toDate().isFuture() }

fun buildHomeworkEmbeds(homeworks: List<Homework>) =
        homeworks.sortedBy { it.dueDate.date }
                .groupBy { it.dueDate.date.substringBefore("T") }
                .map {
                  val date = it.key.toDateSimple()
                  val dueDateDisplay = when {
                    date.isTomorrow() -> "Due tomorrow"
                    date.isToday() -> "Due today"
                    date.isThisWeek() -> "Due ${date.dayOfWeek.toString().toLowerCase().capitalize()}"
                    else -> "Due ${it.key}"
                  }
                  embed {
                    title = dueDateDisplay
                    color = Colors.GREEN
                    fields = it.value.map { homework ->
                      EmbedField(
                              "**${homework.text}**",
                              "${homework.subject.name}\n" +
                                      if (homework.tags.isNotEmpty())
                                        "(${homework.tags.joinToString(", ") { tag -> tag.name }})"
                                      else "",
                              inline = false
                      )
                    } as MutableList<EmbedField>
                  }
                }

fun combineEmbeds(embeds: List<Embed>) = embed {
  color = Colors.GREEN
  title = "Homework"
  fields = embeds.map {
    val fields = mutableListOf<EmbedField>()
    val title = it.title!!
    fields += EmbedField("-------------------------\n", "__**$title**__", false)
    fields += it.fields
    fields
  }.flatten() as MutableList<EmbedField>
}

fun String.toDateSimple(): Date = SimpleDateFormat("yyyy-MM-dd").parse(this)
fun String.toDate(): Date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'").parse(this)
fun Date.toDate() = com.hwboard.Date(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'").format(this))
fun Date.isFuture() = this.after(Date.from(Instant.now()))
fun Date.toLocalDate(): LocalDate = this.toInstant()
        .atZone(ZoneId.systemDefault())
        .toLocalDate()

fun Date.isTomorrow() = this.toLocalDate()
        .isEqual(LocalDate.now().plusDays(1))

fun Date.isToday() = this.toLocalDate()
        .isEqual(LocalDate.now())

fun Date.isThisWeek() = this.toLocalDate()
        .isBefore(LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.SUNDAY)))

val Date.dayOfWeek: DayOfWeek
  get() = this.toLocalDate().dayOfWeek


@UnstableDefault
fun init(bot: Bot) {
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
    if ((dayOfWeek == DayOfWeek.FRIDAY || dayOfWeek == DayOfWeek.SATURDAY) && getHomework().tomorrow.isEmpty()) return@fixedRateTimer
    val embed = buildHomeworkEmbeds(getHomework().tomorrow).firstOrNull()
            ?: embed {
              title = "There is no homework tomorrow"
              color = Colors.GREEN
            }
    subscribers.forEach {
      runBlocking {
        Reminders.dmUser(bot, it.item.toString(), CreateMessage(content = "", embed = embed))
      }
    }
  }
  fixedRateTimer(
          UUID.randomUUID().toString(),
          false,
          0L,
          3.6e+6.toLong()
  ) {
    updatePermanent(bot)
  }
}

@UnstableDefault
fun addHomework(homework: Homework) {
  val homeworkList = Json.parse(Homework.serializer().list, hwFile.readText())
  hwFile.writeText(
          Json.stringify(Homework.serializer().list, homeworkList + homework)
  )
}

fun parseDate(date: String) = Parser().parse(date)
        ?.firstOrNull()
        ?.dates
        ?.firstOrNull()
        ?.let {
          if (it.isFuture()) it else null
        }
        ?.run {
          val cal = GregorianCalendar()
          cal.time = this
          cal[Calendar.HOUR_OF_DAY] = 22
          cal[Calendar.MINUTE] = 59
          cal[Calendar.SECOND] = 59
          cal[Calendar.MILLISECOND] = 999
          cal.time
        }

@UnstableDefault
fun updatePermanent(bot: Bot) {
  val homework = getHomework()
  permanentMessageStorage.forEach {
    runBlocking {
      bot.clientStore.channels[it.item.channel].editMessage(it.item.messageId, MessageEdit(
              content = "",
              embed = combineEmbeds(buildHomeworkEmbeds(homework))
      ))
    }
  }
}

val List<Homework>.tomorrow: List<Homework>
  get() = filter { it.dueDate.date.toDate().isTomorrow() }
