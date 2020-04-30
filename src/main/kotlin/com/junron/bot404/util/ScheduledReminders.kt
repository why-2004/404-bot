package com.junron.bot404.util

import com.github.shyiko.skedule.Schedule
import com.jessecorbett.diskord.dsl.Bot
import com.junron.bot404.commands.Time
import com.junron.bot404.model.Subscriber
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.*
import kotlin.concurrent.fixedRateTimer

class ScheduledReminders<T : Subscriber>(
    var subscribers: List<T>,
    private val bot: Bot,
    val callback: (subscriber: T, bot: Bot) -> Unit
) {
    lateinit var timers: List<Timer>

    init {
        updateSubscriptions(subscribers)
    }

    fun updateSubscriptions(subscribers: List<T>) {
        this.subscribers = subscribers
        if (::timers.isInitialized) timers.forEach { it.cancel() }
        val timings = mutableMapOf<Time, MutableList<T>>()
        subscribers.forEach { subscriber ->
            subscriber.timings.forEach {
                if (timings.containsKey(it)) {
                    timings[it]?.plusAssign(subscriber)
                } else {
                    timings[it] = mutableListOf(subscriber)
                }
            }
        }
        timers = timings.map { (time, subscribers) ->
            fixedRateTimer(
                uuid(),
                false,
                (Schedule.at(LocalTime.of(time.hour, time.minute))
                    .everyDay()
                    .next(ZonedDateTime.now())
                    .toEpochSecond() - ZonedDateTime.now()
                    .toEpochSecond()) * 1000,
                8.64e+7.toLong()
            ) {
                subscribers.forEach {
                    callback(it, bot)
                }
            }
        }
    }
}
