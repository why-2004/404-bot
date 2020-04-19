package com.junron.bot404.util

import com.jessecorbett.diskord.api.rest.Embed
import com.jessecorbett.diskord.api.rest.EmbedField
import com.jessecorbett.diskord.api.rest.MessageEdit
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.embed
import com.jessecorbett.diskord.dsl.field
import com.jessecorbett.diskord.util.Colors
import com.junron.bot404.commands.PermanentMessage
import com.junron.bot404.model.Homework
import com.junron.pyrobase.jsoncache.Storage
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json
import java.io.File

private val hwFile = File("./homework.json")
val permanentMessageStorage = Storage("permanentMessages", PermanentMessage.serializer())
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
fun deleteHomework(homework: Homework) {
  editHomework(homework.copy(deleted = true))
}

@UnstableDefault
fun updatePermanent(bot: Bot) {
  val homework = getHomework()
  permanentMessageStorage.forEach {
    runBlocking {
      bot.clientStore.channels[it.channel].editMessage(it.messageId, MessageEdit(
              content = "",
              embed = combineEmbeds(buildHomeworkEmbeds(homework))
      ))
    }
  }
}

val List<Homework>.tomorrow: List<Homework>
  get() = filter { it.dueDate.toDate().isTomorrow() }
