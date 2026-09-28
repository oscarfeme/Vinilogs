package app.vinilogs.core.data.repository

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * [AvatarUploader] backed by Firebase Storage. Avatars live at `avatars/{uid}.jpg` -- one file
 * per user, so a re-upload simply overwrites the previous avatar rather than accumulating
 * orphaned blobs (`storage.rules`, T-21, is expected to allow a signed-in user to write only
 * their own `avatars/{uid}.jpg` path).
 */
class FirebaseAvatarUploader
    @Inject
    constructor(
        private val storage: FirebaseStorage,
    ) : AvatarUploader {
        override suspend fun upload(uid: String, source: Uri): Result<String> =
            runCatching {
                val avatarRef = storage.reference.child("$AVATARS_PATH/$uid.jpg")
                avatarRef.putFile(source).await()
                avatarRef.downloadUrl.await().toString()
            }

        private companion object {
            const val AVATARS_PATH = "avatars"
        }
    }
