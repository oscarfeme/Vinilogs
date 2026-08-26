package app.vinilogs.core.data.repository

import app.vinilogs.core.model.ProfileUpdate
import app.vinilogs.core.model.PublicRecord
import app.vinilogs.core.model.Record
import app.vinilogs.core.model.UserProfile
import kotlinx.coroutines.flow.Flow

/**
 * Fixed contract per 02-ARCHITECTURE.md §4 — implement against this so feature agents can
 * work in parallel with core:testing's fakes. Real implementation lands in T-13/discovery
 * data-source work.
 */
interface UserRepository {
    fun observeProfile(uid: String): Flow<UserProfile?>

    fun observePublicCollection(uid: String): Flow<List<PublicRecord>>

    suspend fun searchUsers(query: String, page: Int): Result<List<UserProfile>>

    suspend fun updateProfile(update: ProfileUpdate): Result<Unit>

    suspend fun sharedRecords(otherUid: String): Result<List<Record>>

    suspend fun report(reportedUid: String, reason: String): Result<Unit>
}
