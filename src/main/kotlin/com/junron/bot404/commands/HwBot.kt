package com.junron.bot404.commands

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.api.rest.CreateMessage
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CommandSet
import com.jessecorbett.diskord.dsl.command
import com.jessecorbett.diskord.dsl.embed
import com.jessecorbett.diskord.util.Colors
import com.jessecorbett.diskord.util.authorId
import com.jessecorbett.diskord.util.words
import com.junron.bot404.Config.Companion.config
import com.junron.bot404.firebase.HwboardFirestore
import com.junron.bot404.firebase.HwboardFirestore.deleteHomework
import com.junron.bot404.firebase.HwboardFirestore.getHomework
import com.junron.bot404.firebase.HwboardFirestore.hwboardConfig
import com.junron.bot404.model.Homework
import com.junron.bot404.model.HomeworkNullable
import com.junron.bot404.model.Subscriber
import com.junron.bot404.util.*
import com.junron.pyrobase.dateutils.isSchoolDay
import com.junron.pyrobase.jsoncache.IndexableItem
import com.junron.pyrobase.jsoncache.Storage
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.UnstableDefault
import java.util.*
import kotlin.concurrent.fixedRateTimer


@Serializable
data class HwBotSubscriber(
    val userName: String,
    override val authorId: String,
    override val timings: List<Time> = listOf(Time(19, 0)),
    val excludeSubjects: List<String> = emptyList()
) : Subscriber()

object HwBot : Command {
    private val subscribers =
        Storage("subscribers", HwBotSubscriber.serializer())

