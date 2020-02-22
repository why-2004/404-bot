package commands

import com.jessecorbett.diskord.api.model.stringified
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.util.authorId
import commands.HwBot.accept
import commands.HwBot.reject

object Pin {
  fun init(bot: Bot) {
    with(bot) {
      reactionAdded {
        if (it.emoji.stringified == "\uD83D\uDCCC") {
          val message = bot.clientStore.channels[it.channelId].getMessage(it.messageId)
          with(message) {
            if (clientStore.guilds[announcementChannelData.first()].getMember(authorId).roleIds
                            .intersect(HwBot.announcementRoles).isEmpty() || guildId != null
            ) return@reactionAdded bot reject this
            bot.clientStore.channels[it.channelId].pinMessage(it.messageId)
            bot accept this
          }
        }
      }
    }
  }
}
