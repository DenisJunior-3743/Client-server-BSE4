package com.trustbank.loanapp.util

import java.time.LocalDate
import java.time.Period

// Mirrors web/src/lib/phone.ts field-for-field so both clients enforce the
// exact same Uganda phone rule: strictly "07" + 8 more digits.
object Validators {
    const val PHONE_MAX_LENGTH = 10
    private val PHONE_PATTERN = Regex("^07\\d{8}$")
    const val PHONE_ERROR_MESSAGE = "Phone number must start with 07 and have exactly 10 digits"

    /**
     * Strips everything but digits and caps the result at 10 characters.
     * Call this from a TextField's onValueChange (not just on submit) so a
     * letter/symbol can never appear in the field, and typing past 10
     * digits is simply ignored.
     */
    fun sanitizePhoneInput(raw: String): String = raw.filter { it.isDigit() }.take(PHONE_MAX_LENGTH)

    fun isValidUgandaPhone(value: String): Boolean = PHONE_PATTERN.matches(value)

    private val EMAIL_PATTERN = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    fun isValidEmail(value: String): Boolean = EMAIL_PATTERN.matches(value.trim())

    fun isRequired(value: String): Boolean = value.trim().isNotEmpty()

    fun isStrongPassword(value: String): Boolean =
        value.length >= 8 && value.any { it.isDigit() } && value.any { it.isUpperCase() } && value.any { it.isLowerCase() }

    /**
     * 0-100 score across five criteria (length, lowercase, uppercase, digit,
     * symbol) — drives the strength meter shown while a password is typed.
     */
    fun passwordStrengthPercent(value: String): Int {
        if (value.isEmpty()) return 0
        var score = 0
        if (value.length >= 8) score++
        if (value.any { it.isLowerCase() }) score++
        if (value.any { it.isUpperCase() }) score++
        if (value.any { it.isDigit() }) score++
        if (value.any { !it.isLetterOrDigit() }) score++
        return score * 100 / 5
    }

    fun passwordStrengthLabel(percent: Int): String = when {
        percent <= 0 -> ""
        percent < 40 -> "Weak"
        percent < 70 -> "Fair"
        percent < 100 -> "Good"
        else -> "Strong"
    }

    fun isAdult(dateOfBirth: LocalDate, minAge: Int = 18): Boolean =
        Period.between(dateOfBirth, LocalDate.now()).years >= minAge

    fun parseAmount(value: String): Double? = value.trim().toDoubleOrNull()

    fun sanitizeDigitsOnly(raw: String, maxLength: Int): String = raw.filter { it.isDigit() }.take(maxLength)

    private val NON_NAME_CHAR = Regex("[^A-Za-z ]")

    /** Strips digits/symbols as the user types — a full name is letters and spaces only. */
    fun sanitizeNameInput(raw: String): String = raw.replace(NON_NAME_CHAR, "")

    fun isValidFullName(value: String): Boolean = value.trim().length >= 2
}
