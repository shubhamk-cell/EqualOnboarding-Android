package ai.equal.orb.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Rows that wrap: each child at its own size, onto the next row when it won't fit, [spacing]
 * apart both ways. Rows sit on the left, or in the middle when [centered].
 */
@Composable
fun WrapRows(spacing: Dp, modifier: Modifier = Modifier, centered: Boolean = false, content: @Composable () -> Unit) {
    Layout(content, modifier) { measurables, constraints ->
        val gap = spacing.roundToPx()
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else 345.dp.roundToPx()
        val placeables = measurables.map { it.measure(Constraints(maxWidth = width)) }

        // Rows of child indices, each with its top and height.
        class Row(val items: List<Int>, val y: Int, val height: Int)
        val rows = mutableListOf<Row>()
        var items = mutableListOf<Int>()
        var x = 0; var y = 0; var h = 0
        placeables.forEachIndexed { i, p ->
            if (items.isNotEmpty() && x + p.width > width) {
                rows += Row(items, y, h); y += h + gap
                items = mutableListOf(); x = 0; h = 0
            }
            items += i; x += p.width + gap; h = maxOf(h, p.height)
        }
        if (items.isNotEmpty()) rows += Row(items, y, h)

        val height = rows.lastOrNull()?.let { it.y + it.height } ?: 0
        layout(width, height.coerceIn(constraints.minHeight, constraints.maxHeight)) {
            for (row in rows) {
                val rowWidth = row.items.sumOf { placeables[it].width } + gap * (row.items.size - 1)
                var px = if (centered) (width - rowWidth) / 2 else 0
                for (i in row.items) {
                    placeables[i].placeRelative(px, row.y)
                    px += placeables[i].width + gap
                }
            }
        }
    }
}

/**
 * One row whose children share the width by [weights], [spacing] apart. Two children (the pills,
 * weight = label length²) never let either drop below 40 % of the row; other counts split it in
 * proportion. With a fixed [height] every child takes it; otherwise the row is as tall as its
 * tallest child and the rest stretch to match.
 */
@Composable
fun BrickRow(weights: List<Float>, modifier: Modifier = Modifier, spacing: Dp = 10.dp, height: Dp? = null, content: @Composable () -> Unit) {
    Layout(content, modifier) { measurables, constraints ->
        val gap = spacing.roundToPx()
        val total = if (constraints.hasBoundedWidth) constraints.maxWidth else 345.dp.roundToPx()
        val widths = brickWidths(total.toFloat(), weights.take(measurables.size).ifEmpty { measurables.map { 1f } }, gap.toFloat())
            .map { it.roundToInt().coerceAtLeast(0) }
        val fixed = height?.roundToPx()
        val rowHeight = fixed ?: measurables.take(widths.size).withIndex().maxOfOrNull { (i, m) -> m.minIntrinsicHeight(widths[i]) } ?: 0
        val placeables = measurables.take(widths.size).mapIndexed { i, m -> m.measure(Constraints.fixed(widths[i], rowHeight)) }
        layout(total, rowHeight.coerceIn(constraints.minHeight, constraints.maxHeight)) {
            var x = 0
            placeables.forEachIndexed { i, p ->
                p.placeRelative(x, 0)
                x += widths[i] + gap
            }
        }
    }
}

/** Each child's width in a [BrickRow] [total] wide. */
internal fun brickWidths(total: Float, weights: List<Float>, spacing: Float): List<Float> {
    val avail = total - spacing * (weights.size - 1)
    if (weights.size == 2) {
        val minW = total * 0.4f
        val w1 = (avail * weights[0] / (weights[0] + weights[1]).coerceAtLeast(0.0001f)).coerceAtLeast(minW).coerceAtMost(avail - minW)
        return listOf(w1, avail - w1)
    }
    val sum = weights.sum().coerceAtLeast(0.0001f)
    return weights.map { avail * it / sum }
}
