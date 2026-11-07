package tutil.ui.kit

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The kit's wall. A control in `:ui-kit` knows Compose and nothing of the product: not the dossier
 * model, the engine, reference data, search, the model tables, the shortcut table, FX, licensing, or
 * anything else `:tutil-ui` reaches, and not `AppStrings`. The product owns i18n and passes strings in.
 *
 * Two checks, because either alone has a hole. The build file can gain a `project(…)` dependency,
 * which the compile would then happily honor. And a source file can name `AppStrings` without any
 * dependency at all, by taking it as a parameter type once the kit is wired wrong.
 */
class UiKitBoundaryTest {

    private val moduleDir = File(".").canonicalFile

    @Test
    fun `the kit depends on no module in this repository`() {
        val build = File(moduleDir, "build.gradle.kts")
        assertTrue(build.isFile, "run from the :ui-kit module directory, not ${moduleDir.path}")
        val offenders = build.readLines().mapIndexedNotNull { i, line ->
            val code = line.trim()
            if (code.startsWith("//") || code.startsWith("*") || "project(" !in code) null
            else "build.gradle.kts:${i + 1}  $code"
        }
        assertTrue(offenders.isEmpty(), "`:ui-kit` may depend only on Compose:\n" + offenders.joinToString("\n"))
    }

    @Test
    fun `no kit source names a product package or AppStrings`() {
        val offenders = File(moduleDir, "src").listFiles().orEmpty()
            .filter { it.isDirectory && it.name.endsWith("Main") }
            .flatMap { set -> set.walk().filter { it.isFile && it.extension == "kt" }.toList() }
            .flatMap { file -> offenses(file.readText()).map { "${file.relativeTo(moduleDir)}:$it" } }
        assertTrue(
            offenders.isEmpty(),
            "`:ui-kit` takes strings and plain values; it does not reach into the product:\n" +
                offenders.joinToString("\n"),
        )
    }

    /** The scan is empty while the kit has no sources, so prove it would catch each shape. */
    @Test
    fun `the scan catches what it is for`() {
        val caught = offenses(
            """
            import tutil_modeled.NormalSpec
            import tutil.modeltables.label
            import tutil.shortcuts.ControlKey
            import tutil.ui.LocalAppStrings
            val years = tutil.reference.PlanYears
            import tutil.tables.DataTable
            fun f(s: AppStrings) = Unit
            // A comment may say AppStrings, tutil.forecast, and so on.
            import androidx.compose.material3.Text
            import tutil.ui.TipIconButton
            """.trimIndent(),
        )
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7), caught.map { it.substringBefore(" ").toInt() })
    }

    private fun offenses(text: String): List<String> =
        text.lines().mapIndexedNotNull { i, line ->
            val code = line.trim()
            when {
                code.startsWith("//") || code.startsWith("*") || code.startsWith("/*") -> null
                FORBIDDEN.containsMatchIn(code) || "AppStrings" in code -> "${i + 1}  $code"
                else -> null
            }
        }

    private companion object {
        /** Package roots of the modules the kit must not know (todo `extract-ui-kit`, "The module"). */
        val FORBIDDEN_PACKAGES = listOf(
            "tutil_modeled", "tutil.modeling", "tutil.anonymize", // :modeling
            "tutil.forecast", "tutil.taxes", // :forecast-engine (taxes also in :reference-data)
            "tutil.reference", // :reference-data
            "tutil.search", // :tutil-search
            "tutil.modeltables", // :model-tables
            "tutil.shortcuts", // :shortcuts
            "tutil.fx", // :fx-service (also in :modeling)
            "tutil.license", // :license-core, :license-client
            "tutil.assumptions", // :assumption-resolution
            "tutil.brand", // :brand
            "tutil.symbols", // :system-symbols
            "tutil.tables", // :tables
            "tutil.timevalue", // :time-value
        )

        val FORBIDDEN = Regex(
            """(?<![\w.])(""" + FORBIDDEN_PACKAGES.joinToString("|") { Regex.escape(it) } + """)\.""",
        )
    }
}
