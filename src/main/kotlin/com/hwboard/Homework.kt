package com.hwboard

import kotlinx.serialization.Serializable

@Serializable
data class Homework(
        val id: String,
        val subject: Subject,
        val dueDate: Date,
        val text: String,
        val tags: List<Tag>,
        val lastEditPerson: User,
        val lastEditTime: Date
)

data class HomeworkNullable(
        val id: String = "",
        val subject: Subject? = null,
        val dueDate: Date? = null,
        val text: String = "",
        val tags: List<Tag>? = null,
        val lastEditPerson: User? = null,
        val lastEditTime: Date? = null
) {
  fun toHomework() =
          Homework(id, subject!!, dueDate!!, text, tags!!, lastEditPerson!!, lastEditTime!!)
}

@Serializable
data class Subject(val name: String)

@Serializable
data class Tag(val name: String, val color: String)
