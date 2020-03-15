package com.junron.bot404

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File


@Serializable
data class Config(
        val discordToken: String,
        val announcementRoles: List<String>,
        val announcementChannel: String,
        val guild: String,
        val botPrefix: String,
        val hwbotPrefix: String,
        val homeworkFile: String
)

val config =
        Json.parse(Config.serializer(), File("config.json").readText())
