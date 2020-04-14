package com.junron.bot404.commands

import com.jessecorbett.diskord.dsl.*

interface Command {
  fun init(bot: Bot, prefix: CommandSet)
}
