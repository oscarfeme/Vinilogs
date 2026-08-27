package app.vinilogs.feature.auth.forgotpassword

import app.vinilogs.core.testing.MainDispatcherExtension
import app.vinilogs.core.testing.fake.FakeAuthRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class ForgotPasswordViewModelTest {
    companion object {
        @JvmField
        @RegisterExtension
        val mainDispatcherExtension = MainDispatcherExtension(UnconfinedTestDispatcher())
    }

    @Test
    fun `a blank email is rejected without calling the repository`() = runTest {
        val viewModel = ForgotPasswordViewModel(FakeAuthRepository())

        viewModel.onSendResetClick()

        val state = viewModel.uiState.value
        assertNotNull(state.emailError)
        assertFalse(state.isSubmitted)
    }

    @Test
    fun `a registered email shows the generic confirmation -- FR-A3`() = runTest {
        val authRepository = FakeAuthRepository()
        authRepository.signUp("user@example.com", "password123", "User")
        authRepository.signOut()
        val viewModel = ForgotPasswordViewModel(authRepository)
        viewModel.onEmailChange("user@example.com")

        viewModel.onSendResetClick()

        val state = viewModel.uiState.value
        assertTrue(state.isSubmitted)
        assertFalse(state.isLoading)
    }

    @Test
    fun `an unregistered email shows the exact same generic confirmation -- FR-A3`() = runTest {
        // FakeAuthRepository.sendPasswordReset always succeeds regardless of whether an
        // account exists for the address -- matching real Firebase Auth's behaviour and
        // FR-A3's "generic confirmation shown regardless of whether the address exists".
        val viewModel = ForgotPasswordViewModel(FakeAuthRepository())
        viewModel.onEmailChange("nobody@example.com")

        viewModel.onSendResetClick()

        val state = viewModel.uiState.value
        assertTrue(state.isSubmitted)
        assertNull(state.generalError)
    }

    @Test
    fun `editing the email after submission resets the confirmation`() = runTest {
        val viewModel = ForgotPasswordViewModel(FakeAuthRepository())
        viewModel.onEmailChange("user@example.com")
        viewModel.onSendResetClick()

        viewModel.onEmailChange("user2@example.com")

        assertFalse(viewModel.uiState.value.isSubmitted)
    }
}
