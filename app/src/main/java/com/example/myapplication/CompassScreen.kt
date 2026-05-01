package com.example.myapplication

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.WindowManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface as MaterialSurface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CompassScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var azimuth by remember { mutableFloatStateOf(0f) }
    var status by remember { mutableStateOf("正在读取方向传感器") }
    var sensorAccuracy by remember { mutableStateOf("精度未知") }

    var x1 by rememberSaveable { mutableStateOf("5263819") }
    var y1 by rememberSaveable { mutableStateOf("611865") }
    var h1 by rememberSaveable { mutableStateOf("160") }
    var x2 by rememberSaveable { mutableStateOf("5263537") }
    var y2 by rememberSaveable { mutableStateOf("613256") }
    var h2 by rememberSaveable { mutableStateOf("170") }
    var milScale by rememberSaveable { mutableStateOf(MilScale.Mil6000) }
    val initialTargetResult = CoordinateMath.distance(5263819.0, 611865.0, 160.0, 5263537.0, 613256.0, 170.0)
    var targetHorizontalDistance by rememberSaveable { mutableStateOf(initialTargetResult.horizontalDistance) }
    var targetSpatialDistance by rememberSaveable { mutableStateOf(initialTargetResult.spatialDistance) }
    var targetAzimuthDegrees by rememberSaveable { mutableStateOf(initialTargetResult.azimuthDegrees) }
    var targetElevationDegrees by rememberSaveable { mutableStateOf(initialTargetResult.elevationDegrees) }
    var targetText by rememberSaveable { mutableStateOf("") }
    val targetResult = DistanceResult(
        horizontalDistance = targetHorizontalDistance,
        spatialDistance = targetSpatialDistance,
        azimuthDegrees = targetAzimuthDegrees,
        elevationDegrees = targetElevationDegrees,
    )

    fun recalculateTarget() {
        runCatching {
            val calculated = CoordinateMath.distance(
                x1.toDouble(),
                y1.toDouble(),
                h1.toDouble(),
                x2.toDouble(),
                y2.toDouble(),
                h2.toDouble(),
            )
            targetHorizontalDistance = calculated.horizontalDistance
            targetSpatialDistance = calculated.spatialDistance
            targetAzimuthDegrees = calculated.azimuthDegrees
            targetElevationDegrees = calculated.elevationDegrees
            targetText = ""
        }.getOrElse {
            targetText = "两点坐标输入有误"
        }
    }

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val gravity = FloatArray(3)
        val geomagnetic = FloatArray(3)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        val rotationMatrix = FloatArray(9)
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                        azimuth = orientationDegreesForDisplay(context, rotationMatrix)
                        status = "方向传感器：旋转矢量"
                    }

                    Sensor.TYPE_ACCELEROMETER -> {
                        lowPass(event.values, gravity)
                        updateOrientationFromFallback(context, gravity, geomagnetic) {
                            azimuth = it
                            status = "方向传感器：加速度计 + 磁力计"
                        }
                    }

                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        lowPass(event.values, geomagnetic)
                        updateOrientationFromFallback(context, gravity, geomagnetic) {
                            azimuth = it
                            status = "方向传感器：加速度计 + 磁力计"
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                if (sensor?.type == Sensor.TYPE_MAGNETIC_FIELD || sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
                    sensorAccuracy = when (accuracy) {
                        SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "精度高"
                        SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "精度中"
                        SensorManager.SENSOR_STATUS_ACCURACY_LOW -> "精度低，请做 8 字校准"
                        SensorManager.SENSOR_STATUS_UNRELIABLE -> "精度不可靠，请远离磁场并校准"
                        else -> "精度未知"
                    }
                }
            }
        }

        if (rotationVector != null) {
            sensorManager.registerListener(listener, rotationVector, SensorManager.SENSOR_DELAY_UI)
        } else if (accelerometer != null && magneticField != null) {
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
            sensorManager.registerListener(listener, magneticField, SensorManager.SENSOR_DELAY_UI)
        } else {
            status = "当前设备没有可用的指南针传感器"
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    val targetAzimuth = targetResult.azimuthDegrees.toFloat()
    val relativeBearing = signedDeltaDegrees(targetAzimuth, azimuth)
    val azimuthMil = CoordinateMath.degreesToMils(targetResult.azimuthDegrees, milScale)
    val elevationMil = CoordinateMath.degreesToMils(targetResult.elevationDegrees, milScale, signed = true)
    val calculationText = buildString {
        if (targetText.isNotBlank()) append("$targetText\n")
        append("水平距离：${String.format(Locale.US, "%.3f", targetResult.horizontalDistance)} m\n")
        append("直线距离：${String.format(Locale.US, "%.3f", targetResult.spatialDistance)} m\n")
        append("方位角：${String.format(Locale.US, "%.4f", targetResult.azimuthDegrees)}°\n")
        append("方位密位：${String.format(Locale.US, "%.1f", azimuthMil)}\n")
        append("高低角：${String.format(Locale.US, "%.4f", targetResult.elevationDegrees)}°\n")
        append("高低密位：${String.format(Locale.US, "%.1f", elevationMil)}")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedButton(onClick = onBack) {
            Text("返回首页")
        }

        Text(
            text = "坐标量算",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            CompassDial(
                azimuth = azimuth,
                targetAzimuth = targetAzimuth,
            )
            MaterialSurface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .widthIn(max = 150.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                shape = RoundedCornerShape(8.dp),
                tonalElevation = 3.dp,
                shadowElevation = 3.dp,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        text = "方位 ${String.format(Locale.US, "%.2f", targetResult.azimuthDegrees)}°",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "高低 ${String.format(Locale.US, "%.2f", targetResult.elevationDegrees)}°",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        MaterialSurface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(8.dp),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("实时指向", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "${String.format(Locale.US, "%.1f", azimuth)}°  ${directionName(azimuth)}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "目标方位：${String.format(Locale.US, "%.1f", targetAzimuth)}°，相对手机正前方 ${turnText(relativeBearing)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "指南功能精度与手机传感器有关，请先使用手机自带指南针校准后再使用；使用时尽量远离磁铁、车载支架和大电流设备。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        MaterialSurface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(8.dp),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("两点量算", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("点 A")
                CompassTripleNumberRow(
                    first = CompassFieldState("X", x1) { x1 = it },
                    second = CompassFieldState("Y", y1) { y1 = it },
                    third = CompassFieldState("H", h1) { h1 = it },
                )
                Text("点 B")
                CompassTripleNumberRow(
                    first = CompassFieldState("X", x2) { x2 = it },
                    second = CompassFieldState("Y", y2) { y2 = it },
                    third = CompassFieldState("H", h2) { h2 = it },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MilScale.entries.forEach { scale ->
                        FilterChip(
                            selected = milScale == scale,
                            onClick = { milScale = scale },
                            label = { Text(scale.label) },
                        )
                    }
                }
                Button(
                    onClick = ::recalculateTarget,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("计算并显示方向")
                }
                SelectionContainer {
                    Text(
                        text = calculationText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun CompassDial(azimuth: Float, targetAzimuth: Float) {
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val targetColor = MaterialTheme.colorScheme.tertiary

    Canvas(modifier = Modifier.fillMaxSize()) {
        val radius = size.minDimension * 0.38f
        val center = Offset(size.width / 2f, size.height / 2f)
        val tickLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = onSurface.toArgb()
            textSize = 24f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        val targetLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = targetColor.toArgb()
            textSize = 23f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        drawCircle(
            color = Color.White,
            radius = radius,
            center = center,
        )
        drawCircle(
            color = primary,
            radius = radius,
            center = center,
            style = Stroke(width = 4f),
        )

        rotate(degrees = -azimuth, pivot = center) {
            repeat(36) { index ->
                val angle = Math.toRadians((index * 10 - 90).toDouble())
                val startRadius = if (index % 3 == 0) radius - 24f else radius - 12f
                val start = Offset(
                    x = center.x + cos(angle).toFloat() * startRadius,
                    y = center.y + sin(angle).toFloat() * startRadius,
                )
                val end = Offset(
                    x = center.x + cos(angle).toFloat() * radius,
                    y = center.y + sin(angle).toFloat() * radius,
                )
                drawLine(
                    color = if (index % 3 == 0) onSurface else Color(0xFF9AA6A0),
                    start = start,
                    end = end,
                    strokeWidth = if (index % 3 == 0) 3f else 1.5f,
                )
                if (index % 3 == 0) {
                    val labelRadius = radius - 42f
                    val label = (index * 10).toString()
                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        center.x + cos(angle).toFloat() * labelRadius,
                        center.y + sin(angle).toFloat() * labelRadius + 8f,
                        tickLabelPaint,
                    )
                }
            }
        }

        val relativeTarget = signedDeltaDegrees(targetAzimuth, azimuth)
        rotate(degrees = relativeTarget, pivot = center) {
            val targetPath = Path().apply {
                moveTo(center.x, center.y - radius + 20f)
                lineTo(center.x - 14f, center.y - radius + 48f)
                lineTo(center.x, center.y - radius + 39f)
                lineTo(center.x + 14f, center.y - radius + 48f)
                close()
            }
            drawPath(targetPath, targetColor)
            drawLine(
                color = targetColor,
                start = center,
                end = Offset(center.x, center.y - radius + 56f),
                strokeWidth = 5f,
            )
            drawContext.canvas.nativeCanvas.drawText(
                "${String.format(Locale.US, "%.1f", targetAzimuth)}°",
                center.x,
                center.y - radius + 76f,
                targetLabelPaint,
            )
        }

        val forwardNeedle = Path().apply {
            moveTo(center.x, center.y - radius + 58f)
            lineTo(center.x - 11f, center.y + 20f)
            lineTo(center.x, center.y + 10f)
            lineTo(center.x + 11f, center.y + 20f)
            close()
        }
        drawPath(forwardNeedle, primary)
        drawCircle(color = primary, radius = 9f, center = center)
    }
}

private data class CompassFieldState(
    val label: String,
    val value: String,
    val onValueChange: (String) -> Unit,
)

@Composable
private fun CompassTripleNumberRow(
    first: CompassFieldState,
    second: CompassFieldState,
    third: CompassFieldState,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CompassNumberField(first, Modifier.weight(1f))
        CompassNumberField(second, Modifier.weight(1f))
        CompassNumberField(third, Modifier.weight(1f))
    }
}

@Composable
private fun CompassNumberField(state: CompassFieldState, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = state.value,
        onValueChange = state.onValueChange,
        label = { Text(state.label) },
        modifier = modifier,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}

private fun lowPass(source: FloatArray, target: FloatArray) {
    val alpha = 0.12f
    source.indices.take(3).forEach { index ->
        target[index] = target[index] + alpha * (source[index] - target[index])
    }
}

private fun updateOrientationFromFallback(
    context: Context,
    gravity: FloatArray,
    geomagnetic: FloatArray,
    onAzimuth: (Float) -> Unit,
) {
    val rotationMatrix = FloatArray(9)
    if (SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
        onAzimuth(orientationDegreesForDisplay(context, rotationMatrix))
    }
}

private fun orientationDegreesForDisplay(context: Context, rotationMatrix: FloatArray): Float {
    val adjustedRotationMatrix = FloatArray(9)
    val rotation = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation
    val (xAxis, yAxis) = when (rotation) {
        Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
        Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
        Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
        else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
    }
    SensorManager.remapCoordinateSystem(rotationMatrix, xAxis, yAxis, adjustedRotationMatrix)
    val orientation = FloatArray(3)
    SensorManager.getOrientation(adjustedRotationMatrix, orientation)
    return normalizeDegrees(orientation[0] * 180f / PI.toFloat())
}

private fun normalizeDegrees(value: Float): Float {
    var normalized = value % 360f
    if (normalized < 0f) normalized += 360f
    return normalized
}

private fun signedDeltaDegrees(target: Float, current: Float): Float {
    var delta = (target - current) % 360f
    if (delta > 180f) delta -= 360f
    if (delta < -180f) delta += 360f
    return delta
}

private fun directionName(azimuth: Float): String {
    val directions = listOf("北", "东北", "东", "东南", "南", "西南", "西", "西北")
    val index = ((azimuth + 22.5f) / 45f).toInt() % directions.size
    return directions[index]
}

private fun turnText(relativeBearing: Float): String {
    val absValue = kotlin.math.abs(relativeBearing)
    return when {
        absValue < 2f -> "正前方"
        relativeBearing > 0f -> "右转 ${String.format(Locale.US, "%.1f", absValue)}°"
        else -> "左转 ${String.format(Locale.US, "%.1f", absValue)}°"
    }
}
