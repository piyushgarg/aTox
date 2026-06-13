package ltd.evilcorp.atox.ui.testutils

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Custom matchers for testing padding, margins, and layout bounds.
 */
object LayoutMatchers {

    /**
     * Asserts that a node's width is exactly the specified Dp.
     */
    fun hasExactWidth(expectedWidth: Dp): SemanticsMatcher {
        return SemanticsMatcher("has exact width of $expectedWidth") { node ->
            val bounds = node.boundsInRoot
            val widthPx = bounds.width
            val expectedPx = with(node.layoutInfo.density) { expectedWidth.toPx() }
            // Allow 1px tolerance due to rounding
            Math.abs(widthPx - expectedPx) <= 1f
        }
    }

    /**
     * Asserts that a node's height is exactly the specified Dp.
     */
    fun hasExactHeight(expectedHeight: Dp): SemanticsMatcher {
        return SemanticsMatcher("has exact height of $expectedHeight") { node ->
            val bounds = node.boundsInRoot
            val heightPx = bounds.height
            val expectedPx = with(node.layoutInfo.density) { expectedHeight.toPx() }
            // Allow 1px tolerance due to rounding
            Math.abs(heightPx - expectedPx) <= 1f
        }
    }

    /**
     * Asserts that a node has a minimum touch target size (typically 48.dp for Material Design).
     */
    fun hasMinTouchTargetSize(minSize: Dp = 48.dp): SemanticsMatcher {
        return SemanticsMatcher("has minimum touch target size of $minSize") { node ->
            val bounds = node.touchBoundsInRoot
            val widthPx = bounds.width
            val heightPx = bounds.height
            val minPx = with(node.layoutInfo.density) { minSize.toPx() }
            widthPx >= minPx - 1f && heightPx >= minPx - 1f
        }
    }
}
