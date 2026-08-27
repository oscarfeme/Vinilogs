package app.vinilogs.feature.collection.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import app.vinilogs.core.designsystem.theme.spacing
import app.vinilogs.core.designsystem.theme.vinilogsColors

/**
 * A text field per 05-DESIGN-DIRECTION.md §5: "no filled container. 1dp Ink400 bottom rule that
 * becomes 2dp Ink900 on focus and 2dp Alert on error. Label above in labelMedium uppercase."
 *
 * Gap note: this belongs in `core:designsystem` alongside the rest of the type/space/component
 * tokens it's built from, but that module is outside Track D's boundary (CLAUDE.md rule 2).
 * Defined locally here (T-15) and reused as-is by T-17's manual-entry form; flagged in both
 * PRs as a candidate to hoist once another track needs the same field.
 */
@Composable
fun CollectionTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    isError: Boolean = false,
    supportingText: String? = null,
    singleLine: Boolean = true,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
) {
    var isFocused by remember { mutableStateOf(false) }
    val ruleColor =
        when {
            isError -> MaterialTheme.colorScheme.error
            isFocused -> MaterialTheme.colorScheme.onSurface
            else -> MaterialTheme.vinilogsColors.controlOutline
        }
    val ruleThickness =
        if (isError || isFocused) MaterialTheme.spacing.hairlineEmphasis else MaterialTheme.spacing.hairline

    Column(modifier = modifier) {
        if (label != null) TextFieldLabel(label)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isFocused = it.isFocused }
                    .padding(vertical = MaterialTheme.spacing.sm),
            textStyle =
                LocalTextStyle.current.copy(
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
            singleLine = singleLine,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
            keyboardOptions =
                KeyboardOptions(capitalization = capitalization, keyboardType = keyboardType, imeAction = imeAction),
            decorationBox = { innerTextField ->
                if (value.isEmpty() && placeholder != null) TextFieldPlaceholder(placeholder)
                innerTextField()
            },
        )
        HorizontalDivider(thickness = ruleThickness, color = ruleColor)
        if (supportingText != null) TextFieldSupportingText(supportingText, isError)
    }
}

@Composable
private fun TextFieldLabel(label: String) {
    Text(
        text = label.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.vinilogsColors.textTertiary,
        modifier = Modifier.padding(bottom = MaterialTheme.spacing.xxs),
    )
}

@Composable
private fun TextFieldPlaceholder(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.vinilogsColors.textTertiary)
}

@Composable
private fun TextFieldSupportingText(
    text: String,
    isError: Boolean,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.vinilogsColors.textTertiary,
        modifier = Modifier.padding(top = MaterialTheme.spacing.xxs),
    )
}
