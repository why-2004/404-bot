import com.jessecorbett.diskord.api.exception.DiscordBadPermissionsException
import com.jessecorbett.diskord.api.model.stringified
import com.jessecorbett.diskord.api.rest.EmbedField
import com.jessecorbett.diskord.api.rest.MessageEdit
import com.jessecorbett.diskord.dsl.bot
import com.jessecorbett.diskord.dsl.command
import com.jessecorbett.diskord.dsl.commands
import com.jessecorbett.diskord.dsl.embed
import com.jessecorbett.diskord.util.Colors
import com.jessecorbett.diskord.util.words
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.UnstableDefault
import util.*
import java.io.File
import java.util.*

val helpText = """
  Commands
  
  `!404 ping`
  Check if server is alive
  
  `!404 search <name|id|index no>`
  Search class list for student
  
  `!404 classlist`
  Print full classlist
  
  `!404 whois <mention|discord username>`
  Prints information about mentioned user
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

      command("whois") {
        val query = if (usersMentioned.isNotEmpty())
          usersMentioned.first().username
        else
          words.drop(2).joinToString(" ")

        val result = ClassLists.getStudentByDiscordUsername(query)
        reply {
          if (result == null)
            description = "No results found"

          fields = if (result == null)
            mutableListOf()
          else
            buildTableEmbed(listOf(result)) as MutableList<EmbedField>

          color = if (result != null)
            Colors.GREEN
          else
            Colors.RED
        }
      }

      command("members") {
        val guildId = this.guildId ?: return@command
        val indexes = clientStore.guilds[guildId].getMembers(50)
            .filter { !(it.user?.isBot ?: true) }.mapNotNull {
              ClassLists.getStudentByDiscordUsername(it.user?.username!!)?.index
            }
        val list = PaginatedList(
            "0",
            "0",
            10,
            indexes
            )
        list.next()
        val message = reply("", list.generate {
          embed {
            description = buildTable(it, true)
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

      command("poll:export") {
        if (this.author.username != "jro") return@command
        val channel = clientStore.channels[channelId]
        val message = channel.getMessages(20)
            .firstOrNull { it.author.username == "Pollmaster" && it.reactions.isNotEmpty() }
        message ?: return@command kotlin.run {
          reply("No poll found")
        }
        val botMessage = reply("Exporting poll data...")
        val votes = message.reactions.map {
          VoteOption(
              channel.getMessageReactions(message.id, it.emoji)
                  .filter { user ->
                    !user.isBot && ClassLists.getStudentByDiscordUsername(user.username) != null
                  }
                  .map { user -> ClassLists.getStudentByDiscordUsername(user.username)!! },
              it.emoji
          )
        }.dropLast(1)
        val pollCode = message.embeds.first().author?.name?.substringAfter(">> ")
            ?: UUID.randomUUID().toString()
        SheetsApi.exportPoll(pollCode, votes) {
          runBlocking {
            channel.editMessage(botMessage.id, MessageEdit(
                "Results for poll $pollCode:\n$it"
            ))
          }
        }
      }
    }
  }
}
