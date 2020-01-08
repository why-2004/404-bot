package commands

import com.jessecorbett.diskord.api.rest.MessageEdit
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CommandSet
import com.jessecorbett.diskord.dsl.command
import kotlinx.coroutines.runBlocking
import util.ClassLists
import util.SheetsApi
import util.VoteOption
import java.util.*

object Poll : Command {
  override fun init(bot: Bot, prefix: CommandSet) {
    with(bot) {
      with(prefix) {
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
            VoteOption(channel.getMessageReactions(message.id, it.emoji)
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
}
