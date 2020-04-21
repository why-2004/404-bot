package com.junron.bot404.util

import com.junron.bot404.model.Lesson
import kotlinx.serialization.builtins.list
import java.io.File
import java.time.DayOfWeek
import java.util.*

object Timetable {
  lateinit var timetable: List<Lesson>
  fun init() {
    timetable = indentedJson.parse(Lesson.serializer().list, File("./secrets/timetable.json").readText())
  }

  fun getNextLesson(_subject: String, _from: Date? = null): Date? {
    val from = _from ?: Date()
    val subject = if ("chinese" in _subject.toLowerCase()) "MT" else _subject
    return timetable
            .filter { it.subject == subject }
            .map { it.getNextLesson(from) }
            .run {
              if (_from != null) {
                filter {
                  it.isSameDate(from)
                }
              } else {
                this
              }
            }
            .minBy { it.time - from.time }
  }

  fun setLessonTime(_subject: String, date: Date): Date {
    val subject = if ("chinese" in _subject.toLowerCase()) "MT" else _subject
    Calendar.getInstance().apply {
      time = date
    }
    return when (date.dayOfWeek) {
      DayOfWeek.SUNDAY, DayOfWeek.SATURDAY -> date.setTo2359()
      else -> getNextLesson(subject, date.setTo0000()) ?: date.setTo2359()
    }
  }

  private fun Date.setTo2359(): Date {
    val calendar = Calendar.getInstance().apply {
      time = this@setTo2359
    }
    return calendar.apply {
      set(Calendar.HOUR_OF_DAY, 23)
      set(Calendar.MINUTE, 59)
    }.time
  }

  private fun Date.setTo0000(): Date {
    val calendar = Calendar.getInstance().apply {
      time = this@setTo0000
    }
    return calendar.apply {
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
    }.time
  }
}
