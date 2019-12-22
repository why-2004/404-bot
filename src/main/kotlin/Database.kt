import com.toddway.shelf.FileStorage
import com.toddway.shelf.KotlinxSerializer
import com.toddway.shelf.Shelf
import com.toddway.shelf.get
import kotlinx.serialization.UnstableDefault
import util.PaginatedList
import java.io.File

@UnstableDefault
object Database {
  private val database = Shelf(FileStorage(File("secrets/data")), KotlinxSerializer().apply {
    register(PaginatedList.serializer())
  })

  operator fun get(id: String) =
    database.item(id).get<PaginatedList>()

  operator fun minusAssign(id: String) {
    database.item(id).remove()
  }

  operator fun set(id: String, list: PaginatedList) {
    database.item(id).put(list)
  }
}

