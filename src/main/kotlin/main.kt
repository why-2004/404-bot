import com.jessecorbett.diskord.api.exception.DiscordBadPermissionsException
import com.jessecorbett.diskord.api.model.stringified
import com.jessecorbett.diskord.api.rest.MessageEdit
import com.jessecorbett.diskord.dsl.bot
import com.jessecorbett.diskord.dsl.command
import com.jessecorbett.diskord.dsl.commands
import com.jessecorbett.diskord.dsl.embed
import com.jessecorbett.diskord.util.Colors
import com.jessecorbett.diskord.util.words
import kotlinx.serialization.UnstableDefault
import util.ClassLists
import util.EmojiMappings
import util.PaginatedList
import util.buildTable
import java.io.File

val helpText = """
  Commands
  
  `!404 ping`
  Check if server is alive
  
  `!404 search <name|id|index no>`
  Search class list for student
""".trimIndent()

@UnstableDefault
suspend fun main() {
  val token = File("secrets/token.txt").readText().trim()
  bot(token) {
    reactionAdded {
      val message = clientStore.channels[it.channelId].getMessage(it.messageId)
      //      Return if message is not sent by bot
      val list = Database[it.messageId] ?: return@reactionAdded
      val reaction = message.reactions.firstOrNull { reaction -> reaction.emoji == it.emoji }
          ?: return@reactionAdded
      if (reaction.count == 1) return@reactionAdded
      when (reaction.emoji.stringified) {
        EmojiMappings.trash -> {
//          Prevent unauthorized deletion
          if (list.userId != it.userId) return@reactionAdded
          Database -= it.messageId
          message.delete()
          return@reactionAdded
        }
        EmojiMappings.arrowRight -> {
          if (list.next()) {
            clientStore.channels[it.channelId].editMessage(
                it.messageId, MessageEdit(
                "",
                embed = list.generate {
                  embed {
                    description = buildTable(it)
                    color = Colors.GREEN
                  }
                }
            )
            )
            Database[it.messageId] = list
          }
        }
        EmojiMappings.arrowLeft -> {
          if (list.prev()) {
            clientStore.channels[it.channelId].editMessage(
                it.messageId, MessageEdit(
                "",
                embed = list.generate {
                  embed {
                    description = buildTable(it)
                    color = Colors.GREEN
                  }
                }
            )
            )
            Database[it.messageId] = list
          }
        }
        else -> return@reactionAdded
      }
      try {
        clientStore.channels[it.channelId].removeMessageReaction(it.messageId, reaction.emoji.stringified, it.userId)
      } catch (e: DiscordBadPermissionsException) {
        println("Bad permissions for channel: ${it.channelId}")
      }
    }
    commands("!404 ") {
      command("help") {
        reply(helpText)
      }
      command("ping") {
        reply("pong")
      }
      command("search") {
        val query = words.drop(2).joinToString(" ")
        val result = ClassLists.search(query)
        println(query)
        reply {
          description = if (result.isNotEmpty())
            buildTable(result)
          else
            "No results found"

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
