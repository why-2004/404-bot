package commands

import Database
import com.jessecorbett.diskord.api.rest.EmbedField
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CommandSet
import com.jessecorbett.diskord.dsl.command
import com.jessecorbett.diskord.dsl.embed
import com.jessecorbett.diskord.util.Colors
import com.jessecorbett.diskord.util.words
import kotlinx.serialization.UnstableDefault
import util.*

object Classlist : Command {
  @UnstableDefault
  override fun init(bot: Bot, prefix: CommandSet) {
    with(bot) {
      with(prefix) {
        command("search") {
          val query = words.drop(2).joinToString(" ")
          val result = ClassLists.search(query)
          println(query)
          reply {
            if (result.isEmpty())
              description = "No results found"

            fields = if (result.isEmpty())
              mutableListOf()
            else
              buildTableEmbed(result) as MutableList<EmbedField>

            color = if (result.isNotEmpty())
              Colors.GREEN
            else
              Colors.RED
          }
        }
        command("classlist") {
          val list = PaginatedList(
                  "0",
                  "0",
                  10
          )
          list.next()
          val message = reply("", list.generate {
            embed {
              description = buildTable(it)
              color = Colors.GREEN
            }
          })
          message.react(EmojiMappings.arrowLeft)
          message.react(EmojiMappings.trash)
          message.react(EmojiMappings.arrowRight)

          val newList = list.copy(
                  messageId = message.id,
                  userId = author.id
          )
          newList.next()
          Database[message.id] = newList
        }
      }
    }
  }
}
