package com.junron.bot404.model

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class Lesson(
        val subject: String,
        val day: String,
        val timeStart: String,
        val timeEnd: String
) {
  private val endHour = timeEnd.padStart(4, '0').substring(0, 2).toInt()
  private val endMinute = timeEnd.padStart(4, '0').substring(2).toInt()
  private val days = listOf("mon", "tue", "wed", "thu", "fri")
  fun getNextLesson(from: Date = Date()): Date {
    val calendar = Calendar.getInstance(TimeZone.getDefault())
            .apply {
              set(Calendar.DAY_OF_WEEK, days.indexOf(day) + 2)
              set(Calendar.HOUR_OF_DAY, endHour)
              set(Calendar.MINUTE, endMinute)
              set(Calendar.SECOND, 0)
            }
    if (calendar.time.time < from.time) {
      calendar.add(Calendar.DAY_OF_YEAR, 7)
    }
    return calendar.time
  }

}

