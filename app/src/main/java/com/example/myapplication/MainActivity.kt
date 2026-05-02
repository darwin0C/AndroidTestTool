package com.example.myapplication

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.MyApplicationTheme
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

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
    val context = LocalContext.current
    var currentFeature by rememberSaveable { mutableStateOf(ToolFeature.Home) }
    val saveableStateHolder = rememberSaveableStateHolder()
    var hasStartupLocationPermission by rememberSaveable {
        mutableStateOf(isLocationPermissionGranted(context))
    }
    var showLocationPermissionPrompt by rememberSaveable {
        mutableStateOf(!hasStartupLocationPermission)
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        hasStartupLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            isLocationPermissionGranted(context)
        showLocationPermissionPrompt = false
    }

    Scaffold { innerPadding ->
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
                    ToolFeature.Intersection -> IntersectionScreen(onBack = { currentFeature = ToolFeature.Home })
                    ToolFeature.MapDisplay -> MapDisplayScreen(onBack = { currentFeature = ToolFeature.Home })
                    ToolFeature.ProtocolTools -> ProtocolToolsScreen(onBack = { currentFeature = ToolFeature.Home })
                    ToolFeature.History -> HistoryScreen(onBack = { currentFeature = ToolFeature.Home })
                    ToolFeature.SelfTest -> SelfTestScreen(onBack = { currentFeature = ToolFeature.Home })
                    ToolFeature.Settings -> SettingsScreen(onBack = { currentFeature = ToolFeature.Home })
                }
            }
        }
    }

    if (showLocationPermissionPrompt) {
        AlertDialog(
            onDismissRequest = { showLocationPermissionPrompt = false },
            title = { Text("需要定位权限") },
            text = { Text("地图显示和回到当前位置功能需要使用手机定位权限。请授权定位权限后使用相关功能。") },
            confirmButton = {
                Button(
                    onClick = {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                            ),
                        )
                    },
                ) {
                    Text("获取权限")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLocationPermissionPrompt = false }) {
                    Text("稍后")
                }
            },
        )
    }
}

private fun isLocationPermissionGranted(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
}

enum class ToolFeature(
    val title: String,
    val description: String,
    val mark: String,
) {
    Home("首页", "选择要使用的工具", "⌂"),
    CoordinateConvert("坐标转换", "经纬度、UTM、度分秒互转", "XY"),
    CoordinateMeasure("坐标量算", "距离、方位、高低与指南针", "△"),
    Intersection("交会计算", "两点方位交会目标坐标", "∩"),
    MapDisplay("地图显示", "高德地图显示当前位置", "▦"),
    ProtocolTools("协议与校验", "EB 报文生成、XOR/SUM 校验", "EB"),
    History("历史记录", "保存转换、量算和协议结果", "H"),
    SelfTest("计算自检", "内置测试样例核对结果", "T"),
    Settings("参数设置", "投影和默认项", "P"),
}

private enum class ConvertMode(val label: String) {
    BlToUtm("经纬度 → UTM"),
    UtmToBl("UTM → 经纬度"),
    DegreeToDms("度 → 度分秒"),
    DmsToDegree("度分秒 → 度"),
    UtmCrossZone("UTM 跨带"),
}

private enum class HistoryExportFormat(val label: String, val extension: String, val mimeType: String) {
    Txt("TXT", "txt", "text/plain"),
    Csv("CSV", "csv", "text/csv"),
}

private enum class ProtocolToolMode(val label: String) {
    Checksum("校验码计算"),
    Protocol("协议生成"),
}

private data class SelfTestItem(
    val title: String,
    val actual: String,
    val expected: String,
    val passed: Boolean,
)

