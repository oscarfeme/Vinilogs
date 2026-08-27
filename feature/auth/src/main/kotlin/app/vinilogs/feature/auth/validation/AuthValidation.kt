package app.vinilogs.feature.auth.validation

/**
 * Client-side form validation shared by the sign-in, sign-up and forgot-password
 * ViewModels.
 *
 * Password policy assumption (CLAUDE.md rule 7 / 00-README.md's ambiguity rule):
 * 01-REQUIREMENTS.md's FR-A1 states "Password >= 8 chars" for sign-up specifically
 * and is silent on sign-in. [validatePasswordForSignUp] enforces that floor;
 * [validatePasswordForSignIn] only requires a non-empty value, since an existing
 * account's password is Firebase Auth's concern to accept or reject, not a shape
 * this client should second-guess.
 */

private val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

const val MIN_PASSWORD_LENGTH = 8

fun validateEmail(email: String): String? =
    when {
        email.isBlank() -> "Enter your email."
        !EMAIL_REGEX.matches(email.trim()) -> "Enter a valid email address."
        else -> null
    }

fun validatePasswordForSignUp(password: String): String? =
    when {
        password.isEmpty() -> "Enter a password."
        password.length < MIN_PASSWORD_LENGTH -> "Password must be at least $MIN_PASSWORD_LENGTH characters."
        else -> null
    }

fun validatePasswordForSignIn(password: String): String? = if (password.isEmpty()) "Enter your password." else null

fun validateDisplayName(displayName: String): String? = if (displayName.isBlank()) "Enter your name." else null
