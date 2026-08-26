package app.vinilogs.core.testing.fake

import app.vinilogs.core.data.repository.UserRepository
import app.vinilogs.core.model.ProfileUpdate
import app.vinilogs.core.model.PublicRecord
import app.vinilogs.core.model.Record
import app.vinilogs.core.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * In-memory [UserRepository] fake, seeded from [initialProfiles], [initialPublicRecords] and
 * [initialSharedRecords]. [activeUid] is which profile [updateProfile] edits -- the fake has
 * no other notion of "the signed-in user."
 */
class FakeUserRepository(
    initialProfiles: List<UserProfile> = emptyList(),
    private val initialPublicRecords: Map<String, List<PublicRecord>> = emptyMap(),
    private val initialSharedRecords: Map<String, List<Record>> = emptyMap(),
) : UserRepository {
    private val profilesFlow = MutableStateFlow(initialProfiles.associateBy { it.uid })
    private var nextFailure: Throwable? = null
    private val _reports = mutableListOf<Pair<String, String>>()

    var activeUid: String? = initialProfiles.firstOrNull()?.uid

    /** Every `(reportedUid, reason)` passed to [report] so far, for assertions. */
    val reports: List<Pair<String, String>> get() = _reports

    fun failNextCallWith(error: Throwable) {
        nextFailure = error
    }

    private fun <T> consumeFailureOr(onSuccess: () -> T): Result<T> {
        val failure = nextFailure
        if (failure != null) {
            nextFailure = null
            return Result.failure(failure)
        }
        return runCatching(onSuccess)
    }

    override fun observeProfile(uid: String): Flow<UserProfile?> = profilesFlow.map { it[uid] }

    override fun observePublicCollection(uid: String): Flow<List<PublicRecord>> =
        profilesFlow.map { initialPublicRecords[uid].orEmpty() }

    override suspend fun searchUsers(query: String, page: Int): Result<List<UserProfile>> =
        consumeFailureOr {
            profilesFlow.value.values
                .filter { it.isPublic && it.displayName.contains(query, ignoreCase = true) }
                .sortedBy { it.displayName }
        }

    override suspend fun updateProfile(update: ProfileUpdate): Result<Unit> =
        consumeFailureOr {
            val uid = requireNotNull(activeUid) { "No active user set on this fake -- set activeUid first" }
            val current = requireNotNull(profilesFlow.value[uid]) { "No profile for $uid" }
            val updated = current.copy(
                displayName = update.displayName,
                avatarUrl = update.avatarUrl,
                bio = update.bio,
                location = update.location,
                isPublic = update.isPublic,
            )
            profilesFlow.update { it + (uid to updated) }
        }

    override suspend fun sharedRecords(otherUid: String): Result<List<Record>> =
        consumeFailureOr { initialSharedRecords[otherUid].orEmpty() }

    override suspend fun report(reportedUid: String, reason: String): Result<Unit> =
        consumeFailureOr { _reports += reportedUid to reason }
}
