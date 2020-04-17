package com.junron.bot404.model

import com.junron.bot404.util.IndexableItem
import com.junron.bot404.util.toDateString
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class Homework(
        override val id: String,
        val subject: String,
        val dueDate: String,
        val text: String,
        val tags: List<String>,
        val lastEditPerson: String,
        val lastEditTime: String,
        val deleted: Boolean = false
): IndexableItem


data class HomeworkNullable(
        val id: String? = null,
        val subject: String? = null,
        val dueDate: Date? = null,
        val text: String? = null,
        val tags: List<String> = emptyList(),
        val lastEditPerson: String? = null,
        val lastEditTime: Date? = null,
        val deleted: Boolean = false
) {
  fun toHomework() = Homework(
          id!!,
          subject!!,
          dueDate!!.toDateString(),
          text!!,
          tags,
          lastEditPerson!!,
          lastEditTime!!.toDateString()
  )
}
