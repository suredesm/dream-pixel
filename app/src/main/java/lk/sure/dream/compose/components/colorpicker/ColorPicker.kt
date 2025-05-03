package lk.sure.dream.compose.components.colorpicker

import android.graphics.Color.colorToHSV
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

@Composable
fun ColorPickerDialog(
    color: Color,
    onColorChange: (Color) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onClearAllColors: () -> Unit,
    recentColors: List<Color>,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Pick")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text("Pick a color") },
        text = {
            Column {
                ColorPicker(
                    color,
                    onColorChange,
                    recentColors,
                )

                Spacer(Modifier.height(4.dp))

                OutlinedIconButton(
                    onClick = onClearAllColors
                ) {
                    Icon(Icons.Default.ClearAll, "Clear")
                }
            }
        },
    )
}

@Composable
fun ColorPicker(
    color: Color,
    onColorChange: (Color) -> Unit,
    colorList: List<Color>,
    modifier: Modifier = Modifier,
    onColorLongClick: ((Color) -> Unit)? = null,
) {
    val density = LocalDensity.current

    val hsv = remember(color) {
        FloatArray(3).apply { colorToHSV(color.toArgb(), this) }
    }

    var hue by remember { mutableFloatStateOf(hsv[0]) }
    var saturation by remember { mutableFloatStateOf(hsv[1]) }
    var value by remember { mutableFloatStateOf(hsv[2]) }

    var hueText by remember { mutableStateOf(hsv[0].toInt().toString()) }
    var saturationText by remember { mutableStateOf((hsv[1] * 100).toInt().toString()) }
    var valueText by remember { mutableStateOf((hsv[2] * 100).toInt().toString()) }

    var svSize by remember {
        mutableStateOf(IntSize.Zero)
    }

    var colorHex by remember { mutableStateOf(color.toHex()) }

    val prevColorHex by remember { mutableStateOf(color.toHex()) }

    var selectedIndex by remember(color) { mutableStateOf<Int?>(colorList.indexOf(color)) }

    fun onHexFieldChange(hex: String, shouldChangeHex: Boolean) {
        if (shouldChangeHex) colorHex = hex

        val colorInt = "FF$hex".toLong(16).toInt()
        val (h, s, v) = FloatArray(3).apply { colorToHSV(colorInt, this) }
        val c = Color(colorInt)
        hue = h
        saturation = s
        value = v

        hueText = h.toInt().toString()
        saturationText = (s * 100).toInt().toString()
        valueText = (v * 100).toInt().toString()
        onColorChange(c)
        selectedIndex = colorList.indexOf(c).takeIf { it != 1 }
    }

    fun onSnVChange(s: Float, v: Float) {
        val c = Color.hsv(hue, s, v)

        saturation = s
        value = v

        saturationText = (s * 100).toInt().toString()
        valueText = (v * 100).toInt().toString()

        colorHex = c.toHex()
        onColorChange(c)
        selectedIndex = colorList.indexOf(c).takeIf { it != 1 }
    }

    fun onHueChange(h: Float) {
        val c = Color.hsv(h, saturation, value)

        hue = h
        hueText = h.toInt().toString()
        colorHex = c.toHex()
        onColorChange(c)
        selectedIndex = colorList.indexOf(c).takeIf { it != 1 }
    }

    fun onHueTextChange(hText: String, shouldHueTextChange: Boolean) {
        if (shouldHueTextChange) hueText = hText.toIntOrNull()?.toString() ?: "0"

        val h = hText.toInt().toFloat()
        val c = Color.hsv(h, saturation, value)

        hue = h
        colorHex = c.toHex()
        onColorChange(c)
        selectedIndex = colorList.indexOf(c).takeIf { it != 1 }
    }

    fun onSaturationTextChange(sText: String, shouldSaturationTextChange: Boolean) {
        if (shouldSaturationTextChange) saturationText = sText.toIntOrNull()?.toString() ?: "0"

        val s = sText.toInt().toFloat() / 100
        val c = Color.hsv(hue, s, value)

        saturation = s
        colorHex = c.toHex()
        onColorChange(c)
        selectedIndex = colorList.indexOf(c).takeIf { it != 1 }
    }

    fun onValueTextChange(vText: String, shouldValueTextChange: Boolean) {
        if (shouldValueTextChange) valueText = vText.toIntOrNull()?.toString() ?: "0"

        val v = vText.toInt().toFloat() / 100
        val c = Color.hsv(hue, saturation, v)

        value = v
        colorHex = c.toHex()
        onColorChange(c)
        selectedIndex = colorList.indexOf(c).takeIf { it != 1 }
    }

    val radius = 12.dp
    val radiusPx = with(density) { radius.roundToPx() }

    Column(
        modifier = modifier
    ) {
        Row(
            Modifier
                .height(IntrinsicSize.Min)
                .fillMaxWidth()
        ) {
            Box(
                Modifier
                    .background(color)
                    .weight(1F)
                    .fillMaxHeight()
            )

            OutlinedTextField(
                value = colorHex,
                onValueChange = { newValue: String ->
                    if (
                        newValue.length <= 6 &&
                        newValue
                            .uppercase()
                            .all { it.isDigit() || it in 'A'..'F' }
                    ) {
                        handleColorText(
                            colorHex = newValue,
                            onSuccess = { _, _, _, hex ->
                                colorHex = newValue
                                onHexFieldChange(hex, false)
                            },
                            onFailure = {
                                if (newValue.isBlank()) {
                                    colorHex = ""
                                }
                            }
                        )
                    }
                },
                modifier = Modifier
                    .weight(1F)
                    .onFocusChanged {
                        if (!it.isFocused) {
                            handleColorText(
                                colorHex = colorHex,
                                onSuccess = { _, _, _, hex ->
                                    onHexFieldChange(hex, true)
                                },
                                onFailure = { colorHex = prevColorHex }
                            )
                        }
                    },
                prefix = { Text("#") },
                singleLine = true,
                keyboardActions = KeyboardActions(onDone = {
                    handleColorText(
                        colorHex = colorHex,
                        onSuccess = { _, _, _, hex ->
                            onHexFieldChange(hex, true)
                        },
                        onFailure = { colorHex = prevColorHex }
                    )
                }),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
            )
        }

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.5F)
                .background(
                    brush = Brush.horizontalGradient(
                        listOf(Color.White, Color.hsv(hue, 1F, 1F))
                    )
                )
                .drawWithContent {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black)
                        )
                    )
                    drawContent()
                }
                .onSizeChanged { svSize = it }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val posX = (saturation * svSize.width) + dragAmount.x
                        val posY = ((1f - value) * svSize.height) + dragAmount.y

                        val s = (posX / svSize.width).coerceIn(0f, 1f)
                        val v = (1f - (posY / svSize.height)).coerceIn(0f, 1f)
                        onSnVChange(s, v)
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (saturation * svSize.width).toInt() - radiusPx,
                            ((1f - value) * svSize.height).toInt() - radiusPx
                        )
                    }
                    .size(radius * 2)
                    .border(2.dp, Color.White, CircleShape)
                    .background(Color.hsv(hue, saturation, value), CircleShape)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val posX = (saturation * svSize.width) + dragAmount.x
                            val posY = ((1f - value) * svSize.height) + dragAmount.y

                            val s = (posX / svSize.width).coerceIn(0f, 1f)
                            val v = (1f - (posY / svSize.height)).coerceIn(0f, 1f)
                            onSnVChange(s, v)
                        }
                    }
            )
        }

        var size by remember {
            mutableStateOf(IntSize.Zero)
        }

        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth(),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(
                        Brush.horizontalGradient((0..360).map {
                            Color.hsv(it.toFloat(), 1F, 1F)
                        }
                        ), RoundedCornerShape(size.height.toFloat())
                    )
                    .onSizeChanged { size = it }
            )

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (hue * size.width / 360).toInt() - radiusPx, 0
                        )
                    }
                    .size(radius * 2)
                    .border(2.dp, Color.White, CircleShape)
                    .background(Color.hsv(hue, 1F, 1F), CircleShape)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val posX = (hue * size.width / 360) + dragAmount.x

                            val h = (posX / size.width * 360F).coerceIn(0F, 360F)
                            onHueChange(h)
                        }
                    }
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            OutlinedTextField(
                value = hueText,
                onValueChange = {
                    val v = it.ifEmpty { "0" }.toIntOrNull() ?: return@OutlinedTextField
                    if (v !in 0..360) return@OutlinedTextField
                    hueText = it
                    onHueTextChange(v.toString(), false)
                },
                modifier = Modifier
                    .weight(1F)
                    .onFocusChanged {
                        if (it.isFocused) return@onFocusChanged
                        val v = hueText.toIntOrNull() ?: return@onFocusChanged
                        if (v !in 0..360) return@onFocusChanged
                        onHueTextChange(hueText, true)
                    },
                label = { Text("Hue") },
                textStyle = TextStyle(textAlign = TextAlign.End),
                suffix = { Text("°") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                keyboardActions = KeyboardActions(onDone = {
                    val v = hueText.toIntOrNull() ?: return@KeyboardActions
                    if (v !in 0..360) return@KeyboardActions
                    onHueTextChange(hueText, true)
                })
            )

            OutlinedTextField(
                value = saturationText,
                onValueChange = {
                    val v = it.ifEmpty { "0" }.toIntOrNull() ?: return@OutlinedTextField
                    if (v !in 0..100) return@OutlinedTextField
                    saturationText = it
                    onSaturationTextChange(v.toString(), false)
                },
                modifier = Modifier
                    .weight(1F)
                    .onFocusChanged {
                        if (it.isFocused) return@onFocusChanged
                        val v = saturationText.toIntOrNull() ?: return@onFocusChanged
                        if (v !in 0..100) return@onFocusChanged
                        onSaturationTextChange(saturationText, true)
                    },
                keyboardActions = KeyboardActions(onDone = {
                    val v = saturationText.toIntOrNull() ?: return@KeyboardActions
                    if (v !in 0..100) return@KeyboardActions
                    onHueTextChange(saturationText, true)
                }),
                label = { Text("Sat") },
                textStyle = TextStyle(textAlign = TextAlign.End),
                suffix = { Text("%") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )

            OutlinedTextField(
                value = valueText,
                onValueChange = {
                    val v = it.ifEmpty { "0" }.toIntOrNull() ?: return@OutlinedTextField
                    if (v !in 0..100) return@OutlinedTextField
                    valueText = it
                    onValueTextChange(v.toString(), false)
                },
                modifier = Modifier
                    .weight(1F)
                    .onFocusChanged {
                        if (it.isFocused) return@onFocusChanged
                        val v = valueText.toIntOrNull() ?: return@onFocusChanged
                        if (v !in 0..100) return@onFocusChanged
                        onValueTextChange(valueText, true)
                    },
                keyboardActions = KeyboardActions(onDone = {
                    val v = valueText.toIntOrNull() ?: return@KeyboardActions
                    if (v !in 0..100) return@KeyboardActions
                    onHueTextChange(valueText, true)
                }),
                label = { Text("Value") },
                textStyle = TextStyle(textAlign = TextAlign.End),
                suffix = { Text("%") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        Spacer(Modifier.height(16.dp))

        ColorGrid(
            colors = colorList,
            selectedIndex = selectedIndex,
            onSelectedIndexChange = {
                selectedIndex = it
                onHexFieldChange(colorList[it].toHex(), true)
            },
            state = rememberColorGridState(true),
            onColorLongClick = onColorLongClick,
        )
    }
}

@OptIn(ExperimentalStdlibApi::class)
fun Color.toHex(): String {
    return toArgb().toHexString(HexFormat.UpperCase).substring(2)
}

private fun handleColorText(
    colorHex: String,
    onSuccess: (hue: Float, saturation: Float, value: Float, colorHex: String) -> Unit,
    onFailure: () -> Unit,
) {
    val hex = when (colorHex.length) {
        1 -> colorHex.repeat(6)
        2 -> colorHex.repeat(3)
        3 -> buildString {
            colorHex.forEach { append(it); append(it); }
        }

        4, 5, 6 -> colorHex + "0".repeat(6 - colorHex.length)
        else -> return onFailure()
    }


    try {
        val colorInt = "FF$hex".toLong(radix = 16).toInt()
        val color = FloatArray(3).apply { colorToHSV(colorInt, this) }

        onSuccess(color[0], color[1], color[2], Color(colorInt).toHex())
    } catch (_: Exception) {
        onFailure()
    }
}