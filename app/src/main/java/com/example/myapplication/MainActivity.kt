package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.MyApplicationTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(dynamicColor = false) {
                ToolBoxApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolBoxApp() {
    var currentFeature by rememberSaveable { mutableStateOf(ToolFeature.Home) }

    Scaffold(
        topBar = {
            if (currentFeature == ToolFeature.Home) {
                TopAppBar(
                    title = {
                        Column {
                            Text("工程测算工具", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                text = currentFeature.title,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            }
        },
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background,
        ) {
            when (currentFeature) {
                ToolFeature.Home -> HomeScreen(onFeatureSelected = { currentFeature = it })
                ToolFeature.CoordinateConvert -> CoordinateConvertScreen(onBack = { currentFeature = ToolFeature.Home })
                ToolFeature.CoordinateMeasure -> CoordinateMeasureScreen(onBack = { currentFeature = ToolFeature.Home })
                ToolFeature.MapDisplay -> MapDisplayScreen(onBack = { currentFeature = ToolFeature.Home })
                ToolFeature.Compass -> CompassScreen(onBack = { currentFeature = ToolFeature.Home })
                ToolFeature.Checksum -> ChecksumScreen(onBack = { currentFeature = ToolFeature.Home })
                ToolFeature.More -> MoreScreen(onBack = { currentFeature = ToolFeature.Home })
            }
        }
    }
}

enum class ToolFeature(
    val title: String,
    val description: String,
    val mark: String,
) {
    Home("功能选择", "选择要使用的工具", "⌂"),
    CoordinateConvert("坐标转换", "经纬度、UTM、度分秒互转", "XY"),
    CoordinateMeasure("坐标量算", "两点距离、方位角、高差量算", "△"),
    MapDisplay("地图显示", "高德地图显示当前位置", "▦"),
    Compass("指南针", "读取手机方向传感器", "N"),
    Checksum("校验码计算", "XOR、SUM、CRC16 校验", "#"),
    More("其他功能", "预留数据导入、记录管理等工具", "+"),
}

private enum class ConvertMode(val label: String) {
    BlToUtm("经纬度 → UTM"),
    UtmToBl("UTM → 经纬度"),
    Dms("度 / 度分秒"),
}

@Composable
private fun HomeScreen(onFeatureSelected: (ToolFeature) -> Unit) {
    val features = listOf(
        ToolFeature.CoordinateConvert,
        ToolFeature.CoordinateMeasure,
        ToolFeature.MapDisplay,
        ToolFeature.Compass,
        ToolFeature.Checksum,
        ToolFeature.More,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = "常用工具",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "第一版按“计算核心 + 功能页面 + 后续地图接入”的结构搭建，坐标算法先从 qmycoorconverse.cpp 中迁移可独立验证的部分。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        features.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowItems.forEach { feature ->
                    FeatureCard(
                        feature = feature,
                        modifier = Modifier.weight(1f),
                        onClick = { onFeatureSelected(feature) },
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        SectionTitle("软件架构")
        InfoLine("表现层", "Compose 页面负责表单、结果展示、功能导航。")
        InfoLine("计算层", "CoordinateMath 承接坐标转换、量算、校验码等纯计算。")
        InfoLine("扩展层", "地图 SDK、文件导入、历史记录后续独立接入，避免影响计算核心。")
    }
}

@Composable
private fun FeatureCard(feature: ToolFeature, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier
            .height(132.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = feature.mark,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column {
                Text(feature.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    text = feature.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun CoordinateConvertScreen(onBack: () -> Unit) {
    var mode by rememberSaveable { mutableStateOf(ConvertMode.BlToUtm) }
    var lat by rememberSaveable { mutableStateOf("39.908823") }
    var lon by rememberSaveable { mutableStateOf("116.39747") }
    var northing by rememberSaveable { mutableStateOf("") }
    var easting by rememberSaveable { mutableStateOf("") }
    var dms by rememberSaveable { mutableStateOf("116.235831") }
    var hemisphere by rememberSaveable { mutableStateOf(Hemisphere.North) }
    var result by rememberSaveable { mutableStateOf("等待计算") }

    ToolPage(onBack = onBack) {
        ModeChips(
            options = ConvertMode.entries,
            selected = mode,
            label = { it.label },
            onSelected = {
                mode = it
                result = "等待计算"
            },
        )

        when (mode) {
            ConvertMode.BlToUtm -> {
                NumberField("纬度 B", lat, { lat = it }, "例：39.908823")
                NumberField("经度 L", lon, { lon = it }, "例：116.39747")
                PrimaryAction("计算投影坐标") {
                    result = runCatching {
                        val output = CoordinateMath.latLonToUtm(lat.toDouble(), lon.toDouble())
                        val band = CoordinateMath.latitudeBand(lat.toDouble()) ?: '-'
                        "北向 X：${fmt(output.northing)} m\n东向 Y：${fmt(output.easting)} m\n带号：${output.zone}$band\n半球：${output.hemisphere.label}"
                    }.getOrElse { it.message ?: "输入有误" }
                }
            }

            ConvertMode.UtmToBl -> {
                NumberField("北向 X", northing, { northing = it }, "例：4429529.03")
                NumberField("东向 Y", easting, { easting = it }, "例：50534084.68")
                ModeChips(
                    options = Hemisphere.entries,
                    selected = hemisphere,
                    label = { it.label },
                    onSelected = { hemisphere = it },
                )
                PrimaryAction("反算经纬度") {
                    result = runCatching {
                        val output = CoordinateMath.utmToLatLon(northing.toDouble(), easting.toDouble(), hemisphere)
                        "纬度 B：${fmt(output.latitude, 8)}°\n经度 L：${fmt(output.longitude, 8)}°\n纬度 DMS：${fmt(CoordinateMath.decimalToDms(output.latitude), 6)}\n经度 DMS：${fmt(CoordinateMath.decimalToDms(output.longitude), 6)}"
                    }.getOrElse { it.message ?: "输入有误" }
                }
            }

            ConvertMode.Dms -> {
                NumberField("角度值", dms, { dms = it }, "十进制度或 D.MMSS")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            result = runCatching {
                                "度分秒：${fmt(CoordinateMath.decimalToDms(dms.toDouble()), 6)}"
                            }.getOrElse { "输入有误" }
                        },
                    ) { Text("转 DMS") }
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            result = runCatching {
                                "十进制度：${fmt(CoordinateMath.dmsToDecimal(dms.toDouble()), 8)}°"
                            }.getOrElse { "输入有误" }
                        },
                    ) { Text("转度") }
                }
            }
        }

        ResultPanel(result)
    }
}

@Composable
private fun CoordinateMeasureScreen(onBack: () -> Unit) {
    var x1 by rememberSaveable { mutableStateOf("0") }
    var y1 by rememberSaveable { mutableStateOf("0") }
    var h1 by rememberSaveable { mutableStateOf("0") }
    var x2 by rememberSaveable { mutableStateOf("100") }
    var y2 by rememberSaveable { mutableStateOf("100") }
    var h2 by rememberSaveable { mutableStateOf("10") }
    var result by rememberSaveable { mutableStateOf("等待计算") }

    ToolPage(onBack = onBack) {
        SectionTitle("点 A")
        TripleNumberRow(
            first = FieldState("X", x1) { x1 = it },
            second = FieldState("Y", y1) { y1 = it },
            third = FieldState("H", h1) { h1 = it },
        )
        SectionTitle("点 B")
        TripleNumberRow(
            first = FieldState("X", x2) { x2 = it },
            second = FieldState("Y", y2) { y2 = it },
            third = FieldState("H", h2) { h2 = it },
        )
        PrimaryAction("量算") {
            result = runCatching {
                val output = CoordinateMath.distance(
                    x1.toDouble(),
                    y1.toDouble(),
                    h1.toDouble(),
                    x2.toDouble(),
                    y2.toDouble(),
                    h2.toDouble(),
                )
                "平距：${fmt(output.horizontalDistance)} m\n斜距：${fmt(output.spatialDistance)} m\n方位角：${fmt(output.azimuthDegrees, 4)}°\n高低角：${fmt(output.elevationDegrees, 4)}°"
            }.getOrElse { "输入有误" }
        }
        ResultPanel(result)
    }
}

@Composable
private fun MapDisplayScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedButton(onClick = onBack) {
            Text("返回首页")
        }
        AmapLocationMap()
    }
}

@Composable
private fun ChecksumScreen(onBack: () -> Unit) {
    var input by rememberSaveable { mutableStateOf("01 03 00 00 00 02") }
    var mode by rememberSaveable { mutableStateOf(ChecksumMode.Crc16Modbus) }
    var result by rememberSaveable { mutableStateOf("等待计算") }

    ToolPage(onBack = onBack) {
        ModeChips(
            options = ChecksumMode.entries,
            selected = mode,
            label = { it.label },
            onSelected = { mode = it },
        )
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("十六进制数据") },
            placeholder = { Text("例：01 03 00 00 00 02") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
        )
        PrimaryAction("计算校验码") {
            result = runCatching {
                "${mode.label}：${CoordinateMath.checksum(input, mode)}"
            }.getOrElse { it.message ?: "输入有误" }
        }
        ResultPanel(result)
    }
}

@Composable
private fun MoreScreen(onBack: () -> Unit) {
    ToolPage(onBack = onBack) {
        SectionTitle("预留能力")
        InfoLine("数据导入", "CSV、TXT、NMEA 坐标批量导入。")
        InfoLine("历史记录", "保存每次计算输入与结果，支持复制和复查。")
        InfoLine("参数管理", "椭球、投影方式、南北半球、中央经线配置。")
        InfoLine("设备数据", "后续可接串口、蓝牙或网络协议数据解析。")
    }
}

@Composable
private fun ToolPage(onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
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
        content()
    }
}

