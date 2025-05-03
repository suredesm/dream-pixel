package lk.sure.dream.compose.components.canvas

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntRect

val EraseColor = Color(0).toArgb()

/** Normalizes the [IntRect]. Meaning left <= right and top <= bottom */
internal fun IntRect.normalize(): IntRect {
    var left = left
    var right = right
    var top = top
    var bottom = bottom

    if (left > right) {
        val tempLeft = left
        left = right
        right = tempLeft
    }

    if (top > bottom) {
        val tempTop = top
        top = bottom
        bottom = tempTop
    }

    return IntRect(
        left,
        top,
        right,
        bottom
    )
}

/** Normalizes the [Rect]. Meaning left <= right and top <= bottom */
internal fun Rect.normalize(): Rect {
    var left = left
    var right = right
    var top = top
    var bottom = bottom

    if (left > right) {
        val tempLeft = left
        left = right
        right = tempLeft
    }

    if (top > bottom) {
        val tempTop = top
        top = bottom
        bottom = tempTop
    }

    return Rect(
        left,
        top,
        right,
        bottom
    )
}

/**
 * Current color act as bottom color (should be fully opaque)
 * and the [other] act as top color and returns the resulting
 * color.
 */
infix fun Color.overlay(other: Color): Color {
    val topAlpha = other.alpha

    val red = red * (1F - topAlpha) + other.red * topAlpha
    val green = green * (1F - topAlpha) + other.green * topAlpha
    val blue = blue * (1F - topAlpha) + other.blue * topAlpha
    return Color(red, green, blue)
}