package com.junron.bot404.util

import java.text.SimpleDateFormat
import java.util.*

fun uuid(): String = UUID.randomUUID().toString()
val isoTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
