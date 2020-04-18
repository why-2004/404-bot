package com.junron.bot404.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json
import java.io.File


class Storage<T : IndexableItem>(
        name: String,
        private val serializer: KSerializer<T>,
        private val items: MutableList<T> = mutableListOf()
) : List<T> by items {
  private val storageFile = File("data/$name.json")

  init {
    if (!storageFile.exists()) {
      storageFile.createNewFile()
      storageFile.writeText("[]")
    }
    val items = Json.parse(serializer.list, storageFile.readText())
    this.items.removeAll { true }
    items.forEach {
      this.items += it
    }
  }

  private fun write() {
    storageFile.writeText(Json.stringify(serializer.list, items))
  }


  operator fun plusAssign(item: T) {
    this.items += item
    write()
  }

  operator fun minusAssign(id: String) {
    this.items.removeIf { it.id == id }
    write()
  }

  operator fun set(id: String, item: T) {
    this.items.removeIf { it.id == id }
    this.items += item
    write()
  }

}

interface IndexableItem {
  val id: String
}
