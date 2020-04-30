package com.junron.bot404.util

import com.joestelmach.natty.Parser
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.*

fun uuid(): String = UUID.randomUUID().toString()
val isoTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")

fun parseDate(date: String) = Parser().parse(date)
    ?.firstOrNull()
    ?.dates
    ?.firstOrNull()

fun String.toDateSimple(): Date = SimpleDateFormat("yyyy-MM-dd").parse(this)
fun Date.toShortString(): String {
    val day = SimpleDateFormat("dd MMM").format(this)
    return when {
        isTomorrow() -> "Tomorrow"
        isToday() -> "Today"
        isThisWeek() -> dayOfWeek.toString().toLowerCase().capitalize()
        else -> day
    }
}

fun Date.toDetailedString(): String {
    val sdf = SimpleDateFormat("dd MMM")
    val sdf2 = SimpleDateFormat("hh:mm a")
    val day: String = when {
        isToday() -> "Today (${sdf.format(this)})"
        isTomorrow() -> "Tomorrow (${sdf.format(this)})"
        isThisWeek() -> dayOfWeek.toString().toLowerCase()
            .capitalize() + " (${sdf.format(this)})"
        else -> sdf.format(this)
    }
    return "$day at ${sdf2.format(this)}"
}

fun String.toDate(): Date =
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'").parse(this)

fun Date.toDateString(): String =
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'").format(this)

fun Date.isFuture() = this.after(Date.from(Instant.now()))
fun Date.toLocalDate(): LocalDate = this.toInstant()
    .atZone(ZoneId.systemDefault())
    .toLocalDate()

fun Date.isTomorrow() = this.toLocalDate()
    .isEqual(LocalDate.now().plusDays(1))

fun Date.isToday() = this.toLocalDate()
    .isEqual(LocalDate.now())

fun Date.isThisWeek() = this.toLocalDate()
    .isBefore(LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.SUNDAY)))

fun Date.isSameTime() = Calendar.getInstance().apply {
    time = this@isSameTime
}.run {
    val now = Calendar.getInstance()
    now.get(Calendar.HOUR_OF_DAY) == get(Calendar.HOUR_OF_DAY) &&
        now.get(Calendar.MINUTE) == get(Calendar.MINUTE)
}

fun Date.isSameDate(date: Date) = Calendar.getInstance().apply {
    time = this@isSameDate
}.run {
    val now = Calendar.getInstance().apply { time = date }
    now.get(Calendar.YEAR) == get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == get(Calendar.DAY_OF_YEAR)
}


val Date.dayOfWeek: DayOfWeek
    get() = this.toLocalDate().dayOfWeek
val Date.isWeekend: Boolean
    get() = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY

@UnstableDefault
val indentedJson = Json(JsonConfiguration(prettyPrint = true))
