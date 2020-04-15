package com.junron.bot404.model

import com.junron.bot404.util.isFuture
import kotlinx.serialization.Serializable
import java.time.ZoneId
import java.util.*

@Serializable
data class Lesson(
        val subject: String,
        val day: String,
        val timeStart: String,
        val timeEnd: String
) {
  private val startHour = timeStart.padStart(4, '0').substring(0, 2).toInt()
  private val startMinute = timeStart.padStart(4, '0').substring(2).toInt()
  private val days = listOf("mon", "tue", "wed", "thu", "fri")
  fun getNextLesson(): Date {
    val calendar = Calendar.getInstance(TimeZone.getDefault())
            .apply {
              set(Calendar.DAY_OF_WEEK, days.indexOf(day)+2)
              set(Calendar.HOUR, startHour)
              set(Calendar.MINUTE, startMinute)
              set(Calendar.SECOND,0)
            }
    if (!calendar.time.isFuture()) {
      calendar.add(Calendar.DAY_OF_YEAR, 7)
    }
    return calendar.time
  }

}

