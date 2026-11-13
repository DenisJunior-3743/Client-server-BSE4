package com.trustbank.loanapp.util

import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object Formatters {
    private val amountFormat: NumberFormat = NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 0 }
    fun ugx(amount: Number): String = "USh ${amountFormat.format(amount)}"

    /** Same as [ugx], but masked when the user has hidden sensitive amounts (see `AppContainer.visibility`). */
    fun maskedUgx(amount: Number, visible: Boolean): String = if (visible) ugx(amount) else "USh ••••••••"

    private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    fun date(date: LocalDate): String = date.format(dateFormatter)

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
    fun dateTime(dateTime: LocalDateTime): String = dateTime.format(dateTimeFormatter)

    fun initials(fullName: String): String =
        fullName.trim().split(Regex("\\s+")).take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
}
