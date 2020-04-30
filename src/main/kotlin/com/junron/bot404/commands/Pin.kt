package com.junron.bot404.commands

import com.jessecorbett.diskord.api.model.stringified
import com.jessecorbett.diskord.dsl.Bot
import com.junron.bot404.firebase.HwboardFirestore.getConfig
import com.junron.bot404.util.EmojiMappings.pin

object Pin {
    fun init(bot: Bot) {
        with(bot) {
            reactionAdded {
                if (it.emoji.stringified == pin) {
                    val guild = it.guildId ?: return@reactionAdded
                    if (clientStore.guilds[guild].getMember(it.userId).roleIds
                            .intersect(getConfig().editRoles).isEmpty()
                    ) return@reactionAdded
                    bot.clientStore.channels[it.channelId].pinMessage(it.messageId)
                }
            }

            reactionRemoved {
                if (it.emoji.stringified == pin) {
                    val guild = it.guildId ?: return@reactionRemoved
                    if (clientStore.guilds[guild].getMember(it.userId).roleIds
                            .intersect(getConfig().editRoles).isEmpty()
                    ) return@reactionRemoved
                    bot.clientStore.channels[it.channelId].unpinMessage(it.messageId)
                }
            }
        }
    }
}
