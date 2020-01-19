package com.hwboard

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UnstableDefault

@Serializable
sealed class User {
  abstract val name: String
  abstract val read: Boolean
  abstract val write: Boolean
  abstract val superuser: Boolean
}

@Serializable
@UnstableDefault
data class DiscordUser(
        @SerialName("username")
        override val name: String,
        val id: String,
        override val read: Boolean = false,
        override val write: Boolean = false,
        override val superuser: Boolean = false
) : User()
