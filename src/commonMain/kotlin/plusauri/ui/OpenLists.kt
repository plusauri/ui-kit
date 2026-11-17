package plusauri.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

/**
 * The open lists that take Ctrl-Enter as their Enter: menus, selects, search results. Ctrl is usually
 * still held from Ctrl-N / Ctrl-P ([listStep]) when the Enter comes, and a host may bind Ctrl-Enter to
 * something of its own ahead of every control. So a host's window-level key handler reads [count] and,
 * while a list is open, leaves Ctrl-Enter to it; a row's `clickable` (which ignores Ctrl) then picks or
 * opens just as plain Enter would. [CompactDropdownMenu] registers itself.
 */
object OpenLists {
    var count: Int = 0
        private set

    /** Counts the calling list as open for as long as it is composed. Call it inside the list's popup. */
    @Composable
    fun Registered() {
        DisposableEffect(Unit) {
            count++
            onDispose { count-- }
        }
    }
}