@Composable
private fun HomeScreen(onFeatureSelected: (ToolFeature) -> Unit) {
    val features = listOf(
        ToolFeature.CoordinateConvert,
        ToolFeature.CoordinateMeasure,
        ToolFeature.Intersection,
        ToolFeature.MapDisplay,
        ToolFeature.ProtocolTools,
        ToolFeature.History,
        ToolFeature.SelfTest,
        ToolFeature.Settings,
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
                text = "工程量算助手",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "面向工程现场使用，提供坐标转换、坐标量算、交会计算、地图定位导航、协议生成和校验码计算等常用功能。",
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
            text = "本软件用于工程测算辅助，内置计算公式均以通过测试数据进行了自检，计算结果请结合现场情况确认后使用。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "使用中发现问题或有好的建议均可联系作者王超",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun CoordinateConvertScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { SettingsStore.load(context) }
    val ellipsoid = settings.ellipsoidParameters()
    var mode by rememberSaveable { mutableStateOf(ConvertMode.BlToUtm) }
    var lat by rememberSaveable { mutableStateOf("37.804931") }
    var lon by rememberSaveable { mutableStateOf("112.569555") }
    var northing by rememberSaveable { mutableStateOf("4185332") }
    var easting by rememberSaveable {
        mutableStateOf(if (settings.utmInputMode == UtmInputMode.SplitZone) "638171" else "49638171")
    }
    var sourceZone by rememberSaveable { mutableStateOf("49") }
    var baseZone by rememberSaveable { mutableStateOf("50") }
    var degreeInput by rememberSaveable { mutableStateOf("112.569555") }
    var dmsInput by rememberSaveable { mutableStateOf("112.341040") }
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
                        val output = CoordinateMath.latLonToUtm(lat.toDouble(), lon.toDouble(), ellipsoid = ellipsoid)
                        val band = CoordinateMath.latitudeBand(lat.toDouble()) ?: '-'
                        val text = "北向 X：${fmt(output.northing)} m\n东向 Y：${fmt(output.easting)} m\n带号：${output.zone}$band\n半球：${output.hemisphere.label}"
                        val input = "纬度 B：$lat\n经度 L：$lon\n椭球：${settings.ellipsoidPreset.label}"
                        HistoryStore.add(context, "坐标转换", mode.label, input, text)
                        text
                    }.getOrElse { it.message ?: "输入有误" }
                }
            }

            ConvertMode.UtmToBl -> {
                ConvertHint(
                    if (settings.utmInputMode == UtmInputMode.SplitZone) {
                        "输入格式：北向 X 为米；带号单独输入；东向 Y 输入带内东向值，例如 50 带 534084.68 分别输入 50 和 534084.68。"
                    } else {
                        "输入格式：北向 X 为米；东向 Y 使用带号编码值，例如 50 带 534084.68 输入 50534084.68。"
                    } + "按实际位置选择南/北半球。",
                )
                NumberField("北向 X", northing, { northing = it }, "例：4429529.03")
                if (settings.utmInputMode == UtmInputMode.SplitZone) {
                    NumberField("带号", sourceZone, { sourceZone = it }, "例：50")
                }
                NumberField(
                    "东向 Y",
                    easting,
                    { easting = it },
                    if (settings.utmInputMode == UtmInputMode.SplitZone) "例：534084.68" else "例：50534084.68",
                )
                ModeChips(
                    options = Hemisphere.entries,
                    selected = hemisphere,
                    label = { it.label },
                    onSelected = { hemisphere = it },
                )
                PrimaryAction("反算经纬度") {
                    result = runCatching {
                        val encodedEasting = encodedUtmEasting(easting, sourceZone, settings.utmInputMode)
                        val output = CoordinateMath.utmToLatLon(northing.toDouble(), encodedEasting, hemisphere, ellipsoid)
                        val text = "纬度 B：${fmt(output.latitude, 8)}°\n经度 L：${fmt(output.longitude, 8)}°\n纬度 DMS：${CoordinateMath.decimalToDmsText(output.latitude)}\n经度 DMS：${CoordinateMath.decimalToDmsText(output.longitude)}"
                        val input = buildString {
                            append("北向 X：$northing\n")
                            if (settings.utmInputMode == UtmInputMode.SplitZone) append("带号：$sourceZone\n")
                            append("东向 Y：$easting\n半球：${hemisphere.label}\n椭球：${settings.ellipsoidPreset.label}")
                        }
                        HistoryStore.add(context, "坐标转换", mode.label, input, text)
                        text
                    }.getOrElse { it.message ?: "输入有误" }
                }
            }

            ConvertMode.UtmCrossZone -> {
                ConvertHint(
                    if (settings.utmInputMode == UtmInputMode.SplitZone) {
                        "输入格式：北向 X 为米；原始带号单独输入；东向 Y 输入带内东向值，例如 50 带 534084.68 分别输入 50 和 534084.68。"
                    } else {
                        "输入格式：北向 X 为米；东向 Y 使用当前 UTM 带号编码值，例如 50 带 534084.68 输入 50534084.68。"
                    } + "基准带号输入要转换到的目标带号，例如 49、50、51。",
                )
                NumberField("北向 X", northing, { northing = it }, "例：4429529.03")
                if (settings.utmInputMode == UtmInputMode.SplitZone) {
                    NumberField("原始带号", sourceZone, { sourceZone = it }, "例：50")
                }
                NumberField(
                    "东向 Y",
                    easting,
                    { easting = it },
                    if (settings.utmInputMode == UtmInputMode.SplitZone) "例：534084.68" else "例：50534084.68",
                )
                NumberField("基准带号", baseZone, { baseZone = it }, "例：50")
                ModeChips(
                    options = Hemisphere.entries,
                    selected = hemisphere,
                    label = { it.label },
                    onSelected = { hemisphere = it },
                )
                PrimaryAction("转换到基准带") {
                    result = runCatching {
                        val encodedEasting = encodedUtmEasting(easting, sourceZone, settings.utmInputMode)
                        val sourceZoneValue = (encodedEasting / 1_000_000.0).toInt()
                        val targetZone = baseZone.toInt()
                        val output = CoordinateMath.convertUtmToZone(
                            northing = northing.toDouble(),
                            easting = encodedEasting,
                            hemisphere = hemisphere,
                            targetZone = targetZone,
                            ellipsoid = ellipsoid,
                        )
                        val text = "原始带号：$sourceZoneValue\n基准带号：${output.zone}\n北向 X：${fmt(output.northing)} m\n东向 Y：${fmt(output.easting)} m\n带内东向：${fmt(output.easting - output.zone * 1_000_000.0)} m\n半球：${output.hemisphere.label}"
                        val input = buildString {
                            append("北向 X：$northing\n")
                            if (settings.utmInputMode == UtmInputMode.SplitZone) append("原始带号：$sourceZone\n")
                            append("东向 Y：$easting\n基准带号：$baseZone\n半球：${hemisphere.label}\n椭球：${settings.ellipsoidPreset.label}")
                        }
                        HistoryStore.add(context, "坐标转换", mode.label, input, text)
                        text
                    }.getOrElse { it.message ?: "输入有误" }
                }
            }

            ConvertMode.DegreeToDms -> {
                ConvertHint("输入格式：十进制度数字，例如 116.39747；输出格式为 116°23′50.892″。")
                NumberField("十进制度", degreeInput, { degreeInput = it }, "例：116.39747")
                PrimaryAction("度—>度分秒") {
                    result = runCatching {
                        val text = "度分秒：${CoordinateMath.decimalToDmsText(degreeInput.toDouble())}"
                        HistoryStore.add(context, "坐标转换", mode.label, "十进制度：$degreeInput", text)
                        text
                    }.getOrElse { it.message ?: "输入有误" }
                }
            }

            ConvertMode.DmsToDegree -> {
                ConvertHint("输入格式：按原数字形式输入度分秒，例如 116.2350892 表示 116°23′50.892″。")
                NumberField("度分秒", dmsInput, { dmsInput = it }, "例：116.2350892")
                PrimaryAction("度分秒—>度") {
                    result = runCatching {
                        val text = "十进制度：${fmt(CoordinateMath.dmsToDecimal(dmsInput.toDouble()), 8)}°"
                        HistoryStore.add(context, "坐标转换", mode.label, "度分秒：$dmsInput", text)
                        text
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
    val context = LocalContext.current
    val settings = remember { SettingsStore.load(context) }
    val milScale = settings.defaultMilScale
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
        Text(
            text = "当前密位制：${milScale.label}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                val text = "平距：${fmt(output.horizontalDistance)} m\n斜距：${fmt(output.spatialDistance)} m\n方位角：${fmt(output.azimuthDegrees, 4)}°\n方位密位：${fmt(azimuthMil, 1)}\n高低角：${fmt(output.elevationDegrees, 4)}°\n高低密位：${fmt(elevationMil, 1)}"
                val input = "点 A：X=$x1, Y=$y1, H=$h1\n点 B：X=$x2, Y=$y2, H=$h2\n密位制：${milScale.label}"
                HistoryStore.add(context, "坐标量算", "两点量算", input, text)
                text
            }.getOrElse { "输入有误" }
        }
        ResultPanel(result)
    }
}

