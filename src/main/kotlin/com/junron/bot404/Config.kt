package com.junron.bot404

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File


@Serializable
data class Config(
        val discordToken: String,
        val adminRoleIds: List<String>,
        val guild: String,
        val botPrefix: String,
        val hwbotPrefix: String,
        val homeworkFile: String,
        val subjects: MutableList<String>
){
  init {
    subjects.sort()
  }
}

val config =
        Json.parse(Config.serializer(), File("config.json").readText())
