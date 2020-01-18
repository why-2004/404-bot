import com.toddway.shelf.FileStorage
import com.toddway.shelf.KotlinxSerializer
import com.toddway.shelf.Shelf
import com.toddway.shelf.get
import commands.Reminder
import kotlinx.serialization.UnstableDefault
import java.io.File

@UnstableDefault
object RemindersDB {
  private val database = Shelf(FileStorage(File("secrets/databases/reminders")), KotlinxSerializer().apply {
    register(Reminder.serializer())
  })

  fun getAll() = database.all().toList().map { it.get<Reminder>()!! }

  operator fun minusAssign(id: String) {
    database.item(id).remove()
  }

  operator fun set(id: String, reminder: Reminder) {
    database.item(id).put(reminder)
  }
}

