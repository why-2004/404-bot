package com.junron.bot404.model

import com.junron.bot404.commands.Time
import com.junron.pyrobase.jsoncache.IndexableItem


abstract class Subscriber : IndexableItem {
  abstract val timings: List<Time>
  abstract val authorId: String
  override val id: String
    get() = authorId
}
