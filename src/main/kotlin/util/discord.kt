package util

import com.jessecorbett.diskord.api.rest.Embed
import com.jessecorbett.diskord.api.rest.EmbedField
import com.jessecorbett.diskord.dsl.footer
import de.vandermeer.asciitable.AsciiTable
import de.vandermeer.asciitable.CWC_LongestWordMin
import de.vandermeer.skb.interfaces.transformers.textformat.TextAlignment
import kotlinx.serialization.Serializable
import kotlinx.serialization.UnstableDefault
import java.lang.Integer.min
import kotlin.math.ceil

object EmojiMappings {
  const val trash = "\uD83D\uDDD1"
  const val arrowRight = "▶"
  const val arrowLeft = "◀"
  const val eyes = "\uD83D\uDC40"
}

fun buildTable(data: List<Student>): String {
  val table = AsciiTable()
  table.addRule()
  table.addRow("No", "ID", "Name", "Combi")
  table.addRule()
  data.forEach {
    table.addRow(it.index, it.id, it.name, it.combi)
    table.addRule()
  }
  table.renderer.cwc = CWC_LongestWordMin(listOf(3, 3, 15, 3).toIntArray())
  table.setTextAlignment(TextAlignment.CENTER)
  return "```" + table.render()!! + "```"
}

fun buildTableEmbed(data: List<Student>): List<EmbedField> {
  return data.map {
    EmbedField(
        name = "**${it.name}**",
        value = """
          **No**
          ${it.index}
          
          **Id**
          ${it.id}
          
          **Combi**
          ${it.combi}
        """.trimIndent(),
        inline = true
    )
  }

}

@Serializable
@UnstableDefault
data class PaginatedList(
    val userId: String,
    val messageId: String,
    val paginateNumber: Int
) {
  var counter = -1
    private set

  fun next(): Boolean {
    if ((counter + 1) * paginateNumber <= ClassLists.load().size) {
      counter++
      return true
    }
    return false
  }

  fun prev(): Boolean {
    if (counter > 0) {
      counter--
      return true
    }
    return false
  }

  fun generate(generator: (List<Student>) -> Embed): Embed {
    val data = ClassLists.load()
    val currentData = data.subList(
        counter * paginateNumber,
        min(counter * paginateNumber + paginateNumber, data.size)
    )
    val embed = generator(currentData)
    embed.footer("${counter + 1}/${ceil(data.size.toDouble() / paginateNumber).toInt()}")
    return embed
  }

}
