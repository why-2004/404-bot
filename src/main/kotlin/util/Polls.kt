package util

import com.jessecorbett.diskord.api.model.Emoji
import com.willowtreeapps.fuzzywuzzy.diffutils.FuzzySearch
import kotlinx.serialization.Serializable
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.list
import java.io.File


@UnstableDefault
object Polls {

}

@Serializable
data class VoteOption(
    val students: List<Student>,
    val emoji: Emoji
)
