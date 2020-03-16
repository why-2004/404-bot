package com.junron.bot404

import com.jessecorbett.diskord.dsl.bot
import com.jessecorbett.diskord.dsl.command
import com.jessecorbett.diskord.dsl.commands
import com.junron.bot404.commands.HwBot
import com.junron.bot404.commands.Pin
import kotlinx.serialization.UnstableDefault

val helpText = """
  **Commands**
  
  **`${config.botPrefix} ping`**
  Check if server is alive
  
  **`${config.botPrefix} help`**
  Displays this message

  **`${config.hwbotPrefix} show`**
  Displays all homework
  
  **`${config.hwbotPrefix} tomorrow`**
  Displays homework due tomorrow
  
  **`${config.hwbotPrefix} subscribe`**
  Subscribe to daily homework reminders at 7pm.
  Only valid in DMs.
  There will be no reminders on Friday and Saturday
  unless there is homework due on the following day.
    
  **`${config.hwbotPrefix} permanent`**
  Displays homework with automatic updates.
  
  **`${config.hwbotPrefix} add`**
  Add homework interactively
  Only valid in DMs
  Admin permission required
  
  **`${config.hwbotPrefix} delete <id>`**
  Delete homework. Obtain id from `${config.hwbotPrefix} show` or `${config.hwbotPrefix} permanent`
  Only valid in DMs
  Admin permission required
""".trimIndent()

@UnstableDefault
suspend fun main() {
  bot(config.discordToken) {
    commands("${config.botPrefix} ") {
      command("help") {
        reply(helpText)
      }
      command("ping") {
        reply("pong")
      }
    }

    commands("${config.hwbotPrefix} "){
      HwBot.init(this@bot, this)
    }

    Pin.init(this)
  }
}
