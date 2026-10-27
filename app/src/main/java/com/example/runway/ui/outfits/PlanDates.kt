package com.example.runway.ui.outfits

import android.content.Context
import com.example.runway.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// "Today", "Tomorrow", otherwise something like "Mon 22 Sep".
object PlanDates {

    fun label(context: Context, date: LocalDate, today: LocalDate = LocalDate.now()): String = when (date) {
        today -> context.getString(R.string.rw_plan_today)
        today.plusDays(1) -> context.getString(R.string.rw_plan_tomorrow)
        else -> date.format(DateTimeFormatter.ofPattern("EEE d MMM", locale(context)))
    }

    fun monthTitle(context: Context, month: java.time.YearMonth): String =
        month.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale(context)))

    // Uses the language picked in the app, which is not always the phone's language.
    fun locale(context: Context): Locale = context.resources.configuration.locales[0]
}
