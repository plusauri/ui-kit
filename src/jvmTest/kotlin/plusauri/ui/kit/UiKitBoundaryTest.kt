package plusauri.ui.kit

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
        val offenders = sourceOffenders(File(moduleDir, "src"))
        assertTrue(
            offenders.isEmpty(),
            "`:ui-kit` takes strings and plain values; it does not reach into the product:\n" +
                offenders.joinToString("\n"),
        )
    }

    /**
     * The gallery shows the kit the way a host would use it, so it keeps the kit's rule: its one
     * project dependency is the kit, and its sources name no product package. A gallery that reached
     * into a product would stop proving the kit stands on its own.
     */
    @Test
    fun `the gallery depends on the kit alone and names no product package`() {
        val gallery = File(moduleDir, "gallery")
        val build = File(gallery, "build.gradle.kts")
        assertTrue(build.isFile, "the gallery's build file is missing: ${build.path}")
        val projectDeps = build.readLines().mapIndexedNotNull { i, line ->
            val code = line.trim()
            if (code.startsWith("//") || code.startsWith("*") || "project(" !in code) null
            else if ("""project(":")""" in code) null
            else "gallery/build.gradle.kts:${i + 1}  $code"
        }
        assertTrue(projectDeps.isEmpty(), "`:gallery` may depend only on the kit and Compose:\n" + projectDeps.joinToString("\n"))

        val offenders = sourceOffenders(File(gallery, "src"))
        assertTrue(
            offenders.isEmpty(),
            "`:gallery` passes plain words in, as a host does; it does not name a product:\n" +
                offenders.joinToString("\n"),
        )
    }

    private fun sourceOffenders(srcDir: File): List<String> =
        srcDir.listFiles().orEmpty()
            .filter { it.isDirectory && it.name.endsWith("Main") }
            .flatMap { set -> set.walk().filter { it.isFile && it.extension == "kt" }.toList() }
            .flatMap { file -> offenses(file.readText()).map { "${file.relativeTo(moduleDir)}:$it" } }

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
            import plusauri.ui.TipIconButton
            import tutil.ui.TipIconButton
            val color = tutil.app.Accent
            """.trimIndent(),
        )
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7, 11, 12), caught.map { it.substringBefore(" ").toInt() })
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
        /**
         * Every product package starts `tutil.` or `tutil_` (`tutil_modeled` is the Wire model). The kit
         * lives in `plusauri.ui` since step 12 of `extract-ui-kit`, so it has no reason to name any of
         * them, and the repository it moves to should never carry the name.
         */
        val FORBIDDEN = Regex("""(?<![\w.])tutil[._]\w""")
    }
}
