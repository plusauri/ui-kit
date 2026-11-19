package plusauri.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.input.pointer.isAltPressed
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Spacer

/*
 * ── House rule: this file is the *only* pick-one control ──────────────────────
 *
 * Material's `SingleChoiceSegmentedButtonRow` / `SegmentedButton` are **not used in this product**,
 * and the two thin wrappers over them that used to live here were deleted on 2026-08-30 with zero
 * call sites left. Do not add them back and do not import them at a call site: a segmented row is a
 * second toggle vocabulary, and the app already made this move twice (the Brackets headroom rail,
 * the saved-plan view modes) before the rule was written anywhere.
 *
 * The house pick-one control is [MultiButtonSelect] in its `buttonSelections` form — [ButtonData] /
 * [ToggleState] over [ToggleImageButton] — driven controlled-single via `selectedIndex` +
 * `onSelectedIndexChange`, with `toolbarOnly = true`, `widthMode = ToggleButtonWidth.Fit` and
 * `buttonSize = MultiButtonSelectDefaults.CompactHeight` for a form or toolbar row.
 *
 * See `planning/design-notes/dossier-entry-control-conventions.md`.
 */

/** Which side of the panel area the button toolbar sits on. */
enum class ViewSide { Top, Right, Bottom, Left }

/**
 * Per-slot color for a [MultiButtonSelect] toggle — one for each state.
 *
 * Two states rather than one because a slot's color can be answering either of two different
 * questions, and they want different chrome:
 *
 * - **"Which one is open?"** — a legend row keying chart series. Only the *selected* slot needs its
 *   color; an unselected series is not being drawn, so tinting its toggle would claim otherwise.
 *   Pass [selected] alone (the common case, and what [MbsSlotAccent] with no [deselected] gives).
 * - **"What is this slot?"** — the dossier metadata tabs. Errors is red whether or not you are
 *   looking at it, and a reader glancing at the panel should see the severity ladder without first
 *   clicking through it. Pass both.
 *
 * [deselected] is rendered dimmer than [selected] (see `ToggleImageButton`), because once every
 * slot carries a color, saturation is no longer available to say which one is open — and if both
 * states are equally strong the selection indicator ends up doing that job alone.
 *
 * Color is never the *only* channel: the toggles carry their labels, and the selected slot keeps
 * its underbar. That is what makes the ladder survive a reader who cannot separate the hues.
 */
data class MbsSlotAccent(val selected: Color, val deselected: Color? = null)

/**
 * Optional bulk affordance beside a multi-toggle row.
 *
 * Lives in the control rather than in each host because **[MultiButtonSelect]'s `onToggle` cannot
 * express it**: `onToggle(index)` is a *flip*, so calling it for every index inverts the selection
 * instead of setting it. A host wanting "select all" would have to read its own mask and flip only
 * the off entries — per-call-site logic that then drifts (the cliff radar grew a bespoke "Show all"
 * chip with no "clear" counterpart; the Forecaster chips had neither).
 *
 * See [AllAndClear] for why both actions are shown at once rather than one at a time.
 */
enum class MbsBulkControl {
    /** No bulk affordance (default — existing call sites are unchanged). */
    None,

    /**
     * **Both** actions, stacked, each disabled when it would do nothing.
     *
     * Replaced an earlier single tri-state control that showed "all" while anything was off and
     * "clear" once everything was on. That was wrong: it assumed exactly one action is available at
     * a time, which only holds at the two extremes. With 7 of 8 selected both are meaningful, and
     * the tri-state arbitrarily picked one — so "clear, then pick one" had to route *through* "all",
     * which does the opposite of what you want and is not discoverable as a stepping stone.
     *
     * Disabling rather than hiding the inapplicable one makes the pair a state read as well as a
     * control: greyed "all" means everything is already on, greyed "clear" means nothing is.
     * Stacking keeps the cost horizontal — one narrow column — rather than widening the row.
     */
    AllAndClear,
}

/** Labels for [MbsBulkControl.AllAndClear]. Passed in so the control itself stays locale-free. */
data class MbsBulkLabels(
    /** Select every entry; disabled once they all are. */
    val selectAll: String,
    /** Deselect every entry; disabled once none are. */
    val clear: String,
)

/**
 * Whether a **controlled-multi** toolbar may flip [i], given its current [mask] and a
 * [minSelected] floor.
 *
 * Pure so the rule can be asserted without a composition. A floor of 0 (the default) always
 * permits; otherwise the last [minSelected] selected buttons refuse to come off.
 */
internal fun mbsMayToggle(mask: List<Boolean>, i: Int, minSelected: Int): Boolean =
    minSelected <= 0 || !mask[i] || mask.count { it } > minSelected

/** Constrains how many content panels may be visible simultaneously. */
enum class MbsGroupingConstraint {
    /** All panels can be visible at once. */
    NoConstraint,
    /** Exactly one panel is always visible; deselecting index i re-selects index 0. */
    ExactlyOne,
    /** At most one panel is visible; deselecting hides all. */
    OneOrNone,
}

/**
 * How the content panel changes when the selection moves, in **controlled-single** mode
 * (`selectedIndex` + `onSelectedIndexChange`). Ignored in the other modes, where more than one
 * panel can be showing and there is no single "from → to" to animate.
 */
enum class MbsPanelTransition {
    /** The new panel replaces the old one on the next frame (default — every existing call site). */
    None,

