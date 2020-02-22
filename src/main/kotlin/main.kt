import com.jessecorbett.diskord.dsl.bot
import com.jessecorbett.diskord.dsl.command
import com.jessecorbett.diskord.dsl.commands
import commands.*
import kotlinx.serialization.UnstableDefault
import java.io.File

val helpText = """
  Commands
  
  `!404 ping`
  Check if server is alive
  
  `!404 search <name|id|index no>`
  Search class list for student
  
  `!404 classlist`
  Print full classlist
  
  `!404 whois <mention|discord username>`
  Prints information about mentioned user
""".trimIndent()

@UnstableDefault
suspend fun main() {
  val token = File("secrets/token.txt").readText().trim()
  bot(token) {
    commands("!404 ") {
      command("help") {
        reply(helpText)
      }
      command("ping") {
        reply("pong")
      }
      Classlist.init(this@bot, this)
      Poll.init(this@bot, this)
      Reminders.init(this@bot, this)
    }

    commands("!hwbot "){
      HwBot.init(this@bot, this)
    }

    Pin.init(this)
    Reactions.init(this)
  }
}
