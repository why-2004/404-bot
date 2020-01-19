package commands

import com.hwboard.Homework
import com.jessecorbett.diskord.api.rest.EmbedField
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CommandSet
import com.jessecorbett.diskord.dsl.command
import com.jessecorbett.diskord.dsl.embed
import com.jessecorbett.diskord.util.Colors
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.list
import java.io.File
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

object HwBot : Command {
  private val hwFile = File("../hwboard2/data/homework.json")
  @UnstableDefault
  override fun init(bot: Bot, prefix: CommandSet) {
    with(bot) {
      with(prefix) {
        command("show") {
          val homework = getHomework()
          buildHomeworkEmbeds(homework).forEach { reply("", embed = it) }
        }
        command("tomorrow") {
          val homework = getHomework().filter { it.dueDate.date.toDate().isTomorrow() }
          buildHomeworkEmbeds(homework).forEach { reply("", embed = it) }
        }
      }
    }
  }

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

  private fun String.toDate() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'").parse(this)
  private fun Date.isFuture() = this.after(Date.from(Instant.now()))
  private fun Date.isTomorrow() = this.toInstant()
          .atZone(ZoneId.systemDefault())
          .toLocalDate()
          .isEqual(LocalDate.now().plusDays(1))
}
