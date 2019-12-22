package util

import de.vandermeer.asciitable.AsciiTable
import de.vandermeer.asciitable.CWC_LongestWordMin
import de.vandermeer.skb.interfaces.transformers.textformat.TextAlignment

object EmojiMappings {
  const val trash = "\uD83D\uDDD1"
  const val arrowRight = "▶"
  const val arrowLeft = "◀"
  const val eyes = "\uD83D\uDC40"
}

fun buildTable(data: List<Student>): String {
  val table = AsciiTable()
  table.addRule()
  table.addRow("No", "ID", "Name", "Combination")
  table.addRule()
  data.forEach {
    table.addRow(it.index, it.id, it.name, it.combi)
    table.addRule()
  }
  table.renderer.cwc = CWC_LongestWordMin(listOf(3,3,15,3).toIntArray())
  table.setTextAlignment(TextAlignment.CENTER)
  return "```" + table.render()!! + "```"
}