private data class FieldState(
    val label: String,
    val value: String,
    val onValueChange: (String) -> Unit,
)

@Composable
private fun TripleNumberRow(first: FieldState, second: FieldState, third: FieldState) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CompactNumberField(first, Modifier.weight(1f))
        CompactNumberField(second, Modifier.weight(1f))
        CompactNumberField(third, Modifier.weight(1f))
    }
}

@Composable
private fun CompactNumberField(state: FieldState, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = state.value,
        onValueChange = state.onValueChange,
        label = { Text(state.label) },
        modifier = modifier,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}

@Composable
private fun NumberField(label: String, value: String, onValueChange: (String) -> Unit, placeholder: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}

@Composable
private fun PrimaryAction(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(label)
    }
}

@Composable
private fun ResultPanel(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(8.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("结果", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun InfoLine(title: String, body: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = title,
            modifier = Modifier.width(82.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> ModeChips(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelected(option) },
                label = {
                    Text(
                        text = label(option),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        }
    }
}

private fun fmt(value: Double, digits: Int = 3): String {
    return String.format(Locale.US, "%.${digits}f", value)
}

@Preview(showBackground = true)
@Composable
private fun ToolBoxPreview() {
    MyApplicationTheme(dynamicColor = false) {
        ToolBoxApp()
    }
}
