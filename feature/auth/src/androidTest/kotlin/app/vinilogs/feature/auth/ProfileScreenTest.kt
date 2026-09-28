package app.vinilogs.feature.auth

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.vinilogs.core.model.UserProfile
import app.vinilogs.core.testing.createVinilogsComposeRule
import app.vinilogs.core.testing.setVinilogsContent
import app.vinilogs.feature.auth.profile.ProfileUiState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class ProfileScreenTest {
    @get:Rule
    val composeRule = createVinilogsComposeRule()

    private val profile = UserProfile(
        uid = "uid-1",
        displayName = "Alice",
        avatarUrl = null,
        bio = "Vinyl collector",
        location = "London",
        isPublic = true,
        recordCount = 42,
        createdAt = Instant.EPOCH,
    )

    @Test
    fun loading_showsNoProfileContent() {
        composeRule.setVinilogsContent {
            ProfileScreenContent(
                uiState = ProfileUiState(profile = null, isLoading = true),
                onNavigateToEditProfile = {},
                onNavigateToSettings = {},
            )
        }

        composeRule.onAllNodesWithTag("profileDisplayName").assertCountEquals(0)
    }

    @Test
    fun loadedProfile_showsDisplayNameBioAndLocation() {
        composeRule.setVinilogsContent {
            ProfileScreenContent(
                uiState = ProfileUiState(profile = profile, isLoading = false),
                onNavigateToEditProfile = {},
                onNavigateToSettings = {},
            )
        }

        composeRule.onNodeWithTag("profileDisplayName").assertIsDisplayed()
        composeRule.onNodeWithTag("profileBio").assertIsDisplayed()
        composeRule.onNodeWithText("42 records").assertIsDisplayed()
        composeRule.onNodeWithTag("profilePrivacyLabel").assertIsDisplayed()
    }

    @Test
    fun tappingEditButton_invokesOnNavigateToEditProfile() {
        var navigated = false
        composeRule.setVinilogsContent {
            ProfileScreenContent(
                uiState = ProfileUiState(profile = profile, isLoading = false),
                onNavigateToEditProfile = { navigated = true },
                onNavigateToSettings = {},
            )
        }

        composeRule.onNodeWithTag("profileEditButton").performClick()

        assertTrue(navigated)
    }

    @Test
    fun tappingSettingsButton_invokesOnNavigateToSettings() {
        var navigated = false
        composeRule.setVinilogsContent {
            ProfileScreenContent(
                uiState = ProfileUiState(profile = profile, isLoading = false),
                onNavigateToEditProfile = {},
                onNavigateToSettings = { navigated = true },
            )
        }

        composeRule.onNodeWithTag("profileSettingsButton").performClick()

        assertTrue(navigated)
    }

    @Test
    fun privateProfile_showsPrivateLabel() {
        composeRule.setVinilogsContent {
            ProfileScreenContent(
                uiState = ProfileUiState(profile = profile.copy(isPublic = false), isLoading = false),
                onNavigateToEditProfile = {},
                onNavigateToSettings = {},
            )
        }

        composeRule.onNodeWithText("Private").assertIsDisplayed()
    }
}
