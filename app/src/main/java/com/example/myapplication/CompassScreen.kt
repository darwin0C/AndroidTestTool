package com.example.myapplication

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
                        val orientation = FloatArray(3)
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                        SensorManager.getOrientation(rotationMatrix, orientation)
                        azimuth = normalizeDegrees((orientation[0] * 180f / PI.toFloat()))
                        status = "方向传感器：旋转矢量"
                    }

                    Sensor.TYPE_ACCELEROMETER -> {
                        lowPass(event.values, gravity)
                        updateOrientationFromFallback(sensorManager, gravity, geomagnetic) {
                            azimuth = it
                            status = "方向传感器：加速度计 + 磁力计"
                        }
                    }

                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        lowPass(event.values, geomagnetic)
                        updateOrientationFromFallback(sensorManager, gravity, geomagnetic) {
                            azimuth = it
                            status = "方向传感器：加速度计 + 磁力计"
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                if (sensor?.type == Sensor.TYPE_MAGNETIC_FIELD && accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
                    status = "磁力计精度较低，请水平移动手机校准"
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
            text = "指南针",
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
            CompassDial(azimuth = azimuth)
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(8.dp),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("当前方位", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "${String.format(Locale.US, "%.1f", azimuth)}°  ${directionName(azimuth)}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CompassDial(azimuth: Float) {
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val secondary = MaterialTheme.colorScheme.tertiary

    Canvas(modifier = Modifier.fillMaxSize()) {
        val radius = size.minDimension * 0.38f
        val center = Offset(size.width / 2f, size.height / 2f)
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
        }

        rotate(degrees = -azimuth, pivot = center) {
            val needle = Path().apply {
                moveTo(center.x, center.y - radius + 32f)
                lineTo(center.x - 18f, center.y + 22f)
                lineTo(center.x, center.y + 10f)
                lineTo(center.x + 18f, center.y + 22f)
                close()
            }
            drawPath(needle, secondary)
            drawLine(
                color = primary,
                start = center,
                end = Offset(center.x, center.y + radius - 40f),
                strokeWidth = 8f,
            )
        }

        drawCircle(color = primary, radius = 10f, center = center)
    }
}

private fun lowPass(source: FloatArray, target: FloatArray) {
    val alpha = 0.12f
    source.indices.take(3).forEach { index ->
        target[index] = target[index] + alpha * (source[index] - target[index])
    }
}

private fun updateOrientationFromFallback(
    sensorManager: SensorManager,
    gravity: FloatArray,
    geomagnetic: FloatArray,
    onAzimuth: (Float) -> Unit,
) {
    val rotationMatrix = FloatArray(9)
    val orientation = FloatArray(3)
    if (SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        SensorManager.getOrientation(rotationMatrix, orientation)
        onAzimuth(normalizeDegrees(orientation[0] * 180f / PI.toFloat()))
    }
}

private fun normalizeDegrees(value: Float): Float {
    var normalized = value % 360f
    if (normalized < 0f) normalized += 360f
    return normalized
}

private fun directionName(azimuth: Float): String {
    val directions = listOf("北", "东北", "东", "东南", "南", "西南", "西", "西北")
    val index = ((azimuth + 22.5f) / 45f).toInt() % directions.size
    return directions[index]
}
