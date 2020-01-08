import com.jessecorbett.diskord.api.exception.DiscordBadPermissionsException
import com.jessecorbett.diskord.api.model.stringified
import com.jessecorbett.diskord.api.rest.MessageEdit
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.embed
import com.jessecorbett.diskord.util.Colors
import util.EmojiMappings
import util.buildTable

object Reactions {

  fun init(bot: Bot) {
    with(bot) {
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
    }
  }
}
