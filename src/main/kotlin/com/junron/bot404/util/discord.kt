package com.junron.bot404.util

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.api.rest.CreateDM
import com.jessecorbett.diskord.api.rest.CreateMessage
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.util.sendMessage

object EmojiMappings {
  const val ok = "\uD83D\uDC4C"
  const val cross = "❎"
  const val pin = "\uD83D\uDCCC"
}


suspend infix fun Bot.reject(message: Message) =
        clientStore.channels[message.channelId].addMessageReaction(message.id, EmojiMappings.cross)

suspend infix fun Bot.accept(message: Message) =
        clientStore.channels[message.channelId].addMessageReaction(message.id, EmojiMappings.ok)

suspend fun Bot.reject(message: Message, reason: String) = run {
  clientStore.channels[message.channelId].sendMessage(reason)
  Unit
}

suspend fun Bot.dmUser(userId: String, message: CreateMessage) {
  val chatId = clientStore.discord.createDM(CreateDM(userId)).id
  clientStore.channels[chatId].createMessage(message)
}
