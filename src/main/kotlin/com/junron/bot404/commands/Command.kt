package com.junron.bot404.commands

import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CommandSet

interface Command {
  fun init(bot: Bot, prefix: CommandSet)
}