    /**
     * The panels slide as a strip laid out in toolbar order: picking a toggle to the **right** of
     * the current one pushes the current panel off to the left and brings the new one in from the
     * right, and the reverse for a toggle to the left. The container height animates with the
     * content so a taller panel does not pop. Built for [SwitchedCollectionEditor], whose toggles
     * *are* the panels' names, so the motion says where the reader went.
     */
    SlideHorizontal,
}

/**
 * A multi-panel selector driven by a toolbar of [ToggleImageButton]s.
 *
 * The toolbar appears on the specified [buttonBarSide] — top/bottom produce a horizontal
 * row of buttons; left/right produce a vertical column.  Each button toggles the
 * visibility of the corresponding content panel produced by [contentMaker].
 *
 * Selection modes:
 * - **Uncontrolled** (default): state owned here from [buttonSelections] initials.
 * - **Controlled ExactlyOne**: pass [selectedIndex] + [onSelectedIndexChange] (e.g. Risk view).
 * - **Controlled multi-toggle**: pass [selectedMask] + [onToggle] — each index independently
 *   on/off (e.g. Forecast Detail visibility chips). Takes precedence over [selectedIndex].
 *
 * Pastel subsection toolbars (Assumptions, Risk): [equalSlots] = true,
 * [buttonSize] = [SubsectionToolbar.ButtonSize], icons via [subsectionToolbarIcon].
 *
 * @param buttonSelections  (ButtonData, initial ToggleState) for each button/panel.
 * @param contentMaker      Composable content for panel `i`. Ignored when [toolbarOnly].
 * @param buttonBarSide     Which side the toolbar sits on.
 * @param groupingConstraint Constraint on simultaneous visible panels (uncontrolled mode).
 * @param equalSlots        When true, each button gets the **same width**
 *                          (the widest natural width) so **icon centers stay evenly spaced** even
 *                          if labels differ in length. The row stays **left-aligned** and only as
 *                          wide as those slots — it does **not** stretch to the parent. If the
 *                          parent is narrower than the natural row, slots shrink together to fit.
 *
 *                          **Not usable inside a [androidx.compose.material3.DropdownMenu]** (or any
 *                          ancestor that measures with `IntrinsicSize`): this is implemented with
 *                          [EqualWidthToggleRow], a `SubcomposeLayout`, and a DropdownMenu sizes its
 *                          popup by wrapping the content in `width(IntrinsicSize.Max)`. A
 *                          SubcomposeLayout cannot answer an intrinsic query, so the menu throws
 *                          `Asking for intrinsic measurements of SubcomposeLayout layouts is not
 *                          supported` the moment it opens. Leave this false there — plain
 *                          [ToggleButtonWidth.Fit] toggles measure fine.
 *                          A toolbar on the left or right stacks its buttons as an aligned list, each face
 *                          (selection mark and label) starting at the same edge.
 * @param buttonSize        Side length of each [ToggleImageButton] surface (default 48.dp;
 *                          use [SubsectionToolbar.ButtonSize] for pastel subsection toolbars).
 * @param selectedIndex     Controlled ExactlyOne index; null = uncontrolled (unless [selectedMask]).
 * @param onSelectedIndexChange  Required when [selectedIndex] is non-null.
 * @param selectedMask      Controlled multi-toggle: `true` = Selected at that index. Same length as
 *                          [buttonSelections]. When non-null, [onToggle] is required.
 * @param onToggle          Flip handler for controlled multi-toggle (index that was clicked).
 * @param toolbarOnly       When true, render only the toggle row/column — no content panels and no
 *                          weight-fill box. Use when the toolbar sits inline (e.g. beside FID·Year).
 * @param fillPanelHeight   Whether the content panel claims the remaining main-axis space.
 *
 *                          True (default) is right inside a **bounded** pane — a Risk chart or the
 *                          Assumptions scroller, where "the rest of the height" is a real quantity
 *                          and the panel should fill it.
 *
 *                          **Pass false when the parent's height is unbounded**, which is what a
 *                          vertically scrolling column offers its children. A weighted child in an
 *                          unbounded main axis is allotted `mainAxisMin` — zero — so the panel
 *                          measures to nothing and the control renders as a tab strip above an
 *                          empty space. There is no way to detect that from inside here without a
 *                          `BoxWithConstraints`, which is a `SubcomposeLayout` and would break every
 *                          caller that needs intrinsics; so it is the caller's to declare.
 * @param slotSpacing       Gap between buttons; null uses [SubsectionToolbar.SlotSpacing] when
 *                          [equalSlots], else 4.dp.
 * @param tooltips          Optional per-button hover tip. Same length as [buttonSelections] when
 *                          non-null. Null falls back to each button's [ButtonData.hoverName], so an
 *                          icon-only button names itself on hover without the caller listing it.
 * @param enabledMask       Optional per-index interactivity. A `false` entry renders the toggle
 *                          greyed and unclickable — for an option that exists but can do nothing
 *                          right now. Shown-and-disabled rather than hidden so the toolbar keeps
 *                          one shape and the hover tip can say *why*. Same length as
 *                          [buttonSelections] when non-null.
 * @param widthMode         [ToggleButtonWidth.Square] (default) or [ToggleButtonWidth.Fit] for
 *                          text-width toggles.
 * @param labelTextStyle    Style for [ButtonImage.Unicode] content; pass a label-sized style with
 *                          [ToggleButtonWidth.Fit].
 * @param bulkControl       Optional select-all / clear affordance. Requires
 *                          [MbsGroupingConstraint.NoConstraint] — the constrained modes forbid
 *                          "none" (ExactlyOne) or "all" (ExactlyOne / OneOrNone), so offering it
 *                          there would render an action the control must refuse.
 * @param bulkLabels        Required when [bulkControl] is not [MbsBulkControl.None].
 * @param onSetAll          Bulk **set** (not flip) handler, required alongside [selectedMask].
 * @param slotAccents       Optional per-index color for the toggle (same length as
 *                          [buttonSelections] when non-null; a null entry keeps theme chrome for
 *                          that slot). See [MbsSlotAccent] for which of its two states to fill in.
 * @param selectionMark     Optional leading checkbox on every toggle, ticked on the selected one —
 *                          for a **two-option** row, where color alone does not say which side is
 *                          on. Fixed at creation so the row never reflows; see [SelectionMark].
 *                          Requires [ToggleButtonWidth.Fit].
 * @param wrap              When true (horizontal toolbars only), toggles reflow with [FlowRow]
 *                          instead of a single row. Incompatible with [equalSlots] (equal slots
 *                          measure a fixed row of same-width children). Bulk actions trail the
 *                          last wrap item.
 * @param toolbarAlignment  Where a horizontal (Top / Bottom) toolbar sits across the control's
 *                          width — Start by default, as every toolbar has been; Center for a row
 *                          that names two halves of what is below it (the balance-sheet Assets /
 *                          Liabilities switch), where a left-pinned pair reads as a menu rather
 *                          than a divide. Ignored by [toolbarOnly] and the vertical sides.
 * @param panelTransition   How the panel slot moves between selections in controlled-single mode;
 *                          see [MbsPanelTransition]. Ignored when [toolbarOnly] or in the multi
 *                          modes.
 * @param modifier          Applied to the outermost layout container.
 *
 * **Keyboard.** A focused button toggles on Enter or Space, as any button does. The arrow keys along
 * the toolbar (← → for a row, ↑ ↓ for a column) move between its buttons and on through All / Clear,
 * Shift-Enter or Shift-Space is "only this" ([onOnlyThis], Alt-click with the mouse), Enter can be given a "go to" of its own
 * ([onEnter]), and a letter focuses the next button
 * whose label starts with it ([ButtonTypeAhead]).
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalComposeUiApi::class)
@Composable
fun MultiButtonSelect(
    buttonSelections: List<Pair<ButtonData, ToggleState>>,
    contentMaker: @Composable (Int) -> Unit = {},
    buttonBarSide: ViewSide = ViewSide.Top,
    groupingConstraint: MbsGroupingConstraint = MbsGroupingConstraint.NoConstraint,
    /**
     * Floor on how many buttons stay selected in **controlled-multi** mode (`selectedMask` +
     * `onToggle`). Default 0 = no floor.
     *
     * Use it when an empty selection has no meaning — a chart with nothing to draw reads as a
     * broken panel rather than as a filter the reader applied. At the floor the holding buttons
     * simply refuse the flip; they are **not** disabled, because [ToggleImageButton] fades disabled
     * content to 38% and a selected-but-faded button reads as deselected, which is the opposite of
     * what the floor is protecting.
     *
     * [groupingConstraint] does not cover this: it is read only on the uncontrolled path, and its
     * `ExactlyOne` is radio-style single-select rather than a minimum on a pick-many set.
     */
    minSelected: Int = 0,
    equalSlots: Boolean = false,
    buttonSize: Dp = 48.dp,
    selectedIndex: Int? = null,
    onSelectedIndexChange: ((Int) -> Unit)? = null,
    selectedMask: List<Boolean>? = null,
    onToggle: ((Int) -> Unit)? = null,
    toolbarOnly: Boolean = false,
    fillPanelHeight: Boolean = true,
    slotSpacing: Dp? = null,
    tooltips: List<String>? = null,
    enabledMask: List<Boolean>? = null,
    widthMode: ToggleButtonWidth = ToggleButtonWidth.Square,
    labelTextStyle: TextStyle? = null,
    bulkControl: MbsBulkControl = MbsBulkControl.None,
    /**
     * Non-selection affordance pinned to the **trailing edge of the toolbar band** — a panel-level
     * "How … works" chip is the intended use.
     *
     * It is laid out by the toolbar but is not part of it: no index, no toggle, no effect on
     * selection. Position is a design choice; semantics are unchanged. Same idea as [bulkControl],
     * which already rides this row.
     *
     * When set, the horizontal toolbar becomes a `FlowRow` with `SpaceBetween`: this sits hard
     * against the far edge while it and the buttons fit on one line, and **wraps to its own line**
     * when they do not. A `Row` cannot wrap, so on a narrow window a seven-slot toolbar would crowd
     * or clip it. Top-aligned with the image buttons; the buttons are laid out as one unit so they
     * are never redistributed by the arrangement.
     */
    trailing: (@Composable () -> Unit)? = null,
    bulkLabels: MbsBulkLabels? = null,
    onSetAll: ((Boolean) -> Unit)? = null,
    /**
     * `!` on a focused button, in place of this group's own All / Clear decision. For groups that
     * act as one with a sibling — the Forecast Detail core and tax toolbars, where `!` clears (or
     * fills) both at once. Null = this group alone.
     */
    onBang: (() -> Unit)? = null,
    slotAccents: List<MbsSlotAccent?>? = null,
    selectionMark: SelectionMark = SelectionMark.None,
    wrap: Boolean = false,
    panelTransition: MbsPanelTransition = MbsPanelTransition.None,
    toolbarAlignment: Alignment.Horizontal = Alignment.Start,
    /**
     * A [fieldTag] tag per button, for UI tests that pick an option by identity — see
     * [fieldOption]. Null entries (or a null list) leave that button untagged.
     */
    optionTags: List<String?>? = null,
    /**
     * "Only this button" — the keyboard's Shift-Enter on a focused button, and Alt-click. Null uses the control's
     * own rule where it has one: all off then this one on (a bulk multi-toggle; on a button already
     * alone, the inverse), or select it (a single select). A host whose "only" reaches beyond this
     * toolbar passes its own, and should undo itself the same way.
     */
    onOnlyThis: ((Int) -> Unit)? = null,
    /** Focus the first button when this is requested — how Ctrl-F lands on the chips it opened. */
    focusHandoff: FocusHandoff? = null,
    /**
     * Type-to-focus shared with other toolbars that read as one row, this one as [typeAheadSlot].
     * Null gives the toolbar a group of its own.
     */
    typeAhead: ButtonTypeAhead? = null,
    typeAheadSlot: Int = 0,
    /** ↓ from a button in a row toolbar, when something below should take focus; true if it did. */
    onFocusBelow: (() -> Boolean)? = null,
    /** ↑ from a button in a row toolbar, when something above should take focus; true if it did. */
    onFocusAbove: (() -> Boolean)? = null,
    /**
     * ← from the first button leaves the row ([onFocusAbove]) instead of moving to the previous
     * button. Later buttons keep ← as previous, so a second toolbar's first chip can still reach
     * the toolbar before it.
     */
    leftFromFirstLeaves: Boolean = false,
    /** Extra modifier on button [index], ahead of its click — how a host makes buttons walk stops. */
    buttonModifier: @Composable (index: Int) -> Modifier = { Modifier },
    /**
     * Enter on focused button [index], in place of its toggle; true when taken. A chip row uses it
     * to "go to" what the chip shows — turn it on and open it. Space still toggles.
     */
    onEnter: ((index: Int) -> Boolean)? = null,
    /**
     * Per button: on, but what it shows is folded away ([ToggleImageButton]'s `folded`) — a chip
     * whose row is closed. Only the look; what a click does is still [onToggle]'s to decide.
     */
    foldedMask: List<Boolean>? = null,
    modifier: Modifier = Modifier,
) {
    require(optionTags == null || optionTags.size == buttonSelections.size) {
        "optionTags size must match buttonSelections"
    }
    require(selectedMask == null || selectedMask.size == buttonSelections.size) {
        "selectedMask size must match buttonSelections"
    }
    require(tooltips == null || tooltips.size == buttonSelections.size) {
        "tooltips size must match buttonSelections"
    }
    // An icon button with no tip of its own names itself on hover. Null when no button has a name,
    // so a text toolbar keeps its bare buttons. Fixed for the toolbar's life (it follows the
    // buttons, not their state), so the HoverTooltip wrap below never comes and goes.
    val tips = tooltips ?: buttonSelections.map { it.first.hoverName }.takeIf { names -> names.any { it.isNotEmpty() } }
    require(foldedMask == null || foldedMask.size == buttonSelections.size) {
        "foldedMask size must match buttonSelections"
    }
    require(enabledMask == null || enabledMask.size == buttonSelections.size) {
        "enabledMask size must match buttonSelections"
    }
    require(slotAccents == null || slotAccents.size == buttonSelections.size) {
        "slotAccents size must match buttonSelections"
    }
    require(selectedMask == null || onToggle != null) {
        "onToggle is required when selectedMask is set"
    }
    // Legality is derived from the constraint, not configured beside it: a caller must not be able
    // to ask for an affordance whose action the grouping rules would then have to refuse.
    require(bulkControl == MbsBulkControl.None || groupingConstraint == MbsGroupingConstraint.NoConstraint) {
        "bulkControl requires MbsGroupingConstraint.NoConstraint (got $groupingConstraint)"
    }
    require(bulkControl == MbsBulkControl.None || selectedIndex == null) {
        "bulkControl is meaningless in controlled single-select mode"
    }
    require(bulkControl == MbsBulkControl.None || bulkLabels != null) {
        "bulkLabels are required when bulkControl is set"
    }
    require(bulkControl == MbsBulkControl.None || selectedMask == null || onSetAll != null) {
        "onSetAll is required for bulk selection in controlled multi-toggle mode — onToggle flips, " +
            "so it cannot express select-all"
    }
    require(!wrap || !equalSlots) {
        "wrap is incompatible with equalSlots (equal slots need a single measured row)"
    }
    require(selectionMark == SelectionMark.None || widthMode == ToggleButtonWidth.Fit) {
        "selectionMark needs ToggleButtonWidth.Fit — a square toggle has no room beside its glyph"
    }

    val controlledSingle = selectedIndex != null && selectedMask == null
    val controlledMulti = selectedMask != null
    val states = remember {
        mutableStateListOf(*buttonSelections.map { it.second }.toTypedArray())
    }

    fun stateAt(i: Int): ToggleState = when {
        controlledMulti -> if (selectedMask[i]) ToggleState.Selected else ToggleState.Deselected
        controlledSingle -> if (i == selectedIndex) ToggleState.Selected else ToggleState.Deselected
        else -> states[i]
    }

    fun update(i: Int, newState: ToggleState) {
        if (controlledMulti) {
            // Caller owns the mask; ToggleImageButton always emits the toggled state.
            if (!mbsMayToggle(selectedMask, i, minSelected)) return
            onToggle!!.invoke(i)
            return
        }
        if (controlledSingle) {
            // Controlled ExactlyOne: selecting i switches; deselecting the active i is a no-op
            // (always keep one view). ToggleImageButton always emits the toggled state on click.
            when (newState) {
                ToggleState.Selected -> onSelectedIndexChange?.invoke(i)
                ToggleState.Deselected -> onSelectedIndexChange?.invoke(selectedIndex)
            }
            return
        }
        when (newState) {
            ToggleState.Selected -> when (groupingConstraint) {
                MbsGroupingConstraint.ExactlyOne, MbsGroupingConstraint.OneOrNone ->
                    states.indices.forEach { j ->
                        states[j] = if (j == i) ToggleState.Selected else ToggleState.Deselected
                    }
                MbsGroupingConstraint.NoConstraint -> states[i] = newState
            }
            ToggleState.Deselected -> when (groupingConstraint) {
                MbsGroupingConstraint.ExactlyOne -> {
                    states.indices.forEach { j -> states[j] = ToggleState.Deselected }
                    states[0] = ToggleState.Selected
                }
                MbsGroupingConstraint.NoConstraint, MbsGroupingConstraint.OneOrNone ->
                    states[i] = newState
            }
        }
    }

    // Which button holds focus, for Shift-Enter. Plain, not state: nothing draws from it.
    val focusedButton = remember { intArrayOf(-1) }
    val focusManager = LocalFocusManager.current
    val windowInfo = LocalWindowInfo.current

    /**
     * "Only this" on button [i]; false when this toolbar has none (a multi-toggle with no All). On a
     * button already alone, the inverse (every other on, this one off), so doing it twice undoes it.
     */
    fun onlyThis(i: Int): Boolean {
        if (enabledMask?.getOrNull(i) == false) return true
        val alone = buttonSelections.indices.all { (stateAt(it) == ToggleState.Selected) == (it == i) }
        when {
            onOnlyThis != null -> onOnlyThis(i)
            controlledMulti && onSetAll != null -> {
                onSetAll(alone)
                onToggle!!.invoke(i)
            }
            controlledMulti -> return false
            controlledSingle -> onSelectedIndexChange?.invoke(i)
            groupingConstraint != MbsGroupingConstraint.NoConstraint -> update(i, ToggleState.Selected)
            else -> states.indices.forEach { j ->
                states[j] = if ((j == i) != alone) ToggleState.Selected else ToggleState.Deselected
            }
        }
        return true
    }

    val group = typeAhead ?: remember { ButtonTypeAhead() }
    val buttonFocus = remember(buttonSelections.size) { List(buttonSelections.size) { FocusRequester() } }
    val typeAheadButtons = buttonSelections.mapIndexed { i, (data, _) ->
        ButtonTypeAhead.Button(data.typeAheadText(), buttonFocus[i])
    }
    DisposableEffect(group, typeAheadSlot, typeAheadButtons.map { it.text }) {
        group.register(typeAheadSlot, typeAheadButtons)
        onDispose { group.unregister(typeAheadSlot, typeAheadButtons) }
    }

    val vertical = buttonBarSide == ViewSide.Left || buttonBarSide == ViewSide.Right
    val toolbarKeys = Modifier
        // Preview: a focused button would otherwise take Shift-Enter as a plain click.
        .onPreviewKeyEvent { event ->
            val i = focusedButton[0]
            val only = i >= 0 && event.type == KeyEventType.KeyDown && event.isShiftPressed &&
                !event.isCtrlPressed && !event.isAltPressed && !event.isMetaPressed &&
                (event.key == Key.Enter || event.key == Key.NumPadEnter || event.key == Key.Spacebar)
            if (only) {
                onlyThis(i)
                return@onPreviewKeyEvent true
            }
            i >= 0 && event.isPlainKeyDown() && event.isEnterKey && onEnter?.invoke(i) == true
        }
        .onKeyEvent { event ->
            // `!` (Shift-1) before letters: a test key's Shift-1 still carries the digit '1',
            // which the letter search would otherwise take. On when any chip is off; off when all are on.
            if (event.isBang() && bulkControl == MbsBulkControl.AllAndClear && focusedButton[0] >= 0) {
                onBang?.let {
                    it()
                    return@onKeyEvent true
                }
                val allOn = buttonSelections.indices.all { stateAt(it) == ToggleState.Selected }
                if (controlledMulti) onSetAll!!.invoke(!allOn)
                else {
                    val target = if (allOn) ToggleState.Deselected else ToggleState.Selected
                    states.indices.forEach { j -> states[j] = target }
                }
                return@onKeyEvent true
            }
            // A letter first — with Shift it searches backwards, so Shift must not stop it here.
            val typed = event.typeAheadLetter()
            if (typed != null && focusedButton[0] >= 0) {
                return@onKeyEvent group.onChar(typed, typeAheadSlot, focusedButton[0], backward = event.isShiftPressed)
            }
            if (event.type != KeyEventType.KeyDown || event.hasChord || event.isShiftPressed) {
                return@onKeyEvent false
            }
            when (event.walkKey) {
                Key.DirectionDown if !vertical -> onFocusBelow?.invoke() ?: false
                Key.DirectionUp if !vertical -> onFocusAbove?.invoke() ?: false
                Key.DirectionLeft if !vertical && leftFromFirstLeaves && focusedButton[0] == 0 ->
                    onFocusAbove?.invoke() ?: focusManager.moveFocus(FocusDirection.Previous)
                if (vertical) Key.DirectionDown else Key.DirectionRight -> focusManager.moveFocus(FocusDirection.Next)
                if (vertical) Key.DirectionUp else Key.DirectionLeft -> focusManager.moveFocus(FocusDirection.Previous)
                else -> false
            }
        }

    val panels: @Composable () -> Unit = {
        if (panelTransition == MbsPanelTransition.SlideHorizontal && selectedMask == null && selectedIndex != null) {
            // One AnimatedContent keyed on the index, rather than the per-index `if` below: a
            // conditional group per slot has no continuity from one slot to the next, so there is
            // nothing for a transition to run between.
            AnimatedContent(
                targetState = selectedIndex,
                transitionSpec = {
                    val forward = targetState > initialState
                    (slideInHorizontally { w -> if (forward) w else -w } + fadeIn())
                        .togetherWith(slideOutHorizontally { w -> if (forward) -w else w } + fadeOut())
                        .using(SizeTransform(clip = true))
                },
                label = "MultiButtonSelectPanel",
            ) { i -> contentMaker(i) }
        } else {
            buttonSelections.indices.forEach { i ->
                if (stateAt(i) == ToggleState.Selected) contentMaker(i)
            }
        }
    }

    val gap = slotSpacing ?: if (equalSlots) SubsectionToolbar.SlotSpacing else 4.dp

    /**
     * @param tagged false for a copy composed only to be measured ([EqualWidthToggleRow]'s sizing
     *   pass): an unplaced twin carrying the same [fieldTag] tag would make the option's node
     *   ambiguous to a UI test, so only the placed copy is tagged.
     */
    @Composable
    fun toggleAt(i: Int, tagged: Boolean = true) {
        val tip = tips?.getOrNull(i).orEmpty()
        val button: @Composable () -> Unit = {
            ToggleImageButton(
                buttonData = buttonSelections[i].first,
                state = stateAt(i),
                // Alt-click is the mouse's Shift-Enter: only this one. Read at the click, as the
                // seam's Alt-click is ([SeamRegion]), and a plain click where there is no "only".
                onStateChange = { state ->
                    val only = windowInfo.keyboardModifiers.isAltPressed && onlyThis(i)
                    if (!only) update(i, state)
                },
                enabled = enabledMask?.getOrNull(i) ?: true,
                buttonSize = buttonSize,
                widthMode = widthMode,
                textStyle = labelTextStyle ?: MaterialTheme.typography.headlineMedium,
                selectedAccent = slotAccents?.getOrNull(i)?.selected,
                deselectedAccent = slotAccents?.getOrNull(i)?.deselected,
                selectionMark = selectionMark,
                folded = foldedMask?.getOrNull(i) ?: false,
                // An aligned list of side-toolbar buttons reads down its marks, so their faces start at one edge.
                faceAlignment = if (equalSlots && (buttonBarSide == ViewSide.Left || buttonBarSide == ViewSide.Right)) {
                    Alignment.CenterStart
                } else {
                    Alignment.Center
                },
                modifier = (optionTags?.getOrNull(i)?.takeIf { tagged }?.let { Modifier.fieldTag(it) } ?: Modifier)
                    .onFocusChanged { focus ->
                        when {
                            focus.hasFocus -> focusedButton[0] = i
                            focusedButton[0] == i -> focusedButton[0] = -1
                        }
                    }
                    .then(if (tagged) Modifier.focusRequester(buttonFocus[i]).then(buttonModifier(i)) else Modifier)
                    // A single-select lands on its selected button (the open tab), else the first.
                    .then(if (i == (selectedIndex ?: 0) && tagged) Modifier.takesFocusHandoff(focusHandoff) else Modifier),
            )
        }
        // Wrapped whenever the toolbar has tips, blank or not ([HoverTooltip] sleeps on a blank one):
        // a tip that comes and goes must not move the button in the tree, or a focused button is
        // rebuilt and focus drops — a Simulation Summary chip's folded tip did exactly that.
        if (tips != null) {
            HoverTooltip(text = tip) { button() }
        } else {
            button()
        }
    }

    /** Set every entry — a bulk *set*, never a per-index flip. */
    fun setAll(selected: Boolean) {
        if (controlledMulti) {
            onSetAll!!.invoke(selected)
            return
        }
        val target = if (selected) ToggleState.Selected else ToggleState.Deselected
        states.indices.forEach { j -> states[j] = target }
    }

    val allSelected = buttonSelections.indices.all { stateAt(it) == ToggleState.Selected }

    val noneSelected = buttonSelections.indices.none { stateAt(it) == ToggleState.Selected }

    @Composable
    fun bulkButton() {
        if (bulkControl == MbsBulkControl.None || buttonSelections.isEmpty()) return
        val labels = bulkLabels!!
        // Stacked, so the pair costs a narrow column rather than widening the toggle row. Each is
        // disabled when it would be a no-op, which also makes the pair read as current state.
        //
        // Bottom-aligned to the **button surface**, not to the whole toggle column. A square icon
        // toolbar is surface + indicator + label, so centering against all of that pushed the pair
        // low enough that reaching Clear felt like a detour downward rather than a move straight
        // right. Padding the stack down to end exactly where the indicator begins puts it level
        // with the buttons it acts on. Text-mode toggles are already the same height as the stack,
        // so the pad computes to zero and nothing moves.
        // Two **thin** [ActionPill]s, stacked. Stacked so the pair costs a narrow column rather
        // than widening the toggle row; thin so the stack is about the height of the toggles it
        // sits beside instead of half again taller. (They were a bespoke pair of 9sp micro-buttons
        // — a third small-button vocabulary — before the pill existed.) Each is disabled when it
        // would be a no-op, which is what lets the pair read as state as well as control.
        //
        // Centered on the **button surface**, not on the whole toggle column (a square icon
        // toolbar is surface + indicator + label). The stack is a little taller than a text-mode
        // toggle, so it is offset rather than padded: the same sliver shows above All as below
        // Clear, and the row's own height is untouched.
        Column(
            verticalArrangement = Arrangement.spacedBy(ActionPillDefaults.ThinStackGap),
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.offset(y = (buttonSize - BulkStackHeight) / 2),
        ) {
            ActionPill(
                label = labels.selectAll,
                kind = ActionKind.Add,
                density = ActionPillDensity.Thin,
                enabled = !allSelected,
                onClick = { setAll(true) },
            )
            ActionPill(
                label = labels.clear,
                kind = ActionKind.Clear,
                density = ActionPillDensity.Thin,
                enabled = !noneSelected,
                onClick = { setAll(false) },
            )
        }
    }

    val bareToolbarRow: @Composable () -> Unit = {
        if (wrap) {
            // Reflow text-width toggles (Guardrails study legend). All/Clear sit just right of
            // the chip cluster — not pinned to the panel trailing edge. Cap the FlowRow's max
            // width so chips still wrap, but the Row itself sizes to content (chips + bulk).
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                // The stacked All / Clear column is narrow; reserve its width.
                val bulkReserve = if (bulkControl == MbsBulkControl.None) 0.dp else BulkPairReserve
                val flowMax = (maxWidth - bulkReserve - gap).coerceAtLeast(0.dp)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.wrapContentWidth(align = Alignment.Start),
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(gap),
                        verticalArrangement = Arrangement.spacedBy(gap),
                        modifier = Modifier.widthIn(max = flowMax),
                    ) {
                        buttonSelections.indices.forEach { i -> toggleAt(i) }
                    }
                    bulkButton()
                    trailing?.let {
                        Spacer(Modifier.weight(1f))
                        it()
                    }
                }

            }
        } else {
            // The buttons as one unit, so a trailing slot can wrap away from them without the
            // buttons themselves being redistributed.
            val buttonBlock: @Composable () -> Unit = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    verticalAlignment = Alignment.Top,
                ) {
                    if (equalSlots) {
                        // Left-aligned equal-width slots sized to the widest button (not parent fill).
                        EqualWidthToggleRow(
                            count = buttonSelections.size,
                            spacing = gap,
                        ) { i, placed -> toggleAt(i, tagged = placed) }
                    } else {
                        buttonSelections.indices.forEach { i -> toggleAt(i) }
                    }
                    bulkButton()
                }
            }
            if (trailing != null) {
                // FlowRow, not Row: with a seven-slot toolbar on a narrow window there is no room
                // for the chip beside the last button, and a Row cannot wrap — it would crowd or
                // clip it. SpaceBetween puts the chip hard right while both fit on one line, and
                // drops it to its own line when they do not.
                FlowRow(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(gap),
                    itemVerticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    buttonBlock()
                    trailing()
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    // Top, so the bulk stack's own top padding decides where it sits (see bulkButton).
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.wrapContentWidth(),
                ) {
                    buttonBlock()
                }
            }
        }
    }
    val bareToolbarColumn: @Composable () -> Unit = {
        Column(
            verticalArrangement = Arrangement.spacedBy(gap),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (equalSlots) {
                // A column of choices reads as a list only when its buttons line up: each as wide as the widest.
                EqualWidthToggleColumn(count = buttonSelections.size, spacing = gap) { i, placed -> toggleAt(i, tagged = placed) }
            } else {
                buttonSelections.indices.forEach { i -> toggleAt(i) }
            }
            bulkButton()
        }
    }

    // The keys ride on a wrapper that passes its constraints straight through, so the toolbar lays
    // out exactly as it would bare.
    val toolbarRow: @Composable () -> Unit = { Box(toolbarKeys, propagateMinConstraints = true) { bareToolbarRow() } }
    val toolbarColumn: @Composable () -> Unit = { Box(toolbarKeys, propagateMinConstraints = true) { bareToolbarColumn() } }

    if (toolbarOnly) {
        // Inline toolbars (e.g. Forecast Detail chips next to FID·Year) — no content weight box.
        Box(modifier) {
            when (buttonBarSide) {
                ViewSide.Top, ViewSide.Bottom -> toolbarRow()
                ViewSide.Left, ViewSide.Right -> toolbarColumn()
            }
        }
        return
    }

    when (buttonBarSide) {
        // A non-Start alignment centers every child of the column, the panel slot included; the
        // slot is pinned full-width in that case so only the toolbar moves.
        ViewSide.Top -> Column(modifier, horizontalAlignment = toolbarAlignment) {
            toolbarRow()
            Box(modifier = panelSlotModifier(fillPanelHeight, toolbarAlignment)) { panels() }
        }
        ViewSide.Bottom -> Column(modifier, horizontalAlignment = toolbarAlignment) {
            Box(modifier = panelSlotModifier(fillPanelHeight, toolbarAlignment)) { panels() }
            toolbarRow()
        }
        ViewSide.Left -> Row(modifier) {
            toolbarColumn()
            Box(modifier = if (fillPanelHeight) Modifier.weight(1f) else Modifier) { panels() }
        }
        ViewSide.Right -> Row(modifier) {
            Box(modifier = if (fillPanelHeight) Modifier.weight(1f) else Modifier) { panels() }
            toolbarColumn()
        }
    }
}