@Composable
private fun IntersectionScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { SettingsStore.load(context) }
    val milScale = settings.defaultMilScale
    var ax by rememberSaveable { mutableStateOf("5263587.23") }
    var ay by rememberSaveable { mutableStateOf("51611653.29") }
    var aAzimuth by rememberSaveable { mutableStateOf("1534.09") }
    var bx by rememberSaveable { mutableStateOf("5264047.00") }
    var by by rememberSaveable { mutableStateOf("51611569.83") }
    var bAzimuth by rememberSaveable { mutableStateOf("1780.47") }
    var result by rememberSaveable { mutableStateOf("等待计算") }

    ToolPage(onBack = onBack) {
        SectionTitle("交会计算")
        ConvertHint("输入 A、B 两点平面坐标和各自观测目标的方位密位，计算两条观测方位线的交点。交会计算只考虑平面坐标。")
        Text(
            text = "当前密位制：${milScale.label}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SectionTitle("A 点")
        TripleNumberRow(
            first = FieldState("X", ax) { ax = it },
            second = FieldState("Y", ay) { ay = it },
            third = FieldState("方位密位", aAzimuth) { aAzimuth = it },
        )
        SectionTitle("B 点")
        TripleNumberRow(
            first = FieldState("X", bx) { bx = it },
            second = FieldState("Y", by) { by = it },
            third = FieldState("方位密位", bAzimuth) { bAzimuth = it },
        )
        PrimaryAction("计算目标点") {
            result = runCatching {
                val output = CoordinateMath.intersectionByMils(
                    ax = ax.toDouble(),
                    ay = ay.toDouble(),
                    aAzimuthMil = aAzimuth.toDouble(),
                    bx = bx.toDouble(),
                    by = by.toDouble(),
                    bAzimuthMil = bAzimuth.toDouble(),
                    scale = milScale,
                )
                val text = buildIntersectionText(output, milScale)
                val input = "A 点：X=$ax, Y=$ay, 方位密位=$aAzimuth\nB 点：X=$bx, Y=$by, 方位密位=$bAzimuth\n密位制：${milScale.label}"
                HistoryStore.add(context, "交会计算", "两点方位交会", input, text)
                text
            }.getOrElse { it.message ?: "输入有误" }
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
private fun ProtocolToolsScreen(onBack: () -> Unit) {
    var toolMode by rememberSaveable { mutableStateOf(ProtocolToolMode.Checksum) }

    ToolPage(onBack = onBack) {
        SectionTitle("协议与校验")
        ModeChips(
            options = ProtocolToolMode.entries,
            selected = toolMode,
            label = { it.label },
            onSelected = { toolMode = it },
        )
        when (toolMode) {
            ProtocolToolMode.Checksum -> ChecksumContent()
            ProtocolToolMode.Protocol -> ProtocolContent()
        }
    }
}

@Composable
private fun ChecksumContent() {
    var input by rememberSaveable { mutableStateOf("01 03 00 00 00 02") }
    var mode by rememberSaveable { mutableStateOf(ChecksumMode.Xor8) }
    var result by rememberSaveable { mutableStateOf("等待计算") }

    SectionTitle("校验码计算")
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

@Composable
private fun ProtocolContent() {
    val context = LocalContext.current
    var protocol by rememberSaveable { mutableStateOf(ProtocolKind.Eb90) }
    var receiveCode by rememberSaveable { mutableStateOf("01") }
    var sendCode by rememberSaveable { mutableStateOf("02") }
    var commandCode by rememberSaveable { mutableStateOf("10") }
    var commandParameter by rememberSaveable { mutableStateOf("") }
    var result by rememberSaveable { mutableStateOf("等待生成") }

    fun fieldMaxChars() = protocol.fieldBytes * 2

    SectionTitle("协议生成")
    ConvertHint("所有输入均为十六进制。EB90 的发站码、收站码、命令字均为 1 字节（2 位）；EB48 均为 2 字节（4 位）。命令参数长度不限制，长度字段和校验码自动生成。")
    SectionTitle("协议类型")
    ModeChips(
        options = ProtocolKind.entries,
        selected = protocol,
        label = { it.label },
        onSelected = {
            protocol = it
            val maxChars = it.fieldBytes * 2
            receiveCode = ProtocolBuilder.sanitizeHex(receiveCode).takeLast(maxChars).padStart(maxChars, '0')
            sendCode = ProtocolBuilder.sanitizeHex(sendCode).takeLast(maxChars).padStart(maxChars, '0')
            commandCode = ProtocolBuilder.sanitizeHex(commandCode).takeLast(maxChars).padStart(maxChars, '0')
            result = "等待生成"
        },
    )
    SectionTitle("基本字段")
    TripleTextRow(
        first = FieldState("收站码", receiveCode) {
            receiveCode = ProtocolBuilder.sanitizeHex(it, fieldMaxChars())
        },
        second = FieldState("发站码", sendCode) {
            sendCode = ProtocolBuilder.sanitizeHex(it, fieldMaxChars())
        },
        third = FieldState("命令字", commandCode) {
            commandCode = ProtocolBuilder.sanitizeHex(it, fieldMaxChars())
        },
    )

    SectionTitle("命令参数")
    OutlinedTextField(
        value = commandParameter,
        onValueChange = { commandParameter = ProtocolBuilder.sanitizeHex(it) },
        label = { Text("命令参数") },
        placeholder = { Text("例：01 02 A0") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 3,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
    )
    PrimaryAction("生成报文") {
        result = runCatching {
            val output = ProtocolBuilder.build(
                kind = protocol,
                receiveCodeText = receiveCode,
                sendCodeText = sendCode,
                commandCodeText = commandCode,
                parameterText = commandParameter,
            )
            val text = "报文：${output.message}\n总长度：${output.length} 字节\n校验码：${output.checksum}"
            val input = "协议：${protocol.label}\n收站码：$receiveCode\n发站码：$sendCode\n命令字：$commandCode\n命令参数：$commandParameter"
            HistoryStore.add(context, "协议生成", protocol.label, input, text)
            text
        }.getOrElse { it.message ?: "输入有误" }
    }
    ResultPanel(result)
}

@Composable
private fun HistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var entries by remember { mutableStateOf(HistoryStore.load(context)) }
    var exportText by rememberSaveable { mutableStateOf("") }

    ToolPage(onBack = onBack) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionTitle("历史记录")
            OutlinedButton(
                onClick = {
                    HistoryStore.clear(context)
                    entries = emptyList()
                },
                enabled = entries.isNotEmpty(),
            ) {
                Text("清空")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = {
                    exportText = exportHistory(context, entries, HistoryExportFormat.Txt)
                },
                enabled = entries.isNotEmpty(),
                modifier = Modifier.weight(1f),
            ) {
                Text("导出 TXT")
            }
            OutlinedButton(
                onClick = {
                    exportText = exportHistory(context, entries, HistoryExportFormat.Csv)
                },
                enabled = entries.isNotEmpty(),
                modifier = Modifier.weight(1f),
            ) {
                Text("导出 CSV")
            }
        }
        if (exportText.isNotBlank()) {
            Text(
                text = exportText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (entries.isEmpty()) {
            InfoLine("暂无记录", "完成坐标转换、坐标量算、交会计算或协议生成后，会自动保存结果。")
        } else {
            entries.forEach { entry ->
                HistoryEntryCard(entry)
            }
        }
    }
}

@Composable
private fun HistoryEntryCard(entry: HistoryEntry) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = entry.category,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = HistoryStore.formatTime(entry.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(entry.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            SelectionContainer {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "原始输入",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = entry.input.ifBlank { "旧记录未保存输入" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "结果",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = entry.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SelfTestScreen(onBack: () -> Unit) {
    val items = remember { buildSelfTestItems() }
    val passedCount = items.count { it.passed }

    ToolPage(onBack = onBack) {
        SectionTitle("计算自检/测试样例")
        ConvertHint("使用内置样例自动计算并与参考结果对比。坐标量算、大地解算和交会计算按 6000 密位制核对。")
        Text(
            text = "通过：$passedCount / ${items.size}",
            style = MaterialTheme.typography.titleMedium,
            color = if (passedCount == items.size) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.SemiBold,
        )
        items.forEach { item ->
            SelfTestCard(item)
        }
    }
}

@Composable
private fun SelfTestCard(item: SelfTestItem) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    text = if (item.passed) "通过" else "需复核",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (item.passed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            SelectionContainer {
                Text(
                    text = "计算值：\n${item.actual}\n\n参考值：\n${item.expected}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val current = remember { SettingsStore.load(context) }
    var projectionMode by rememberSaveable { mutableStateOf(current.projectionMode) }
    var defaultMilScale by rememberSaveable { mutableStateOf(current.defaultMilScale) }
    var defaultMapInput by rememberSaveable { mutableStateOf(current.defaultMapInput) }
    var utmInputMode by rememberSaveable { mutableStateOf(current.utmInputMode) }
    var northReference by rememberSaveable { mutableStateOf(current.northReference) }
    var historyLimit by rememberSaveable { mutableStateOf(current.historyLimit.toString()) }
    var saveText by rememberSaveable { mutableStateOf("") }

    ToolPage(onBack = onBack) {
        SectionTitle("参数设置")
        ConvertHint("参数会作为新进入功能时的默认值。坐标转换和地图 UTM 计算默认采用 WGS84 椭球，已打开的界面请返回首页后重新进入以读取新设置。")

        SectionTitle("投影方式")
        ModeChips(
            options = ProjectionMode.entries,
            selected = projectionMode,
            label = { it.label },
            onSelected = {
                projectionMode = it
                saveText = ""
            },
        )

        SectionTitle("默认值")
        Text("默认密位制", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        ModeChips(
            options = MilScale.entries,
            selected = defaultMilScale,
            label = { it.label },
            onSelected = {
                defaultMilScale = it
                saveText = ""
            },
        )
        Text("地图默认输入格式", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        ModeChips(
            options = DefaultMapInput.entries,
            selected = defaultMapInput,
            label = { it.label },
            onSelected = {
                defaultMapInput = it
                saveText = ""
            },
        )
        Text("UTM 输入方式", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        ModeChips(
            options = UtmInputMode.entries,
            selected = utmInputMode,
            label = { it.label },
            onSelected = {
                utmInputMode = it
                saveText = ""
            },
        )
        Text("指南北向基准", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        ModeChips(
            options = NorthReference.entries,
            selected = northReference,
            label = { it.label },
            onSelected = {
                northReference = it
                saveText = ""
            },
        )
        NumberField("历史记录条数限制", historyLimit, {
            historyLimit = it
            saveText = ""
        }, "默认：100")

        PrimaryAction("保存参数") {
            saveText = runCatching {
                val limit = historyLimit.toInt()
                require(limit in 1..10000) { "历史记录条数限制应为 1 到 10000" }
                SettingsStore.save(
                    context,
                    AppSettings(
                        projectionMode = projectionMode,
                        defaultMilScale = defaultMilScale,
                        defaultMapInput = defaultMapInput,
                        utmInputMode = utmInputMode,
                        northReference = northReference,
                        historyLimit = limit,
                    ),
                )
                "参数已保存"
            }.getOrElse { it.message ?: "参数输入有误" }
        }
        if (saveText.isNotBlank()) {
            ResultPanel(saveText)
        }
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
private fun TripleTextRow(first: FieldState, second: FieldState, third: FieldState) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CompactHexField(first, Modifier.weight(1f))
        CompactHexField(second, Modifier.weight(1f))
        CompactHexField(third, Modifier.weight(1f))
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
private fun CompactHexField(state: FieldState, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = state.value,
        onValueChange = state.onValueChange,
        label = { Text(state.label) },
        modifier = modifier,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
        textStyle = MaterialTheme.typography.bodySmall,
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

private fun buildIntersectionText(result: IntersectionResult, milScale: MilScale): String {
    val aMil = CoordinateMath.degreesToMils(result.azimuthFromADegrees, milScale)
    val bMil = CoordinateMath.degreesToMils(result.azimuthFromBDegrees, milScale)
    return buildString {
        append("目标点 X：${fmt(result.x)}\n")
        append("目标点 Y：${fmt(result.y)}\n")
        append("A 点至目标距离：${fmt(result.distanceFromA)} m\n")
        append("B 点至目标距离：${fmt(result.distanceFromB)} m\n")
        append("A 点观测方位：${fmt(result.azimuthFromADegrees, 4)}° / ${fmt(aMil, 1)}\n")
        append("B 点观测方位：${fmt(result.azimuthFromBDegrees, 4)}° / ${fmt(bMil, 1)}")
    }
}

private fun buildSelfTestItems(): List<SelfTestItem> {
    val scale = MilScale.Mil6000
    return listOf(
        selfTestItem("坐标转换：UTM → 经纬度") {
            val geo = CoordinateMath.utmToLatLon(4185332.0, 49638171.0, Hemisphere.North)
            val actual = "纬度：${fmt(geo.latitude, 6)}\n经度：${fmt(geo.longitude, 6)}\n纬度 DMS：${CoordinateMath.decimalToDmsText(geo.latitude, 2)}\n经度 DMS：${CoordinateMath.decimalToDmsText(geo.longitude, 2)}"
            val expected = "纬度：37.804931\n经度：112.569555\n纬度 DMS：37°48′17.75″\n经度 DMS：112°34′10.40″"
            SelfTestItem(
                title = "坐标转换：UTM → 经纬度",
                actual = actual,
                expected = expected,
                passed = near(geo.latitude, 37.804931, 0.0001) && near(geo.longitude, 112.569555, 0.0001),
            )
        },
        selfTestItem("坐标转换：经纬度 → UTM") {
            val utm = CoordinateMath.latLonToUtm(37.804931, 112.569555)
            val actual = "北向 X：${fmt(utm.northing, 2)}\n东向 Y：${fmt(utm.easting, 2)}\n带号：${utm.zone}"
            val expected = "北向 X：4185332\n东向 Y：49638171"
            SelfTestItem(
                title = "坐标转换：经纬度 → UTM",
                actual = actual,
                expected = expected,
                passed = near(utm.northing, 4185332.0, 5.0) && near(utm.easting, 49638171.0, 5.0),
            )
        },
        selfTestItem("UTM 跨带计算到 50 带") {
            val converted = CoordinateMath.convertUtmToZone(4185332.0, 49638171.0, Hemisphere.North, 50)
            val actual = "北向 X：${fmt(converted.northing, 2)}\n东向 Y：${fmt(converted.easting, 2)}"
            val expected = "北向 X：4193427.50\n东向 Y：50109892.99"
            SelfTestItem(
                title = "UTM 跨带计算到 50 带",
                actual = actual,
                expected = expected,
                passed = near(converted.northing, 4193427.5, 5.0) && near(converted.easting, 50109892.99, 5.0),
            )
        },
        selfTestItem("坐标量算") {
            val result = CoordinateMath.distance(5263819.6, 51611865.5, 161.0, 5263537.2, 51613256.1, 170.0)
            val azimuthMil = CoordinateMath.degreesToMils(result.azimuthDegrees, scale)
            val elevationMil = CoordinateMath.degreesToMils(result.elevationDegrees, scale, signed = true)
            val actual = "D：${fmt(result.horizontalDistance, 2)}\n方位角：${fmt(azimuthMil, 2)}\n高低角：${fmt(elevationMil, 2)}"
            val expected = "D：1419.01\n方位角：1691.32\n高低角：6.06"
            SelfTestItem(
                title = "坐标量算",
                actual = actual,
                expected = expected,
                passed = near(result.horizontalDistance, 1419.01, 0.1) && near(azimuthMil, 1691.32, 0.2) && near(elevationMil, 6.06, 0.1),
            )
        },
        selfTestItem("大地解算") {
            val result = CoordinateMath.forwardSolveByMils(
                x = 5259857.87,
                y = 51635675.36,
                h = 167.0,
                azimuthMil = 2455.506,
                elevationMil = -2.6738,
                distance = 1210.0,
                scale = scale,
            )
            val actual = "Pts：[${fmt(result.x, 2)}, ${fmt(result.y, 2)}, ${fmt(result.h, 2)}]"
            val expected = "Pts：[5258839.30, 51636328.52, 164]"
            SelfTestItem(
                title = "大地解算",
                actual = actual,
                expected = expected,
                passed = near(result.x, 5258839.30, 0.2) && near(result.y, 51636328.52, 0.2) && near(result.h, 164.0, 0.5),
            )
        },
        selfTestItem("交会计算") {
            val result = CoordinateMath.intersectionByMils(
                ax = 5263587.23,
                ay = 51611653.29,
                aAzimuthMil = 1534.09,
                bx = 5264047.00,
                by = 51611569.83,
                bAzimuthMil = 1780.47,
                scale = scale,
            )
            val actual = "${fmt(result.x, 2)}, ${fmt(result.y, 2)}"
            val expected = "5263529.04, 51613282.15"
            SelfTestItem(
                title = "交会计算",
                actual = actual,
                expected = expected,
                passed = near(result.x, 5263529.04, 0.5) && near(result.y, 51613282.15, 0.5),
            )
        },
    )
}

private fun selfTestItem(title: String, block: () -> SelfTestItem): SelfTestItem {
    return runCatching(block).getOrElse {
        SelfTestItem(title, it.message ?: "计算失败", "应正常完成计算", false)
    }
}

private fun near(actual: Double, expected: Double, tolerance: Double): Boolean {
    return abs(actual - expected) <= tolerance
}

private fun exportHistory(context: Context, entries: List<HistoryEntry>, format: HistoryExportFormat): String {
    return runCatching {
        val exportDir = File(context.cacheDir, "history_exports")
        exportDir.mkdirs()
        val timeText = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(exportDir, "history_$timeText.${format.extension}")
        val content = when (format) {
            HistoryExportFormat.Txt -> buildHistoryTxt(entries)
            HistoryExportFormat.Csv -> buildHistoryCsv(entries)
        }
        file.writeText(content, Charsets.UTF_8)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = format.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "工程量算助手历史记录")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "导出历史记录"))
        "已生成 ${format.label} 文件：${file.name}"
    }.getOrElse { it.message ?: "导出失败" }
}

private fun buildHistoryTxt(entries: List<HistoryEntry>): String {
    return buildString {
        appendLine("工程量算助手历史记录")
        appendLine("导出时间：${HistoryStore.formatTime(System.currentTimeMillis())}")
        appendLine()
        entries.forEachIndexed { index, entry ->
            appendLine("${index + 1}. ${entry.category} - ${entry.title}")
            appendLine("时间：${HistoryStore.formatTime(entry.timestamp)}")
            appendLine("原始输入：")
            appendLine(entry.input.ifBlank { "旧记录未保存输入" })
            appendLine("结果：")
            appendLine(entry.content)
            appendLine()
        }
    }
}

private fun buildHistoryCsv(entries: List<HistoryEntry>): String {
    return buildString {
        append('\uFEFF')
        appendLine(listOf("时间", "类别", "标题", "原始输入", "结果").joinToString(",") { csvCell(it) })
        entries.forEach { entry ->
            appendLine(
                listOf(
                    HistoryStore.formatTime(entry.timestamp),
                    entry.category,
                    entry.title,
                    entry.input.ifBlank { "旧记录未保存输入" },
                    entry.content,
                ).joinToString(",") { csvCell(it) },
            )
        }
    }
}

private fun csvCell(value: String): String {
    return "\"${value.replace("\"", "\"\"")}\""
}

private fun encodedUtmEasting(eastingText: String, zoneText: String, inputMode: UtmInputMode): Double {
    val rawEasting = eastingText.toDouble()
    return when (inputMode) {
        UtmInputMode.MergedZone -> {
            val zone = (rawEasting / 1_000_000.0).toInt()
            require(zone in 1..60) { "UTM 带号应为 1 到 60" }
            rawEasting
        }
        UtmInputMode.SplitZone -> {
            val zone = zoneText.toIntOrNull()
            require(zone != null && zone in 1..60) { "UTM 带号应为 1 到 60" }
            rawEasting + zone * 1_000_000.0
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ToolBoxPreview() {
    MyApplicationTheme(dynamicColor = false) {
        ToolBoxApp()
    }
}
