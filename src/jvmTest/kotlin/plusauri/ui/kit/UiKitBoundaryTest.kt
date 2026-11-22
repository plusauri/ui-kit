package plusauri.ui.kit

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The kit's wall: it depends on Compose and nothing else, and it explains itself in its own terms.
 *
 * Three checks, each closing a different hole:
 * - **Dependencies.** Every `implementation(…)` / `api(…)` line in the kit's and the gallery's build
 *   files must be on [ALLOWED_DEPENDENCIES]. Without this a build file could gain a library, or a
 *   `project(…)`, and the compile would honor it.
 * - **Imports.** A main source file of the kit or the gallery may import only the kit itself, Compose,
 *   Kotlin, kotlinx and the JDK ([ALLOWED_IMPORTS]). Tests may import more (JUnit, the UI test harness).
 * - **Words.** No file under `src/` or `gallery/`, and no build file, may use a host product's vocabulary
 *   ([HOST_WORDS]), in code or in comments. The kit's KDoc ships in the sources jar and shows on hover
 *   in every host's IDE, so it describes controls in general terms ("a table row", "a side panel"),
 *   never through one product's screens.
 */
class UiKitBoundaryTest {

    private val moduleDir = File(".").canonicalFile

    @Test
    fun `the kit and the gallery declare only allowed dependencies`() {
        val offenders = listOf("build.gradle.kts", "gallery/build.gradle.kts").flatMap { path ->
            val file = File(moduleDir, path)
            assertTrue(file.isFile, "run from the kit's root directory, not ${moduleDir.path}: $path is missing")
            dependencyOffenses(file.readText()).map { "$path:$it" }
        }
        assertTrue(offenders.isEmpty(), "the kit depends on Compose and nothing else:\n" + offenders.joinToString("\n"))
    }

    @Test
    fun `main sources import only the kit, Compose, Kotlin and the JDK`() {
        val offenders = listOf(File(moduleDir, "src"), File(moduleDir, "gallery/src")).flatMap { src ->
            src.listFiles().orEmpty()
                .filter { it.isDirectory && it.name.endsWith("Main") }
                .flatMap { set -> set.walk().filter { it.isFile && it.extension == "kt" }.toList() }
                .flatMap { file -> importOffenses(file.readText()).map { "${file.relativeTo(moduleDir)}:$it" } }
        }
        assertTrue(offenders.isEmpty(), "a kit or gallery source imports outside the kit's rule:\n" + offenders.joinToString("\n"))
    }

    @Test
    fun `no source, comment or build file uses a host product's vocabulary`() {
        val self = File(moduleDir, "src/jvmTest/kotlin/plusauri/ui/kit/UiKitBoundaryTest.kt").canonicalFile
        val files = (File(moduleDir, "src").walk() + File(moduleDir, "gallery/src").walk())
            .filter { it.isFile && it.extension in setOf("kt", "kts", "html", "md") && it.canonicalFile != self }.toList() +
            listOf("build.gradle.kts", "settings.gradle.kts", "gallery/build.gradle.kts", "gradle.properties")
                .map { File(moduleDir, it) }
        val offenders = files.flatMap { file -> wordOffenses(file.readText()).map { "${file.relativeTo(moduleDir)}:$it" } }
        assertTrue(
            offenders.isEmpty(),
            "describe the control in general terms, not through one host's screens:\n" + offenders.joinToString("\n"),
        )
    }

    /** Proves each scan catches what it is for, so an empty result means clean, not blind. */
    @Test
    fun `the scans catch what they are for`() {
        val deps = dependencyOffenses(
            """
            implementation(compose.material3)
            implementation(project(":"))
            implementation(project(":engine"))
            implementation("com.example:charts:1.0")
            implementation(kotlin("test"))
            api(libs.something)
            // implementation("com.example:in-a-comment:1.0")
            """.trimIndent(),
        )
        assertEquals(listOf(3, 4, 6), deps.map { it.substringBefore(" ").toInt() })

        val imports = importOffenses(
            """
            import androidx.compose.material3.Text
            import plusauri.ui.TipIconButton
            import kotlin.math.abs
            import kotlinx.coroutines.delay
            import java.awt.Toolkit
            import org.jetbrains.compose.resources.painterResource
            import com.example.model.Plan
            import plusauri.uikit.Other
            """.trimIndent(),
        )
        assertEquals(listOf(7, 8), imports.map { it.substringBefore(" ").toInt() })

        val words = wordOffenses(
            """
            /** Opens like the Bird's Eye modal. */
            // See the dossier pane.
            val shown = "Forecast Detail"
            /** A side panel that folds away. */
            import tutil.ui.Thing
            """.trimIndent(),
        )
        assertEquals(listOf(1, 2, 3, 5), words.map { it.substringBefore(" ").toInt() })
    }

    private fun dependencyOffenses(text: String): List<String> =
        text.lines().mapIndexedNotNull { i, line ->
            val code = line.trim()
            val call = DEPENDENCY_CALL.find(code)
            when {
                code.startsWith("//") || code.startsWith("*") || call == null -> null
                ALLOWED_DEPENDENCIES.any { it.matches(call.groupValues[1].trim()) } -> null
                else -> "${i + 1}  $code"
            }
        }

    private fun importOffenses(text: String): List<String> =
        text.lines().mapIndexedNotNull { i, line ->
            val code = line.trim()
            if (!code.startsWith("import ")) null
            else if (ALLOWED_IMPORTS.any { code.removePrefix("import ").startsWith(it) }) null
            else "${i + 1}  $code"
        }

    private fun wordOffenses(text: String): List<String> =
        text.lines().mapIndexedNotNull { i, line -> if (HOST_WORDS.containsMatchIn(line)) "${i + 1}  ${line.trim()}" else null }

    private companion object {
        val DEPENDENCY_CALL = Regex("""^(?:implementation|api|compileOnly|runtimeOnly)\((.*)\)$""")

        val ALLOWED_DEPENDENCIES = listOf(
            Regex("""compose\.[\w.]+"""),
            Regex("""kotlin\("test"\)"""),
            Regex("""project\(":"\)"""),
            Regex(""""org\.junit\.jupiter:junit-jupiter:[\w.-]+""""),
        )

        val ALLOWED_IMPORTS = listOf(
            "plusauri.ui.", "androidx.compose.", "org.jetbrains.compose.resources.", "org.jetbrains.skia.",
            "kotlin.", "kotlinx.coroutines.", "kotlinx.browser.", "java.", "javax.",
        )

        /**
         * Words that belong to a host product, not to a general control kit: its package and model
         * names, and the names of its screens and features.
         */
        val HOST_WORDS = Regex(
            """(?<![\w.])tutil[._]\w|Tutilis|Loquilis|AppStrings|[Dd]ossier|Bird's Eye|BirdsEye|Forecast(er\b| Detail)|""" +
                """Guardrails|Learning Center|\blessons?\b|Cogilis|design partner|\bSimulator\b|Glossary|Medicare|Social Security|\bRoth\b|Sankey""",
        )
    }
}
