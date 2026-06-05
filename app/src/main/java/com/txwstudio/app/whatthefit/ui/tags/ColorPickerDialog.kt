package com.txwstudio.app.whatthefit.ui.tags

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.txwstudio.app.whatthefit.R
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private val PRESET_COLORS = listOf(
    0xFF000000L, 0xFF9E9E9EL, 0xFFFFFFFFL, 0xFF795548L, 0xFFD8C3A5L,
    0xFFE53935L, 0xFFEC407AL, 0xFF8E24AAL, 0xFF5E35B1L, 0xFF3949ABL,
    0xFF1E88E5L, 0xFF00ACC1L, 0xFF00897BL, 0xFF43A047L, 0xFFC0CA33L,
    0xFFFDD835L, 0xFFFB8C00L, 0xFFF4511EL,
)

private fun hsvToArgb(h: Float, s: Float, v: Float): Long =
    Color.hsv(h.coerceIn(0f, 360f), s.coerceIn(0f, 1f), v.coerceIn(0f, 1f)).toArgb()
        .toLong() and 0xFFFFFFFFL

private fun argbToHsv(argb: Long): FloatArray {
    val out = FloatArray(3)
    android.graphics.Color.colorToHSV((argb and 0xFFFFFFFFL).toInt(), out)
    return out
}

private fun argbToHex(argb: Long): String = "#%06X".format(argb and 0xFFFFFFL)

private fun parseHex(text: String): Long? {
    val cleaned = text.trim().removePrefix("#")
    if (cleaned.length != 6) return null
    val rgb = cleaned.toLongOrNull(16) ?: return null
    return 0xFF000000L or rgb
}

/** Add/edit dialog for a color: a name field, HSV wheel + brightness, manual HEX, and presets. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPickerDialog(
    title: String,
    initialName: String,
    initialArgb: Long,
    onConfirm: (name: String, argb: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialHsv = remember(initialArgb) { argbToHsv(initialArgb) }
    var name by remember { mutableStateOf(initialName) }
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var sat by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }
    var hexText by remember { mutableStateOf(argbToHex(initialArgb)) }

    val argb = hsvToArgb(hue, sat, value)

    fun setFromHsv(h: Float, s: Float, v: Float) {
        hue = h; sat = s; value = v
        hexText = argbToHex(hsvToArgb(h, s, v))
    }

    fun setFromArgb(a: Long, updateHex: Boolean) {
        val hsv = argbToHsv(a)
        hue = hsv[0]; sat = hsv[1]; value = hsv[2]
        if (updateHex) hexText = argbToHex(a)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.item_field_name)) },
                    modifier = Modifier.fillMaxWidth(),
                )

                ColorWheel(
                    hue = hue,
                    saturation = sat,
                    value = value,
                    onChange = { h, s -> setFromHsv(h, s, value) },
                    modifier = Modifier
                        .size(200.dp)
                        .align(Alignment.CenterHorizontally),
                )

                Slider(
                    value = value,
                    onValueChange = { setFromHsv(hue, sat, it) },
                    valueRange = 0f..1f
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(argb))
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                    )
                    OutlinedTextField(
                        value = hexText,
                        onValueChange = { input ->
                            hexText = input
                            parseHex(input)?.let { setFromArgb(it, updateHex = false) }
                        },
                        singleLine = true,
                        label = { Text("HEX") },
                        modifier = Modifier.weight(1f),
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PRESET_COLORS.forEach { preset ->
                        val selected = preset == argb
                        Box(
                            Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable { setFromArgb(preset, updateHex = true) }
                                .background(Color(preset))
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape,
                                ),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.trim(), argb) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.action_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun ColorWheel(
    hue: Float,
    saturation: Float,
    value: Float,
    onChange: (hue: Float, sat: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hueColors = remember { (0..360 step 30).map { Color.hsv(it.toFloat(), 1f, 1f) } }
    Box(
        modifier
            .pointerInput(Unit) {
                detectTapGestures { pos -> emitWheelChange(pos, size, onChange) }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { emitWheelChange(it, size, onChange) },
                ) { change, _ -> emitWheelChange(change.position, size, onChange) }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                brush = Brush.sweepGradient(hueColors, center),
                radius = radius,
                center = center
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.White, Color.Transparent),
                    center,
                    radius
                ),
                radius = radius,
                center = center,
            )
            if (value < 1f) {
                drawCircle(Color.Black.copy(alpha = 1f - value), radius = radius, center = center)
            }
            val rad = Math.toRadians(hue.toDouble())
            val tx = center.x + (cos(rad) * saturation * radius).toFloat()
            val ty = center.y + (sin(rad) * saturation * radius).toFloat()
            drawCircle(
                Color.White,
                radius = 7.dp.toPx(),
                center = Offset(tx, ty),
                style = Stroke(3.dp.toPx())
            )
            drawCircle(
                Color.Black,
                radius = 7.dp.toPx(),
                center = Offset(tx, ty),
                style = Stroke(1.dp.toPx())
            )
        }
    }
}

private fun emitWheelChange(pos: Offset, size: IntSize, onChange: (Float, Float) -> Unit) {
    val radius = minOf(size.width, size.height) / 2f
    val dx = pos.x - size.width / 2f
    val dy = pos.y - size.height / 2f
    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    if (angle < 0f) angle += 360f
    val sat = (hypot(dx, dy) / radius).coerceIn(0f, 1f)
    onChange(angle, sat)
}
