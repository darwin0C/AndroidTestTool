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
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
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
    val saveableStateHolder = rememberSaveableStateHolder()

    Scaffold(
        topBar = {
            if (currentFeature == ToolFeature.Home) {
                TopAppBar(
                    title = {
                        Text("工程测算工具", maxLines = 1, overflow = TextOverflow.Ellipsis)
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
            saveableStateHolder.SaveableStateProvider(currentFeature.name) {
                when (currentFeature) {
                    ToolFeature.Home -> HomeScreen(onFeatureSelected = { currentFeature = it })
                    ToolFeature.CoordinateConvert -> CoordinateConvertScreen(onBack = { currentFeature = ToolFeature.Home })
                    ToolFeature.CoordinateMeasure -> CompassScreen(onBack = { currentFeature = ToolFeature.Home })
                    ToolFeature.MapDisplay -> MapDisplayScreen(onBack = { currentFeature = ToolFeature.Home })
                    ToolFeature.Checksum -> ChecksumScreen(onBack = { currentFeature = ToolFeature.Home })
                    ToolFeature.More -> MoreScreen(onBack = { currentFeature = ToolFeature.Home })
                }
            }
        }
    }
}

enum class ToolFeature(
    val title: String,
    val description: String,
    val mark: String,
) {
    Home("首页", "选择要使用的工具", "⌂"),
    CoordinateConvert("坐标转换", "经纬度、UTM、度分秒互转", "XY"),
    CoordinateMeasure("坐标量算", "距离、方位、高低与指南针", "△"),
    MapDisplay("地图显示", "高德地图显示当前位置", "▦"),
    Checksum("校验码计算", "XOR、SUM 校验", "#"),
    More("其他功能", "预留记录管理等工具", "+"),
}

private enum class ConvertMode(val label: String) {
    BlToUtm("经纬度 → UTM"),
    UtmToBl("UTM → 经纬度"),
    DegreeToDms("度 → 度分秒"),
    DmsToDegree("度分秒 → 度"),
}

@Composable
private fun HomeScreen(onFeatureSelected: (ToolFeature) -> Unit) {
    val features = listOf(
        ToolFeature.CoordinateConvert,
        ToolFeature.CoordinateMeasure,
        ToolFeature.MapDisplay,
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
        HomeIntro()

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

        SoftwareNote()
    }
}

@Composable
private fun HomeIntro() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "工程测算工具",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "面向工程现场使用，提供坐标转换、坐标量算、地图定位导航和校验码计算等常用功能。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun FeatureCard(feature: ToolFeature, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier
            .height(122.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp, pressedElevation = 3.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
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
private fun SoftwareNote() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(8.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "软件说明",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "本软件用于工程测算辅助，计算结果请结合现场情况确认后使用。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "使用中发现问题可联系作者王超",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun CoordinateConvertScreen(onBack: () -> Unit) {
    var mode by rememberSaveable { mutableStateOf(ConvertMode.BlToUtm) }
    var lat by rememberSaveable { mutableStateOf("39.908823") }
    var lon by rememberSaveable { mutableStateOf("116.39747") }
    var northing by rememberSaveable { mutableStateOf("") }
    var easting by rememberSaveable { mutableStateOf("") }
    var degreeInput by rememberSaveable { mutableStateOf("116.39747") }
    var dmsInput by rememberSaveable { mutableStateOf("116.2350892") }
    var hemisphere by rememberSaveable { mutableStateOf(Hemisphere.North) }
    var result by rememberSaveable { mutableStateOf("等待计算") }

    ToolPage(onBack = onBack) {
        ConvertModeGrid(
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
                ConvertHint("输入格式：纬度 B、经度 L 使用十进制度，例如 39.908823、116.39747；北纬东经为正，南纬西经为负。")
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
                ConvertHint("输入格式：北向 X 为米；东向 Y 使用带号编码值，例如 50 带 534084.68 输入 50534084.68；按实际位置选择南/北半球。")
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
                        "纬度 B：${fmt(output.latitude, 8)}°\n经度 L：${fmt(output.longitude, 8)}°\n纬度 DMS：${CoordinateMath.decimalToDmsText(output.latitude)}\n经度 DMS：${CoordinateMath.decimalToDmsText(output.longitude)}"
                    }.getOrElse { it.message ?: "输入有误" }
                }
            }

            ConvertMode.DegreeToDms -> {
                ConvertHint("输入格式：十进制度数字，例如 116.39747；输出格式为 116°23′50.892″。")
                NumberField("十进制度", degreeInput, { degreeInput = it }, "例：116.39747")
                PrimaryAction("度—>度分秒") {
                    result = runCatching {
                        "度分秒：${CoordinateMath.decimalToDmsText(degreeInput.toDouble())}"
                    }.getOrElse { it.message ?: "输入有误" }
                }
            }

            ConvertMode.DmsToDegree -> {
                ConvertHint("输入格式：按原数字形式输入度分秒，例如 116.2350892 表示 116°23′50.892″。")
                NumberField("度分秒", dmsInput, { dmsInput = it }, "例：116.2350892")
                PrimaryAction("度分秒—>度") {
                    result = runCatching {
                        "十进制度：${fmt(CoordinateMath.dmsToDecimal(dmsInput.toDouble()), 8)}°"
                    }.getOrElse { it.message ?: "输入有误" }
                }
            }
        }

        ResultPanel(result)
    }
}

@Composable
private fun ConvertHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(8.dp))
            .padding(12.dp),
    )
}

@Composable
private fun CoordinateMeasureScreen(onBack: () -> Unit) {
    var x1 by rememberSaveable { mutableStateOf("0") }
    var y1 by rememberSaveable { mutableStateOf("0") }
    var h1 by rememberSaveable { mutableStateOf("0") }
    var x2 by rememberSaveable { mutableStateOf("100") }
    var y2 by rememberSaveable { mutableStateOf("100") }
    var h2 by rememberSaveable { mutableStateOf("10") }
    var milScale by rememberSaveable { mutableStateOf(MilScale.Mil6000) }
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
        SectionTitle("密位")
        ModeChips(
            options = MilScale.entries,
            selected = milScale,
            label = { it.label },
            onSelected = { milScale = it },
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
                val azimuthMil = CoordinateMath.degreesToMils(output.azimuthDegrees, milScale)
                val elevationMil = CoordinateMath.degreesToMils(output.elevationDegrees, milScale, signed = true)
                "平距：${fmt(output.horizontalDistance)} m\n斜距：${fmt(output.spatialDistance)} m\n方位角：${fmt(output.azimuthDegrees, 4)}°\n方位密位：${fmt(azimuthMil, 1)}\n高低角：${fmt(output.elevationDegrees, 4)}°\n高低密位：${fmt(elevationMil, 1)}"
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
        AmapLocationMap(
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ChecksumScreen(onBack: () -> Unit) {
    var input by rememberSaveable { mutableStateOf("01 03 00 00 00 02") }
    var mode by rememberSaveable { mutableStateOf(ChecksumMode.Xor8) }
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
        SelectionContainer {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
private fun <T> ConvertModeGrid(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        options.chunked(2).forEach { rowItems ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                rowItems.forEach { option ->
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
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
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
