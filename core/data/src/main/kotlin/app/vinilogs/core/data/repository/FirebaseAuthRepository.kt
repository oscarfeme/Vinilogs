package app.vinilogs.core.data.repository

import app.vinilogs.core.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

/**
 * [AuthRepository] backed by Firebase Auth, with `signUp` additionally creating the user's
 * `users/{uid}` profile document in Firestore (02-ARCHITECTURE.md §3, T-08's scope). All
 * Firebase SDK exceptions (`FirebaseAuthException` and friends) are caught by [runCatching]
 * and surfaced as `Result.failure` — nothing from the SDK is allowed to throw across the
 * repository boundary.
 */
class FirebaseAuthRepository
    @Inject
    constructor(
        private val firebaseAuth: FirebaseAuth,
        private val firestore: FirebaseFirestore,
    ) : AuthRepository {
        override val currentUser: Flow<User?> =
            callbackFlow {
                val listener =
                    FirebaseAuth.AuthStateListener { auth ->
                        trySend(auth.currentUser?.toDomainUser())
                    }
                firebaseAuth.addAuthStateListener(listener)
                awaitClose { firebaseAuth.removeAuthStateListener(listener) }
            }

        override suspend fun signUp(email: String, password: String, displayName: String): Result<User> =
            runCatching {
                val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser = requireNotNull(authResult.user) { "Firebase returned no user after sign-up" }

                val profileUpdate = UserProfileChangeRequest.Builder().setDisplayName(displayName).build()
                firebaseUser.updateProfile(profileUpdate).await()

                createProfileDocument(uid = firebaseUser.uid, displayName = displayName)

                User(uid = firebaseUser.uid, email = firebaseUser.email.orEmpty(), displayName = displayName)
            }

        override suspend fun signIn(email: String, password: String): Result<User> =
            runCatching {
                val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
                val firebaseUser = requireNotNull(authResult.user) { "Firebase returned no user after sign-in" }
                firebaseUser.toDomainUser()
            }

        override suspend fun sendPasswordReset(email: String): Result<Unit> =
            runCatching {
                firebaseAuth.sendPasswordResetEmail(email).await()
            }

        override suspend fun signOut() {
            firebaseAuth.signOut()
        }

        override suspend fun deleteAccount(): Result<Unit> =
            runCatching {
                val firebaseUser = requireNotNull(firebaseAuth.currentUser) { "No signed-in user to delete" }
                firebaseUser.delete().await()
            }

        /**
         * `users/{uid}` per 02-ARCHITECTURE.md §3: displayName(+Lower), isPublic default (FR-A5:
         * public), recordCount, createdAt.
         */
        private suspend fun createProfileDocument(uid: String, displayName: String) {
            val profile =
                mapOf(
                    "displayName" to displayName,
                    "displayNameLower" to displayName.lowercase(),
                    "isPublic" to true,
                    "recordCount" to 0,
                    "createdAt" to Date(),
                )
            firestore
                .collection(USERS_COLLECTION)
                .document(uid)
                .set(profile)
                .await()
        }

        private companion object {
            const val USERS_COLLECTION = "users"
        }
    }

/** Maps Firebase's [FirebaseUser] to the domain [User] (02-ARCHITECTURE.md §4). */
internal fun FirebaseUser.toDomainUser(): User =
    User(
        uid = uid,
        email = email.orEmpty(),
        displayName = displayName.orEmpty(),
    )
