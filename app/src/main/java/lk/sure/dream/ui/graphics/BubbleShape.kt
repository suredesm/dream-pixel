package lk.sure.dream.ui.graphics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

class BubbleShape(
    private val cornerRadius: Dp,
    private val tailSize: Dp,
    private val pointTo: Offset
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path = Path().apply {
            val corner = with(density) { cornerRadius.toPx() }
            var topLeftCorner = corner
            var bottomLeftCorner = corner
            var topRightCorner = corner
            var bottomRightCorner = corner

            val tail = with(density) { tailSize.toPx() }

            val leftTail = if (pointTo.x - tail < 0F) pointTo.x
            else tail

            val rightTail = if (pointTo.x + tail >= size.width) size.width - pointTo.x
            else tail

            val shouldPointFromTop = if (pointTo.x in -rightTail ..< size.width + leftTail) {
                pointTo.y < size.height * .5F
            } else {
                null
            }

            when (shouldPointFromTop) {
                true -> if (pointTo.x - leftTail in 0F ..< corner)
                    topLeftCorner = pointTo.x - leftTail
                else if (pointTo.x - size.width + tail in -corner ..< 0F) {
                    topRightCorner -= size.width - pointTo.x - tail
                }
                false -> if (pointTo.x in 0F ..< corner)
                    bottomLeftCorner -= pointTo.x
                else if (pointTo.x - size.width in -corner ..< 0F) {
                    bottomRightCorner += size.width - pointTo.x
                }
                null -> {}
            }

            moveTo(0F, topLeftCorner)
            arcTo(
                rect = Rect(0F, 0F, 2 * topLeftCorner, 2 * topLeftCorner),
                startAngleDegrees = 180F,
                sweepAngleDegrees = 90F,
                forceMoveTo = false
            )

            if (shouldPointFromTop != null && shouldPointFromTop) {
                lineTo(pointTo.x.coerceIn(0F, size.width) - leftTail, 0F)
                relativeLineTo(leftTail, -tail)
                relativeLineTo(rightTail, tail)
                lineTo(pointTo.x.coerceIn(0F, size.width) + rightTail, 0F)
            }

            lineTo(size.width - topRightCorner, 0F)
            arcTo(
                rect = Rect(Offset(size.width - 2 * topRightCorner, 0F), Size(2 * topRightCorner, 2 * topRightCorner)),
                startAngleDegrees = 270F,
                sweepAngleDegrees = 90F,
                forceMoveTo = false
            )
            lineTo(size.width, size.height - bottomRightCorner)
            arcTo(
                rect = Rect(
                    Offset(size.width - 2 * bottomRightCorner, size.height - 2 * bottomRightCorner),
                    Size(2 * bottomRightCorner, 2 * bottomRightCorner)
                ),
                startAngleDegrees = 0F,
                sweepAngleDegrees = 90F,
                forceMoveTo = false
            )

            if (shouldPointFromTop != null && !shouldPointFromTop) {
                lineTo(pointTo.x + tail, size.height)
                relativeLineTo(-tail, tail)
                relativeLineTo(-tail, -tail)
                lineTo(pointTo.x - tail, size.height)
            }

            lineTo(bottomRightCorner, size.height)
            arcTo(
                rect = Rect(
                    Offset(0F, size.height - 2 * bottomRightCorner),
                    Size(2 * bottomRightCorner, 2 * bottomRightCorner)
                ),
                startAngleDegrees = 90F,
                sweepAngleDegrees = 90F,
                forceMoveTo = false
            )
            close()
        }

        return Outline.Generic(path)
    }

}