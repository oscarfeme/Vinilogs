package app.vinilogs.core.data.repository

import android.net.Uri

/**
 * Uploads a locally-picked avatar image to Firebase Storage and returns its public download
 * URL. Not one of the three fixed repository interfaces in 02-ARCHITECTURE.md §4 -- `Uri`
 * upload has no place in [UserRepository]'s contract ([UserRepository.updateProfile] takes an
 * already-resolved `avatarUrl: String?`, matching [CollectionRepository.setCoverImage]'s
 * separation of "upload bytes" from "save metadata" for record covers). A small,
 * deliberately-scoped addition for T-19 (FR-A4's avatar image field) rather than scope creep
 * on the fixed contracts -- see the T-19 PR description for the reasoning.
 */
interface AvatarUploader {
    suspend fun upload(uid: String, source: Uri): Result<String>
}
