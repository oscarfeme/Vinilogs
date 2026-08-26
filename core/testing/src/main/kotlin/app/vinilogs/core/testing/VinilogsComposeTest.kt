package app.vinilogs.core.testing

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import app.vinilogs.core.designsystem.theme.VinilogsTheme

/** Compose UI test rule for screen/component tests -- use with `@get:Rule`. */
fun createVinilogsComposeRule(): ComposeContentTestRule = createComposeRule()

/** Sets [content] inside [VinilogsTheme], matching how the real app renders every screen. */
fun ComposeContentTestRule.setVinilogsContent(content: @Composable () -> Unit) {
    setContent {
        VinilogsTheme {
            content()
        }
    }
}
