package app.vinilogs.feature.auth.component

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.VisualTransformation
import app.vinilogs.core.designsystem.theme.spacing
import app.vinilogs.core.designsystem.theme.vinilogsColors

/**
 * Underline-style text field per 05-DESIGN-DIRECTION.md §5 ("no filled container.
 * 1dp Ink400 bottom rule that becomes 2dp Ink900 on focus and 2dp Alert on
 * error. Label above in labelMedium uppercase"). Built locally in `feature:auth`
 * because `core:designsystem` (T-02) has no shared form text field yet -- a
 * candidate to promote there once a second form (e.g. add/edit record, T-17)
 * needs the same treatment.
 *
 * Reuses Material 3's filled [TextField] rather than a hand-rolled
 * `BasicTextField`: with its container made transparent, its bottom indicator
 * is already exactly the 1dp/2dp rule the spec calls for, and it comes with
 * working IME/accessibility semantics for free. The one accepted deviation
 * from the spec is that the label floats down into the field when empty and
 * unfocused rather than sitting statically above at all times -- standard
 * Material behaviour, not custom here.
 */
@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    singleLine: Boolean = true,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    val isError = errorMessage != null
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = MaterialTheme.spacing.minTouchTarget),
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        supportingText = if (errorMessage != null) {
            { Text(errorMessage, style = MaterialTheme.typography.bodySmall) }
        } else {
            null
        },
        isError = isError,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            errorContainerColor = Color.Transparent,
            focusedIndicatorColor = MaterialTheme.colorScheme.onSurface,
            unfocusedIndicatorColor = MaterialTheme.vinilogsColors.controlOutline,
            errorIndicatorColor = MaterialTheme.colorScheme.error,
            focusedLabelColor = MaterialTheme.colorScheme.onSurface,
            unfocusedLabelColor = MaterialTheme.vinilogsColors.textSecondary,
            errorLabelColor = MaterialTheme.colorScheme.error,
            cursorColor = MaterialTheme.colorScheme.onSurface,
            errorSupportingTextColor = MaterialTheme.colorScheme.error,
        ),
    )
}
