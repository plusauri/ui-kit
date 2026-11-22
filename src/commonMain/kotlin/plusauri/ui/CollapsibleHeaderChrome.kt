package plusauri.ui

/**
 * Header bar fill for [CollapsibleSection].
 *
 * - [Section] — soft-sky top-level chrome ([collapsibleHeaderBarColor]); the default for a
 *   top-level section.
 * - [Nested] — warm peach chrome ([nestedCollapsibleHeaderBarColor]) for a collapsible that
 *   sits **inside** a Section bar so the two levels do not look identical.
 * - [Phase] — pale ice blue-gray with a border and a softened chevron, for the bar over one step
 *   of a sequence (one stage of a multi-stage editor, say). Its own value rather than a recolor of
 *   [Nested], so ordinary nested bars keep their peach.
 */
enum class CollapsibleHeaderChrome {
    Section,
    Nested,
    Phase,
}
