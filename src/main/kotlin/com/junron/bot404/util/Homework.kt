package com.junron.bot404.util

import com.github.shyiko.skedule.Schedule
import com.jessecorbett.diskord.api.rest.CreateMessage
import com.jessecorbett.diskord.api.rest.Embed
import com.jessecorbett.diskord.api.rest.EmbedField
import com.jessecorbett.diskord.api.rest.MessageEdit
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.embed
import com.jessecorbett.diskord.dsl.field
import com.jessecorbett.diskord.util.Colors
import com.junron.bot404.commands.PermanentMessage
import com.junron.bot404.model.Homework
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.builtins.list
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.File
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.*
import kotlin.concurrent.fixedRateTimer

private val hwFile = File("./homework.json")
val permanentMessageStorage = Storage("permanentMessages", PermanentMessage.serializer())
val subscribers = Storage("subscribers", Long.serializer())
val tags = listOf(
        "Graded",
        "Project",
        "Optional",
        "Assessment"
)

@UnstableDefault
fun getHomework() =
        indentedJson.parse(Homework.serializer().list, hwFile.readText())
                .filter { it.dueDate.toDate().isFuture() }

fun buildHomeworkEmbeds(homeworks: List<Homework>): List<Embed> {
  var counter = 0
  return homeworks.sortedBy { it.dueDate }
          .groupBy { it.dueDate.substringBefore("T") }
          .map {
            val date = it.key.toDateSimple()
            val dueDateDisplay = "Due " + date.toShortString()
            embed {
              title = dueDateDisplay
              color = Colors.GREEN
              fields = it.value.map { homework ->
                EmbedField(
                        "**#${counter++} ${homework.text}**",
                        "${homework.subject}\n" +
                                if (homework.tags.isNotEmpty())
                                  "(${homework.tags.joinToString(", ")})"
                                else "",
                        inline = false
                )
              } as MutableList<EmbedField>
            }
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

fun Homework.generateEmbed() = embed {
  title = text
  field("Subject", subject, false)
  field("Due", dueDate.toDate().toShortString(), false)
  field("Tags", if (tags.isEmpty()) "None" else tags.joinToString(", "), false)
  field("Last edited by", lastEditPerson, false)
  field("Last updated", lastEditTime.toDate().toDetailedString(), false)
  color = Colors.GREEN
}

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
          indentedJson.stringify(Homework.serializer().list, homeworkList + homework)
  )
}

@UnstableDefault
fun editHomework(homework: Homework) {
  val homeworkList = Json.parse(Homework.serializer().list, hwFile.readText())
          .filter { it.id != homework.id } + homework
  hwFile.writeText(
          indentedJson.stringify(Homework.serializer().list, homeworkList)
  )
}

@UnstableDefault
fun deleteHomework(homeworkIndex: Int): Boolean {
  val currentHomework = getHomework().sortedBy { it.dueDate }
  if (homeworkIndex !in 0..currentHomework.lastIndex) return false
  val homeworkId = currentHomework[homeworkIndex].id
  val homeworkList = Json.parse(Homework.serializer().list, hwFile.readText())
  hwFile.writeText(
          indentedJson.stringify(Homework.serializer().list, homeworkList
                  .filter { it.id != homeworkId }
          )
  )
  return true
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
  get() = filter { it.dueDate.toDate().isTomorrow() }
