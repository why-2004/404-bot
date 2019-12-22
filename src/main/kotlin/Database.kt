import com.jessecorbett.diskord.api.rest.Embed
import com.toddway.shelf.FileStorage
import com.toddway.shelf.KotlinxSerializer
import com.toddway.shelf.Shelf
import com.toddway.shelf.get
import java.io.File
import kotlinx.serialization.Serializable

object Database {
  private val database = Shelf(FileStorage(File("secrets/data")), KotlinxSerializer().apply {
    register(Query.serializer())
  })

  operator fun get(id: String) =
    database.item(id).get<Query>()

  operator fun minusAssign(id: String) {
    database.item(id).remove()
  }

  operator fun set(id: String, query: Query) {
    database.item(id).put(query)
  }
}

@Serializable
data class Query(val query: String, val site: String, val userId: String) {
  var answerNumber = -1
    private set

  lateinit var cache: List<Embed>
  fun increment(): Boolean {
    answerNumber++
    if (answerNumber >= cache.size) {
      answerNumber--
      return false
    }
    return true
  }

  fun decrement(): Boolean {
    answerNumber--
    if (answerNumber < 0) {
      answerNumber++
      return false
    }
    return true
  }
}
