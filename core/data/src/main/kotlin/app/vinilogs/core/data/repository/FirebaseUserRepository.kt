package app.vinilogs.core.data.repository

import app.vinilogs.core.model.ProfileUpdate
import app.vinilogs.core.model.PublicRecord
import app.vinilogs.core.model.Record
import app.vinilogs.core.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.time.Instant
import javax.inject.Inject

/**
 * [UserRepository] backed by Firestore's `users/{uid}` document (02-ARCHITECTURE.md §3). Only
 * [observeProfile] and [updateProfile] are real -- the profile-viewing/editing half FR-A4/A5
 * actually needs (T-19's scope). `searchUsers`, `observePublicCollection`, `sharedRecords` and
 * `report` are Phase 2/T-22 (discovery repository) concerns and are stubbed the same way
 * [RoomCollectionRepository]'s not-yet-relevant methods are: a documented [Result.failure] (or,
 * for the one `Flow`-returning method, a [flow] that throws on collection) pointing at the task
 * that will implement them.
 */
class FirebaseUserRepository
    @Inject
    constructor(
        private val firestore: FirebaseFirestore,
        private val firebaseAuth: FirebaseAuth,
    ) : UserRepository {
        override fun observeProfile(uid: String): Flow<UserProfile?> =
            callbackFlow {
                val registration =
                    firestore
                        .collection(USERS_COLLECTION)
                        .document(uid)
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                // No screen owned by this task treats a listener error as fatal --
                                // matches this repository's "never throw across the boundary" style.
                                trySend(null)
                                return@addSnapshotListener
                            }
                            trySend(snapshot?.toUserProfile())
                        }
                awaitClose { registration.remove() }
            }

        override fun observePublicCollection(uid: String): Flow<List<PublicRecord>> =
            flow {
                throw NotImplementedError(
                    "observePublicCollection needs T-22's publicRecords projection reads -- " +
                        "lands with the discovery repository work",
                )
            }

        override suspend fun searchUsers(query: String, page: Int): Result<List<UserProfile>> =
            Result.failure(
                NotImplementedError(
                    "searchUsers needs T-22's displayNameLower prefix search -- lands with the " +
                        "discovery repository work",
                ),
            )

        /**
         * Writes the editable subset of the `users/{uid}` document (FR-A4/A5). Uses
         * [SetOptions.merge] rather than a plain `set()` -- [ProfileUpdate] deliberately doesn't
         * carry `recordCount`/`createdAt` (those are server/sign-up-time denormalised fields, not
         * user-editable), and a non-merging write would silently wipe them.
         */
        override suspend fun updateProfile(update: ProfileUpdate): Result<Unit> =
            runCatching {
                val uid =
                    requireNotNull(firebaseAuth.currentUser?.uid) {
                        "No signed-in user to update a profile for"
                    }
                val payload =
                    mapOf(
                        "displayName" to update.displayName,
                        "displayNameLower" to update.displayName.lowercase(),
                        "avatarUrl" to update.avatarUrl,
                        "bio" to update.bio,
                        "location" to update.location,
                        "isPublic" to update.isPublic,
                    )
                firestore
                    .collection(USERS_COLLECTION)
                    .document(uid)
                    .set(payload, SetOptions.merge())
                    .await()
            }

        override suspend fun sharedRecords(otherUid: String): Result<List<Record>> =
            Result.failure(
                NotImplementedError(
                    "sharedRecords needs T-22's local shared-record computation -- lands with the " +
                        "discovery repository work",
                ),
            )

        override suspend fun report(reportedUid: String, reason: String): Result<Unit> =
            Result.failure(
                NotImplementedError(
                    "report needs T-22's reports/{reportId} writes -- lands with the discovery " +
                        "repository work",
                ),
            )

        private companion object {
            const val USERS_COLLECTION = "users"
        }
    }

/**
 * Maps a `users/{uid}` [DocumentSnapshot] to the domain [UserProfile], or `null` if the document
 * doesn't exist (e.g. profile not yet created). `isPublic` defaults to `true` (FR-A5: "default
 * public") if the field is somehow missing, matching [FirebaseAuthRepository]'s sign-up write.
 */
internal fun DocumentSnapshot.toUserProfile(): UserProfile? {
    if (!exists()) return null
    return UserProfile(
        uid = id,
        displayName = getString("displayName").orEmpty(),
        avatarUrl = getString("avatarUrl"),
        bio = getString("bio"),
        location = getString("location"),
        isPublic = getBoolean("isPublic") ?: true,
        recordCount = (getLong("recordCount") ?: 0L).toInt(),
        createdAt = getDate("createdAt")?.toInstant() ?: Instant.EPOCH,
    )
}
