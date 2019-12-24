package util

import com.willowtreeapps.fuzzywuzzy.diffutils.FuzzySearch
import kotlinx.serialization.Serializable
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.list
import java.io.File


@UnstableDefault
object ClassLists {
  private lateinit var cache: List<Student>

  fun load(): List<Student> {
    if (ClassLists::cache.isInitialized) return cache
    val data = File("secrets/classlist.json").readText()
    cache = Json.nonstrict.parse(Student.serializer().list, data)
        .mapIndexed { index, student ->
          student.copy(index = index + 1)
        }
    return cache
  }

  fun search(query: String): List<Student> {
    return listOf(
        FuzzySearch.extractSorted(query, load().map { it.name }, 70),
        FuzzySearch.extractSorted(query, load().map { it.discord }, 70),
        FuzzySearch.extractSorted(query, load().map { it.index.toString() }, 70),
        FuzzySearch.extractSorted(query, load().map { it.id }, 70)
    ).flatten().take(3).map {
      load()[it.index]
    }.distinct()
  }

  fun getStudentByDiscordUsername(query: String) =
      load().firstOrNull {
        it.discord.toLowerCase() == query.toLowerCase()
      }

}

@Serializable
data class Student(
    val index: Int = 0,
    val name: String,
    val id: String,
    val combination: String,
    val discord: String
)
