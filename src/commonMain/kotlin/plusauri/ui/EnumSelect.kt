package plusauri.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import plusauri.ui.NumericInput
import plusauri.ui.TypeAheadMatcher
import plusauri.ui.fieldOption
import plusauri.ui.fieldTag
import plusauri.ui.listKey
import plusauri.ui.typeAheadChar
import plusauri.ui.typeAheadText

/**
 * A dropdown select for a fixed list of options.
 *
 * Displays the currently selected option in an outlined field and shows a compact
 * [CompactDropdownMenu] of [options] on click. The open menu takes the keyboard: Up/Down move
 * between rows, Enter commits, Escape closes, and typing a letter is type-to-select
 * ([TypeAheadMatcher]), so every menu built on that matcher answers to letters the same way. On
 * the focused, closed field a typed letter opens the menu already on the match.
 *
 * @param options       Full list of selectable values.
 * @param value         Currently selected value.
 * @param onValueChange Called when the user selects a different option.
 * @param display       Converts an option to its display string; defaults to [toString].
 * @param displaySelected Display string for the **closed field** when it must be terser than a
 *                      menu row; defaults to [display]. A menu row has room for an annotation
 *                      ("Final review · due Mar 3"), while a narrow closed field would truncate
 *                      that annotation away along with half the name.
 * @param label         Optional floating label shown on the border (same convention as
 *                      [NumericInput]). Prefer null inside a [LabeledField].
 * @param enabled       When false the dropdown is non-interactive.
 * @param optionTag     A [fieldTag] tag for an option's menu row, for UI tests that pick by
 *                      identity rather than by caption — see [fieldOption]. Null leaves the row
 *                      untagged.
 * @param modifier      Applied to the outer [Box].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> EnumSelect(
    options: List<T>,
    value: T,
    onValueChange: (T) -> Unit,
    display: (T) -> String = { it.toString() },
    displaySelected: ((T) -> String)? = null,
    label: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    optionTag: (T) -> String? = { null },
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedText = (displaySelected ?: display)(value)
    val selectedIndex = options.indexOf(value).takeIf { it >= 0 }
    val texts = remember(options, display) { options.map { typeAheadText(display(it)) } }
    val focusRequesters = remember(options.size) { List(options.size) { FocusRequester() } }
    val typeAhead = remember { TypeAheadMatcher() }
    // The option a keystroke on the closed field found, focused once the menu opens.
    var pendingFocus by remember { mutableStateOf<Int?>(null) }
    // The row keyboard focus is on, drawn with an explicit ring: a menu row's own focus
    // indication is too faint to follow while arrowing through the list.
    var focusedIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(expanded) {
        if (!expanded) typeAhead.reset()
    }

    // Outlined field is a button-like target; override Material's 48 dp floor so the field
    // height matches NumericInput and other compact text fields at the same padding.
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        Box(
            modifier = modifier.onPreviewKeyEvent { event ->
                if (expanded || !enabled) return@onPreviewKeyEvent false
                val ch = event.typeAheadChar() ?: return@onPreviewKeyEvent false
                pendingFocus = typeAhead.onChar(ch, texts, selectedIndex)
                expanded = true
                true
            },
        ) {
            val interactionSource = remember { MutableInteractionSource() }
            BasicTextField(
                value = selectedText,
                onValueChange = {},
                readOnly = true,
                enabled = enabled,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                ),
                interactionSource = interactionSource,
                decorationBox = { innerTextField ->
                    OutlinedTextFieldDefaults.DecorationBox(
                        value = selectedText,
                        innerTextField = innerTextField,
                        enabled = enabled,
                        singleLine = true,
                        visualTransformation = VisualTransformation.None,
                        interactionSource = interactionSource,
                        label = label,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && enabled) },
                        contentPadding = OutlinedTextFieldDefaults.contentPadding(
                            start = 8.dp, top = 3.dp, end = 8.dp, bottom = 3.dp
                        ),
                    )
                },
            )
            // The outlined chrome and chevron sit outside the inner text field, so a clickable
            // on BasicTextField itself misses almost every click. Cover the whole control.
            Box(
                Modifier
                    .matchParentSize()
                    .clickable(
                        enabled = enabled,
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { expanded = !expanded },
                    ),
            )
            CompactDropdownMenu(
                expanded = expanded && enabled,
                onDismissRequest = { expanded = false },
                offset = CompactMenuBelowOffset,
                placement = CompactMenuPlacement.BelowAnchor,
            ) {
                // From inside the popup, once its rows exist: requesting focus before the popup
                // composes finds no row to focus.
                LaunchedEffect(Unit) {
                    if (options.isNotEmpty()) focusRequesters[pendingFocus ?: selectedIndex ?: 0].requestFocus()
                    pendingFocus = null
                }
                options.forEachIndexed { i, option ->
                    CompactDropdownMenuItem(
                        text = display(option),
                        selected = option == value,
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        },
                        modifier = Modifier
                            .then(
                                if (i == focusedIndex) Modifier.border(1.dp, MaterialTheme.colorScheme.primary)
                                else Modifier
                            )
                            .then(optionTag(option)?.let { Modifier.fieldTag(it) } ?: Modifier)
                            .focusRequester(focusRequesters[i])
                            .onFocusChanged { if (it.isFocused) focusedIndex = i }
                            .onPreviewKeyEvent { event ->
                                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                when (event.listKey) {
                                    Key.DirectionUp -> { focusRequesters[(i - 1).mod(options.size)].requestFocus(); true }
                                    Key.DirectionDown -> { focusRequesters[(i + 1).mod(options.size)].requestFocus(); true }
                                    Key.Escape -> { expanded = false; true }
                                    // Enter/Space fall through to the row's click.
                                    else -> event.typeAheadChar()?.let { ch ->
                                        typeAhead.onChar(ch, texts, i)?.let { focusRequesters[it].requestFocus() }
                                        true
                                    } ?: false
                                }
                            },
                    )
                }
            }
        }
    }
}
