package app.vinilogs.feature.auth.signup

import app.cash.turbine.test
import app.vinilogs.core.testing.MainDispatcherExtension
import app.vinilogs.core.testing.fake.FakeAuthRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class SignUpViewModelTest {
    companion object {
        @JvmField
        @RegisterExtension
        val mainDispatcherExtension = MainDispatcherExtension(UnconfinedTestDispatcher())
    }

    @Test
    fun `submitting blank fields sets every field error without calling the repository`() = runTest {
        val viewModel = SignUpViewModel(FakeAuthRepository())

        viewModel.onSignUpClick()

        val state = viewModel.uiState.value
        assertNotNull(state.displayNameError)
        assertNotNull(state.emailError)
        assertNotNull(state.passwordError)
        assertFalse(state.isLoading)
    }

    @Test
    fun `a password under 8 characters is rejected -- FR-A1`() = runTest {
        val viewModel = SignUpViewModel(FakeAuthRepository())
        viewModel.onDisplayNameChange("New User")
        viewModel.onEmailChange("new@example.com")
        viewModel.onPasswordChange("short1")

        viewModel.onSignUpClick()

        assertNotNull(viewModel.uiState.value.passwordError)
    }

    @Test
    fun `an 8-character password passes validation`() = runTest {
        val authRepository = FakeAuthRepository()
        val viewModel = SignUpViewModel(authRepository)
        viewModel.onDisplayNameChange("New User")
        viewModel.onEmailChange("new@example.com")
        viewModel.onPasswordChange("exactly8")

        viewModel.signUpSucceeded.test {
            viewModel.onSignUpClick()
            awaitItem()
        }

        assertNull(viewModel.uiState.value.passwordError)
    }

    @Test
    fun `successful sign up clears loading and emits signUpSucceeded`() = runTest {
        val authRepository = FakeAuthRepository()
        val viewModel = SignUpViewModel(authRepository)
        viewModel.onDisplayNameChange("New User")
        viewModel.onEmailChange("new@example.com")
        viewModel.onPasswordChange("password123")

        viewModel.signUpSucceeded.test {
            viewModel.onSignUpClick()
            awaitItem()
        }

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.generalError)
    }

    @Test
    fun `a duplicate email surfaces as an inline general error -- FR-A1`() = runTest {
        val authRepository = FakeAuthRepository()
        authRepository.signUp("taken@example.com", "password123", "First")
        authRepository.signOut()
        val viewModel = SignUpViewModel(authRepository)
        viewModel.onDisplayNameChange("Second User")
        viewModel.onEmailChange("taken@example.com")
        viewModel.onPasswordChange("password123")

        viewModel.onSignUpClick()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.generalError)
    }
}