    @ExperimentalStdlibApi
    @UnstableDefault
    override fun init(bot: Bot, prefix: CommandSet) {
        HwboardFirestore.addListener {
            updatePermanent(bot)
        }
        val subscriberReminders =
            ScheduledReminders(subscribers, bot) { subscriber, _ ->
                val homework =
                    getHomework().filter { it.subject !in subscriber.excludeSubjects }
                if (homework.tomorrow.isEmpty() && !Calendar.getInstance()
                        .apply { add(Calendar.DATE, 1) }.time.isSchoolDay()
                ) return@ScheduledReminders
                val embed = buildHomeworkEmbeds(homework.tomorrow).firstOrNull()
                    ?: embed {
                        title = "There is no homework due tomorrow"
                        color = Colors.GREEN
                    }
                runBlocking {
                    bot.dmUser(
                        subscriber.authorId,
                        CreateMessage(content = "", embed = embed)
                    )
                }
            }
        Subscriptions.init(bot, prefix, subscribers, subscriberReminders) {
            HwBotSubscriber(
                it.author.username,
                it.authorId,
                listOf(Time(19, 0))
            )
        }
        with(bot) {
            with(prefix) {
                command("config") {
                    if (guildId != null) return@command bot reject this
                    val subscriber =
                        subscribers.find { it.authorId == authorId }
                            ?: return@command run {
                                reply("You are not subscribed. Send `${config.hwbotPrefix} subscribe` to subscribe.")
                            }
                    with(Conversation(subscriber)) {
                        init(
                            bot,
                            channelId,
                            listOf(
                                TimeQuestion("Please enter reminder time (24 hour hh:mm)") {
                                    state = state.copy(timings = listOf(it))
                                    next()
                                },
                                MultipleChoiceQuestion(
                                    "Enter subjects you do not want to be notified about, separated by commas. Enter - to skip.",
                                    hwboardConfig.subjects
                                ) {
                                    state = state.copy(excludeSubjects = it)
                                    next()
                                },
                                Done("Reminders updated!") {
                                    subscribers -= state.id
                                    subscribers += state
                                    reply(
                                        "**Reminders**\n" + state.timings.joinToString(
                                            "\n"
                                        ) {
                                            "${it.hour.toString().padStart(
                                                2,
                                                '0'
                                            )}:${it.minute.toString()
                                                .padStart(2, '0')}"
                                        })
                                    subscriberReminders.updateSubscriptions(
                                        subscribers
                                    )
                                })
                        )
                    }
                }
                command("show") {
                    val homework = getHomework()
                    reply(
                        "",
                        embed = combineEmbeds(buildHomeworkEmbeds(homework))
                    )
                }
                command("permanent") {
                    val homework = getHomework()
                    val id = reply(
                        "",
                        embed = combineEmbeds(buildHomeworkEmbeds(homework))
                    ).id
                    permanentMessageStorage += PermanentMessage(channelId, id)
                }
                command("tomorrow") {
                    val homework = getHomework().tomorrow
                    val embed =
                        buildHomeworkEmbeds(homework).firstOrNull() ?: embed {
                            title = "There is no homework tomorrow"
                            color = Colors.GREEN
                        }
                    reply("", embed = embed)
                }
                command("delete") {
                    val homework =
                        getSelectedHomework(bot, this) ?: return@command
                    if (words.getOrNull(3) == "--force") {
                        deleteHomework(homework)
                        updatePermanent(bot)
                        return@command bot accept this
                    }
                    with(Conversation(HomeworkNullable())) {
                        init(bot, channelId, listOf(TextQuestion(
                            "Are you sure you want to delete '${homework.text}'? Type 'y' to confirm."
                        ) {
                            if (it.toLowerCase().trim() == "y") {
                                deleteHomework(homework)
                                next()
                                updatePermanent(bot)
                                return@TextQuestion
                            }
                            bot.reject(this@command, "Cancelled")
                            cancel()
                        }, Done("Homework deleted") {})
                        )
                    }
                }

                command("info") {
                    val homework =
                        getSelectedHomework(bot, this, false) ?: return@command
                    reply("", embed = homework.generateEmbed())
                }

                command("edit") {
                    val homework =
                        getSelectedHomework(bot, this) ?: return@command
                    val fields = listOf("Subject", "Text", "Due date", "Tags")
                    reply("Editing ${homework.text}")
                    with(Conversation(homework)) {
                        init(
                            bot,
                            channelId,
                            listOf(
                                MultipleChoiceQuestion(
                                    "Select fields you want to edit, separated by commas.",
                                    fields
                                ) { selectedFields ->
                                    if ("Subject" in selectedFields) addQuestion(
                                        ChoiceQuestion(
                                            "Select subject: ",
                                            hwboardConfig.subjects
                                        ) {
                                            state = state.copy(subject = it)
                                            next()
                                        })
                                    if ("Text" in selectedFields) addQuestion(
                                        TextQuestion("Enter text: ") {
                                            state = state.copy(text = it)
                                            next()
                                        })
                                    if ("Due date" in selectedFields) addQuestion(
                                        DateQuestion("Enter due date: ", true) {
                                            var date = it
                                            if (it.isSameTime()) {
                                                date = Timetable.setLessonTime(
                                                    state.subject,
                                                    date
                                                )
                                            }
                                            state =
                                                state.copy(dueDate = date.toDateString())
                                            next()
                                        })
                                    if ("Tags" in selectedFields) addQuestion(
                                        MultipleChoiceQuestion(
                                            "Please enter tags numbers, separated by commas. Enter '-' for no tags.",
                                            HwboardFirestore.getTags().map { it.name }
                                        ) {
                                            val tags =
                                                HwboardFirestore.getTags().filter {tag ->
                                                    tag.name in it
                                                }.map { it.id }
                                            state = state.copy(tags = tags)
                                            next()
                                        })
                                    addQuestion(Done("Homework edited successfully") {
                                        state = state.copy(
                                            lastEditPerson = author.username,
                                            lastEditTime = Date().toDateString()
                                        )
                                        reply("", embed = state.generateEmbed())
                                        HwboardFirestore.updateHomework(state)
                                        updatePermanent(bot)
                                    })
                                    next()
                                })
                        )
                    }
                }
                command("add") {
                    val config = hwboardConfig
                    if (clientStore.guilds[config.guild].getMember(authorId).roleIds
                            .intersect(config.editRoles)
                            .isEmpty() || guildId != null
                    ) return@command bot reject this
                    with(Conversation(HomeworkNullable(id = uuid()))) {
                        init(
                            bot, channelId, listOf(
                                ChoiceQuestion(
                                    "Select subject: ",
                                    config.subjects
                                ) {
                                    state = state.copy(subject = it)
                                    next()
                                },
                                DateQuestion("Enter due date: ", true) {
                                    var date = it
                                    if (it.isSameTime()) {
                                        date = Timetable.setLessonTime(
                                            state.subject!!,
                                            date
                                        )
                                    }
                                    state = state.copy(dueDate = date)
                                    next()
                                },
                                TextQuestion("Enter homework text: ") {
                                    state = state.copy(text = it)
                                    next()
                                },
                                MultipleChoiceQuestion(
                                    "Please enter tags numbers, separated by commas. Enter '-' for no tags.",
                                    HwboardFirestore.getTags().map { it.name }
                                ) {
                                    val tags =
                                        HwboardFirestore.getTags().filter {tag ->
                                            tag.name in it
                                        }
                                    state = state.copy(tags = tags)
                                    next()
                                },
                                Done("Homework added successfully") {
                                    state = state.copy(
                                        lastEditPerson = author.username,
                                        lastEditTime = Date()
                                    )
                                    reply(
                                        "",
                                        embed = state.toHomework()
                                            .generateEmbed()
                                    )
                                    HwboardFirestore.updateHomework(state.toHomework())
                                    updatePermanent(bot)
                                })
                        )
                    }
                }
            }
        }
    }

    private suspend fun getSelectedHomework(
        bot: Bot,
        message: Message,
        adminOnly: Boolean = true
    ): Homework? {
        with(bot) {
            with(message) {
                val config = hwboardConfig
                if (adminOnly && clientStore.guilds[config.guild].getMember(
                        authorId
                    ).roleIds
                        .intersect(config.editRoles)
                        .isEmpty() || guildId != null
                ) return run {
                    bot reject this
                    null
                }
                val index = words.getOrNull(2)?.toIntOrNull()
                    ?: return run {
                        bot reject this
                        null
                    }
                return getHomework().sortedBy { it.dueDate }.getOrNull(index)
                    ?: return run {
                        bot.reject(
                            this,
                            "$index is not a valid homework index."
                        )
                        null
                    }
            }
        }
    }
}

@Serializable
data class PermanentMessage(val channel: String, val messageId: String) :
    IndexableItem {
    override val id: String
        get() = messageId
}
