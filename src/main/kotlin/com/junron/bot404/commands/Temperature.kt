package com.junron.bot404.commands

import com.jessecorbett.diskord.api.rest.CreateMessage
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CommandSet
import com.jessecorbett.diskord.dsl.command
import com.jessecorbett.diskord.util.authorId
import com.junron.bot404.model.Subscriber
import com.junron.bot404.util.*
import com.junron.pyrobase.dateutils.isSchoolDay
import com.junron.pyrobase.jsoncache.Storage
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class Time(val hour: Int, val minute: Int)

@Serializable
data class TemperatureSubscriber(
    val userName: String, override val authorId: String,
    override val timings: List<Time> = listOf(Time(8, 0))
) : Subscriber()

object Temperature : Command {
    private val subscribers =
        Storage("temp_subscribers", TemperatureSubscriber.serializer())
    private lateinit var reminders: ScheduledReminders<TemperatureSubscriber>
    override fun init(bot: Bot, prefix: CommandSet) {
        with(bot) {
            with(prefix) {
                reminders = ScheduledReminders(subscribers, bot) { it, _ ->
                    if (!Date().isSchoolDay()) return@ScheduledReminders
                    runBlocking {
                        bot.dmUser(
                            it.authorId,
                            CreateMessage(content = "Please take your temperature. https://forms.office.com/Pages/ResponsePage.aspx?id=cnEq1_jViUiahddCR1FZKi_YUnieBUBCi4vce5KjIHVUMkoxVUdBMVo2VUJTNFlSU1dFNEtNWUwxNS4u")
                        )
                    }
                }
                Subscriptions.init(
                    bot,
                    this,
                    subscribers,
                    reminders,
                    """
                  You will be reminded to submit your temperature at 8am.
                  **Commands:**
                  `!temperature config` to change reminder time
                  `!temperature reminders` to list reminders
                """.trimIndent(),
                    "You are not subscribed. Send `!temperature subscribe` to subscribe."
                ) {
                    TemperatureSubscriber(it.author.username, it.authorId)
                }
                command("config") {
                    if (guildId != null) return@command bot reject this
                    val subscriberId =
                        subscribers.find { it.authorId == authorId }
                            ?.id
                            ?: return@command run {
                                reply("You are not subscribed. Send `!temperature subscribe` to subscribe.")
                            }
                    with(Conversation(listOf<Time>())) {
                        val question =
                            TimeQuestion("Please enter reminder time (24 hour hh:mm)") {
                                state += it
                                next()
                            }
                        lateinit var hasNext: BooleanQuestion
                        hasNext = BooleanQuestion("Add another reminder? ") {
                            if (it) {
                                addQuestion(question)
                                addQuestion(hasNext)
                            }
                            next()
                        }
                        init(
                            bot, channelId, listOf(question, hasNext,
                                Done("Reminders updated!") {
                                    subscribers -= subscriberId
                                    subscribers += TemperatureSubscriber(
                                        author.username,
                                        authorId,
                                        state
                                    )
                                    reply(
                                        "**Reminders**\n" + state.joinToString(
                                            "\n"
                                        ) {
                                            "${it.hour.toString().padStart(
                                                2,
                                                '0'
                                            )}:${it.minute.toString()
                                                .padStart(2, '0')}"
                                        })
                                    reminders.updateSubscriptions(subscribers)
                                })
                        )
                    }
                }
            }
        }
    }
}

