package com.junron.bot404.util

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.dsl.Bot

object EmojiMappings {
  const val ok = "\uD83D\uDC4C"
  const val cross = "❎"
  const val pin = "\uD83D\uDCCC"
}


suspend infix fun Bot.reject(message: Message) =
        clientStore.channels[message.channelId].addMessageReaction(message.id, EmojiMappings.cross)

suspend infix fun Bot.accept(message: Message) =
        clientStore.channels[message.channelId].addMessageReaction(message.id, EmojiMappings.ok)
