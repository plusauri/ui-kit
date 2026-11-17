package plusauri.ui

/**
 * Header bar fill for [CollapsibleSection].
 *
 * - [Section] — soft-sky top-level chrome ([collapsibleHeaderBarColor]); default for Forecast
 *   Detail, Guardrails Studies, Bird's Eye Detail, etc.
 * - [Nested] — warm peach chrome ([nestedCollapsibleHeaderBarColor]) for a collapsible that
 *   sits **inside** a Section bar so the two levels do not look identical.
 * - [Phase] — pale ice blue-gray with a border and a softened chevron, for the bar over one phase
 *   of a phased generator (`PhaseSection`). Its own value rather than a recolor of [Nested]
 *   because the nest bars elsewhere (growth editor, Bird's Eye) keep peach.
 */
enum class CollapsibleHeaderChrome {
    Section,
    Nested,
    Phase,
}
