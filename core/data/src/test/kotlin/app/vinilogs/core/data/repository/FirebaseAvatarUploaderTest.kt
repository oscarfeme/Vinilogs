package app.vinilogs.core.data.repository

import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FirebaseAvatarUploaderTest {
    private val storage = mockk<FirebaseStorage>()
    private val uploader = FirebaseAvatarUploader(storage)

    @Test
    fun `upload writes to avatars slash uid dot jpg and returns the download URL`() =
        runTest {
            val sourceUri = mockk<Uri>()
            val avatarRef = mockk<StorageReference>()
            val rootRef = mockk<StorageReference> { every { child("avatars/uid-1.jpg") } returns avatarRef }
            every { storage.reference } returns rootRef
            every { avatarRef.putFile(sourceUri) } returns successfulUploadTask()
            // `toString()` is inherited from Any, so unlike a type's own members it's ambiguous
            // as a bare call inside this block -- `this@mockk` disambiguates it to the Uri mock,
            // same as FirebaseAuthRepositoryTest's firebaseUser() helper.
            val downloadUri = mockk<Uri> {
                every { this@mockk.toString() } returns "https://example.com/avatars/uid-1.jpg"
            }
            every { avatarRef.downloadUrl } returns successfulTask(downloadUri)

            val result = uploader.upload("uid-1", sourceUri)

            assertTrue(result.isSuccess)
            assertEquals("https://example.com/avatars/uid-1.jpg", result.getOrNull())
        }

    @Test
    fun `upload wraps a Storage failure as Result failure instead of throwing`() =
        runTest {
            val sourceUri = mockk<Uri>()
            val avatarRef = mockk<StorageReference>()
            val rootRef = mockk<StorageReference> { every { child("avatars/uid-1.jpg") } returns avatarRef }
            every { storage.reference } returns rootRef
            val exception = RuntimeException("permission denied")
            every { avatarRef.putFile(sourceUri) } returns failedUploadTask(exception)

            val result = uploader.upload("uid-1", sourceUri)

            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
        }
}

private fun successfulUploadTask(): UploadTask =
    mockk {
        every { isComplete } returns true
        every { isCanceled } returns false
        every { exception } returns null
        every { result } returns mockk()
    }

private fun failedUploadTask(exception: Exception): UploadTask =
    mockk {
        every { isComplete } returns true
        every { isCanceled } returns false
        every { this@mockk.exception } returns exception
    }

private fun <T> successfulTask(value: T): Task<T> =
    mockk {
        every { isComplete } returns true
        every { isCanceled } returns false
        every { exception } returns null
        every { result } returns value
    }
