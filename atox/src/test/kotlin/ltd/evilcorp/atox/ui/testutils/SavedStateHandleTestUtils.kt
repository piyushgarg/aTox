package ltd.evilcorp.atox.ui.testutils

import androidx.lifecycle.SavedStateHandle

/**
 * Utility for creating a SavedStateHandle with predefined initial values for testing
 * process death and state restoration scenarios.
 */
object SavedStateHandleTestUtils {

    /**
     * Creates a SavedStateHandle initialized with the given map.
     */
    fun createSavedStateHandle(initialState: Map<String, Any?> = emptyMap()): SavedStateHandle {
        return SavedStateHandle(initialState)
    }
}
