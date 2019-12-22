import com.jessecorbett.diskord.dsl.bot
import com.jessecorbett.diskord.dsl.command
import com.jessecorbett.diskord.dsl.commands
import com.jessecorbett.diskord.util.words
import kotlinx.serialization.UnstableDefault
import util.ClassLists
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
//    reactionAdded {
//      val message = clientStore.channels[it.channelId].getMessage(it.messageId)
//      //      Return if message is not sent by bot
//      val query = Database[it.messageId] ?: return@reactionAdded
//      val reaction = message.reactions.firstOrNull { reaction -> reaction.emoji == it.emoji } ?: return@reactionAdded
//      if (reaction.count == 1) return@reactionAdded
//      when (reaction.emoji.stringified) {
//        util.EmojiMappings.trash -> {
////          Prevent unauthorized deletion
//          if (query.userId != it.userId) return@reactionAdded
//          Database -= it.messageId
//          message.delete()
//          return@reactionAdded
//        }
//        util.EmojiMappings.arrowRight -> {
//          if (query.increment()) {
//            clientStore.channels[it.channelId].editMessage(
//              it.messageId, MessageEdit(
//                "",
//                embed = query.cache[query.answerNumber]
//              )
//            )
//            Database[it.messageId] = query
//          }
//        }
//        util.EmojiMappings.arrowLeft -> {
//          if (query.decrement()) {
//            clientStore.channels[it.channelId].editMessage(
//              it.messageId, MessageEdit(
//                "",
//                embed = query.cache[query.answerNumber]
//              )
//            )
//            Database[it.messageId] = query
//          }
//        }
//        else -> return@reactionAdded
//      }
//      try {
//        clientStore.channels[it.channelId].removeMessageReaction(it.messageId, reaction.emoji.stringified, it.userId)
//      } catch (e: DiscordBadPermissionsException) {
//        println("Bad permissions for channel: ${it.channelId}")
//      }
//    }
    commands("!404 ") {
      command("help") {
        reply(helpText)
      }
      command("ping") {
        reply("pong")
      }
      command("search"){
        val query = words.drop(2).joinToString(" ")
        val result = ClassLists.search(query)
        println(query)
        reply(buildTable(result))
      }
    }
  }
}
