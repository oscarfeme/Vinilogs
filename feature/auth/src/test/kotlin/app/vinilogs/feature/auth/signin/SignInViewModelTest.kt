package app.vinilogs.feature.auth.signin

import app.cash.turbine.test
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
class SignInViewModelTest {
    companion object {
        // Unconfined, not the extension's default StandardTestDispatcher: viewModelScope
        // launches need to run eagerly on this test's own coroutine so `runTest` observes
        // their effects without a separate, uncoordinated scheduler to advance.
        @JvmField
        @RegisterExtension
        val mainDispatcherExtension = MainDispatcherExtension(UnconfinedTestDispatcher())
    }

    @Test
    fun `initial state is empty and not loading`() {
        val viewModel = SignInViewModel(FakeAuthRepository())

        val state = viewModel.uiState.value

        assertTrue(state.email.isEmpty())
        assertTrue(state.password.isEmpty())
        assertFalse(state.isLoading)
        assertNull(state.generalError)
    }

    @Test
    fun `submitting blank fields sets field errors without calling the repository`() = runTest {
        val viewModel = SignInViewModel(FakeAuthRepository())

        viewModel.onSignInClick()

        val state = viewModel.uiState.value
        assertNotNull(state.emailError)
        assertNotNull(state.passwordError)
        assertFalse(state.isLoading)
    }

    @Test
    fun `an invalid email shape is rejected client-side`() = runTest {
        val viewModel = SignInViewModel(FakeAuthRepository())
        viewModel.onEmailChange("not-an-email")
        viewModel.onPasswordChange("password123")

        viewModel.onSignInClick()

        assertNotNull(viewModel.uiState.value.emailError)
    }

    @Test
    fun `typing into a field clears that field's previous error`() = runTest {
        val viewModel = SignInViewModel(FakeAuthRepository())
        viewModel.onSignInClick() // populates emailError/passwordError

        viewModel.onEmailChange("user@example.com")

        assertNull(viewModel.uiState.value.emailError)
    }

    @Test
    fun `successful sign in clears loading and emits signInSucceeded`() = runTest {
        val authRepository = FakeAuthRepository()
        authRepository.signUp("user@example.com", "password123", "User")
        authRepository.signOut()
        val viewModel = SignInViewModel(authRepository)
        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("password123")

        viewModel.signInSucceeded.test {
            viewModel.onSignInClick()
            awaitItem()
        }

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.generalError)
    }

    @Test
    fun `a repository failure surfaces as a general error, not a crash`() = runTest {
        val authRepository = FakeAuthRepository()
        authRepository.signUp("user@example.com", "correct-password", "User")
        authRepository.signOut()
        val viewModel = SignInViewModel(authRepository)
        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("wrong-password")

        viewModel.onSignInClick()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.generalError)
    }
}
