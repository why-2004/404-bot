package com.junron.bot404.model

import com.junron.bot404.util.uuid
import com.junron.pyrobase.jsoncache.IndexableItem
import kotlinx.serialization.Serializable

@Serializable
data class Tag(
    val name: String = "",
    val color: String = "",
    override val id: String = uuid()
) : IndexableItem {
    fun toMap() = mapOf(
        "name" to name,
        "color" to color,
        "id" to id
    )
}
