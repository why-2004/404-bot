package com.hwboard

import kotlinx.serialization.Serializable
import kotlinx.serialization.toUtf8Bytes

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
        val subject: Subject = Subject(""),
        val dueDate: Date? = null,
        val text: String = "",
        val tags: List<Tag> = emptyList(),
        val lastEditPerson: User? = null,
        val lastEditTime: Date?  = null
)

@Serializable
data class Subject(val name: String)

@Serializable
data class Tag(val name: String, val color: String)