// Weight so panel content can fill remaining height (Risk charts, Assumptions scroll) — unless the
// caller says its parent is unbounded, where a weight would measure to zero.
private fun ColumnScope.panelSlotModifier(fillPanelHeight: Boolean, toolbarAlignment: Alignment.Horizontal): Modifier {
    val height = if (fillPanelHeight) Modifier.weight(1f, fill = true) else Modifier
    return if (toolbarAlignment == Alignment.Start) height else height.fillMaxWidth()
}

/** Height of the stacked All / Clear pair; used to bottom-align it with the toggle surfaces. */
private val BulkStackHeight: Dp = ActionPillDefaults.ThinHeight * 2 + ActionPillDefaults.ThinStackGap

/** Width the wrap layout keeps free for the stacked All / Clear pair beside the chip cluster. */
private val BulkPairReserve: Dp = 64.dp

/**
 * Horizontal row of [count] children, each forced to the same width (the max natural width)
 * so icon centers share a constant pitch. The row is only as wide as those slots — left-aligned
 * under a wide parent — and shrinks uniformly when the parent is narrower than the natural row.
 */
@Composable
private fun EqualWidthToggleRow(
    count: Int,
    spacing: Dp,
    /** [placed] is false for the sizing pass, whose copies are measured and never placed. */
    content: @Composable (index: Int, placed: Boolean) -> Unit,
) {
    SubcomposeLayout(modifier = Modifier.wrapContentWidth(align = Alignment.Start)) { constraints ->
        if (count <= 0) {
            return@SubcomposeLayout layout(0, 0) {}
        }
        val spacingPx = spacing.roundToPx()
        val gaps = spacingPx * (count - 1)

        val naturalWidths = List(count) { i ->
            subcompose("measure-$i") { content(i, false) }
                .first()
                .measure(Constraints())
                .width
        }
        val naturalSlot = naturalWidths.maxOrNull() ?: 0
        val maxContentWidth = if (constraints.hasBoundedWidth) {
            (constraints.maxWidth - gaps).coerceAtLeast(0)
        } else {
            Int.MAX_VALUE
        }
        val slotWidth = minOf(naturalSlot, maxContentWidth / count).coerceAtLeast(0)

        val placeables = List(count) { i ->
            subcompose("place-$i") { content(i, true) }
                .first()
                .measure(Constraints(minWidth = slotWidth, maxWidth = slotWidth))
        }
        val height = placeables.maxOfOrNull { it.height } ?: 0
        val width = slotWidth * count + gaps
        layout(width, height) {
            var x = 0
            placeables.forEach { placeable ->
                placeable.placeRelative(x, 0)
                x += slotWidth + spacingPx
            }
        }
    }
}

