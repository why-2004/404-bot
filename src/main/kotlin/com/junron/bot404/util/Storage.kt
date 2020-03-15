package com.junron.bot404.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.list
import java.io.File


class Storage<T>(
        val name: String,
        internal val serializer: KSerializer<T>,
        private val items: MutableList<ItemWrapper<T>> = mutableListOf()
) : List<ItemWrapper<T>> by items {
  private val storageFile = File("data/$name.json")

  init {
    if (!storageFile.exists()) {
      storageFile.createNewFile()
      storageFile.writeText("[]")
    }
    val items = Json.parse(ItemWrapper.serializer(serializer).list, storageFile.readText())
    this.items.removeAll { true }
    items.forEach {
      this.items += it
    }
  }

  private fun write() {
    GlobalScope.launch(Dispatchers.IO) {
      storageFile.writeText(Json.stringify(ItemWrapper.serializer(serializer).list, items))
    }
  }


  operator fun plusAssign(item: T) {
    this.items += ItemWrapper(uuid(), item)
    write()
  }

  operator fun minusAssign(id: String) {
    this.items.removeIf { it.id == id }
    write()
  }

  operator fun set(id: String, item: T) {
    this.items.removeIf { it.id == id }
    this.items += ItemWrapper(id, item)
    write()
  }

}

@Serializable
data class ItemWrapper<T>(val id: String, val item: T)
