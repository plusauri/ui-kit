package plusauri.ui

/**
 * An option that can appear in a [MultiColumnSelect] menu.
 *
 * - [Label]: the same string is shown in both the menu and the trigger button.
 * - [KeyLabel]: a short [key] (mnemonic / flag / direction glyph) and a [label] side-by-side in
 *   the menu cell. By default the closed trigger shows only [key] (currency symbol, …). Set
 *   [showKeyAndLabelOnTrigger] when the closed control should read like the open cell
 *   (e.g. planning region: `🇩🇪 Germany`).
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
