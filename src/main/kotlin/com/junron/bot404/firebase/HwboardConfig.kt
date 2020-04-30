package com.junron.bot404.firebase

data class HwboardConfig(
    val name: String = "404",
    val editRoles: List<String> = emptyList(),
    val subjects: List<String> = emptyList(),
    val guild: String = "",
    val tags: List<String> = emptyList()
)
