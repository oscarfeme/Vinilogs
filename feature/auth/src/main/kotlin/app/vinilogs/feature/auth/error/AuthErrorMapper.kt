package app.vinilogs.feature.auth.error

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

/**
 * Maps a failed sign-in/sign-up [Throwable] to copy a user can act on.
 *
 * Firebase Auth's SDK exposes specific exception types (wrong password, email
 * already registered, weak password, ...) that let this be more helpful than a
 * generic message. Everything else -- including every failure
 * `core:testing`'s `FakeAuthRepository` throws today, since the real
 * Firebase-backed `AuthRepository` (T-08) isn't implemented yet -- falls back to
 * [fallback]. This mapping is therefore written against the fixed contract but
 * only exercised end-to-end once T-08 lands; until then only the fallback path
 * is reachable in tests.
 */
fun Throwable.toAuthErrorMessage(fallback: String = "Something went wrong. Please try again."): String =
    when (this) {
        is FirebaseAuthUserCollisionException -> "An account with this email already exists."
        is FirebaseAuthWeakPasswordException -> "Choose a stronger password (at least 8 characters)."
        is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password."
        is FirebaseAuthInvalidUserException -> "No account found for this email."
        is FirebaseTooManyRequestsException -> "Too many attempts. Please wait a moment and try again."
        is FirebaseNetworkException -> "No connection. Check your network and try again."
        else -> fallback
    }
