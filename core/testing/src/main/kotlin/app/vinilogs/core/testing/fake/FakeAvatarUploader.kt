package app.vinilogs.core.testing.fake

import android.net.Uri
import app.vinilogs.core.data.repository.AvatarUploader

/**
 * In-memory [AvatarUploader] fake. [upload] returns a deterministic fake URL derived from [uid]
 * and the uploaded [Uri] rather than touching Firebase Storage. Call [failNextCallWith] to make
 * the next [upload] return a failure, for testing error paths (T-19).
 */
class FakeAvatarUploader : AvatarUploader {
    private var nextFailure: Throwable? = null

    /** Every `(uid, source)` passed to [upload] so far, for assertions. */
    val uploads = mutableListOf<Pair<String, Uri>>()

    fun failNextCallWith(error: Throwable) {
        nextFailure = error
    }

    override suspend fun upload(uid: String, source: Uri): Result<String> {
        val failure = nextFailure
        if (failure != null) {
            nextFailure = null
            return Result.failure(failure)
        }
        uploads += uid to source
        return Result.success("https://fake-storage.example/avatars/$uid.jpg")
    }
}
