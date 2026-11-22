package plusauri.ui

/**
 * An option of a select menu whose closed trigger may show less than the open menu row. Type-to-
 * select over these options is [TypeAheadMatcher.onCharOption].
 *
 * - [Label]: the same string is shown in both the menu and the trigger button.
 * - [KeyLabel]: a short [key] (mnemonic / flag / direction glyph) and a [label] side-by-side in
 *   the menu cell. By default the closed trigger shows only [key] (a currency symbol, say). Set
 *   [showKeyAndLabelOnTrigger] when the closed control should read like the open cell
 *   (e.g. a country: `🇩🇪 Germany`).
 */
sealed class SelectOption {
    data class Label(val label: String) : SelectOption()
    data class KeyLabel(
        val key: String,
        val label: String,
        val showKeyAndLabelOnTrigger: Boolean = false,
    ) : SelectOption()

    val mainButtonLabel: String
        get() = when (this) {
            is Label -> label
            is KeyLabel -> if (showKeyAndLabelOnTrigger) "$key $label" else key
        }
}