/**
 * The vertical twin of [EqualWidthToggleRow]: [count] children stacked with [spacing] between them, each as wide
 * as the widest one's natural width (capped by the parent's width), so a side toolbar's buttons line up as a list.
 */
@Composable
private fun EqualWidthToggleColumn(
    count: Int,
    spacing: Dp,
    /** [placed] is false for the sizing pass, whose copies are measured and never placed. */
    content: @Composable (index: Int, placed: Boolean) -> Unit,
) {
    SubcomposeLayout { constraints ->
        if (count <= 0) {
            return@SubcomposeLayout layout(0, 0) {}
        }
        val spacingPx = spacing.roundToPx()
        val naturalSlot = List(count) { i ->
            subcompose("measure-$i") { content(i, false) }.first().measure(Constraints()).width
        }.maxOrNull() ?: 0
        val slotWidth = if (constraints.hasBoundedWidth) minOf(naturalSlot, constraints.maxWidth) else naturalSlot
        val placeables = List(count) { i ->
            subcompose("place-$i") { content(i, true) }
                .first()
                .measure(Constraints(minWidth = slotWidth, maxWidth = slotWidth))
        }
        val height = placeables.sumOf { it.height } + spacingPx * (count - 1)
        layout(slotWidth, height) {
            var y = 0
            placeables.forEach { placeable ->
                placeable.placeRelative(0, y)
                y += placeable.height + spacingPx
            }
        }
    }
}

