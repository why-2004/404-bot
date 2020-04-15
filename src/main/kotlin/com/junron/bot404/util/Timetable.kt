package com.junron.bot404.util

import com.junron.bot404.model.Lesson
import kotlinx.serialization.builtins.list
import java.io.File
import java.util.*

object Timetable {
  lateinit var timetable: List<Lesson>
  fun init() {
    timetable = indentedJson.parse(Lesson.serializer().list, File("./secrets/timetable.json").readText())
  }

  fun getNextLesson(_subject: String): Date? {
    val subject = if ("chinese" in _subject.toLowerCase()) "MT" else _subject
    val now = Date()
    return timetable
            .filter { it.subject == subject }
            .map { it.getNextLesson() }
            .minBy { it.time - now.time }
  }
}
