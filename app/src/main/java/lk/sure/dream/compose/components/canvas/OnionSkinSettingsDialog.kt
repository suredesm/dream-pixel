package lk.sure.dream.compose.components.canvas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun OnionSkinSettingsDialog(
    onionSkinSettings: OnionSkinSettings,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismissRequest
            ) { Text("Cancel") }
        },
        title = { Text("Onion Skinning") },
        text = { Content(onionSkinSettings) },
        modifier = modifier,
    )
}

@Composable
private fun Content(settings: OnionSkinSettings) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Enable", style = MaterialTheme.typography.labelMedium)

            Switch(
                checked = settings.enabled,
                onCheckedChange = { settings.enabled = it }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Wrap Frames", style = MaterialTheme.typography.labelMedium)

            Switch(
                checked = settings.isSkinWrapped,
                onCheckedChange = { settings.isSkinWrapped = it }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Use Colors", style = MaterialTheme.typography.labelMedium)

            Switch(
                checked = settings.useTrueColors,
                onCheckedChange = { settings.useTrueColors = it }
            )
        }

        NumberedSlider(
            value = settings.backwardSkinCount,
            onValueChange = settings::changeBackwardSkinCount,
            valueRange = 0..3,
            label = { Text("Backward Frames", style = MaterialTheme.typography.labelMedium) }
        )


        NumberedSlider(
            value = settings.forwardSkinCount,
            onValueChange = settings::changeForwardSkinCount,
            valueRange = 0..3,
            label = { Text("Forward Frames", style = MaterialTheme.typography.labelMedium) }
        )
    }
}

@Composable
fun NumberedSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    label: (@Composable () -> Unit)? = null,
    valueRange: IntRange = 0..10,
) {
    var v by remember { mutableFloatStateOf(value.toFloat()) }

    LaunchedEffect(v.roundToInt()) {
        onValueChange(v.roundToInt())
    }

    Column {
        Row(
            modifier = Modifier.height(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            label?.let { it() }
        }

        Slider(
            value = v,
            onValueChange = { v = it },
            modifier = modifier,
            valueRange = with(valueRange) {
                start.toFloat()..endInclusive.toFloat()
            },
            steps = with(valueRange) {
                (endInclusive.toFloat() - start.toFloat()).toInt() - 1
            },
        )

        Row(
            modifier.fillMaxWidth(),
            Arrangement.SpaceBetween,
        ) {
            valueRange.forEach {
                Text("$it", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}