/**
 * Shared metrics for [MultiButtonSelect]. Tweak here to resize every multi-select at once — same
 * single-source-of-truth idea as [CompactButtonDefaults].
 */
object MultiButtonSelectDefaults {
    // ── Text-toggle rows on the tax lenses ───────────────────────────────────
    /**
     * Height of a text-width ([ToggleButtonWidth.Fit]) toggle in a lens control row — the cliff
     * radar's lane groups and the Brackets lens's "Room to" row. Height only; width follows the
     * label.
     *
     * Shared so the two lenses, which stack on the same card and are read together, cannot drift
     * into two chip sizes. It moved here when Brackets swapped its Material segmented row for this
     * same toggle toolbar.
     */
    val LensToggleHeight: Dp = 26.dp

    /** Gap between toggles inside one lens control row. See [LensToggleHeight]. */
    val LensToggleSlotSpacing: Dp = 6.dp

    // ── Compact (desktop / web toolbar) ──────────────────────────────────────
    /** Snug horizontal inset for a text-only chip — still clears the rounded edge. */
    val CompactHorizontalPadding: Dp = 8.dp
    val CompactVerticalPadding: Dp = 2.dp
    /**
     * Toggle height for desktop/web toolbar rows — the saved-plan view modes, the display-basis
     * poles, the person editor's Role. Paired with [LocalMinimumInteractiveComponentSize] = 0 so
     * Material's ~40.dp minimum does not win.
     */
    val CompactHeight: Dp = 28.dp
}
