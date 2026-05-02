package com.example.myapplication

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.ViewParent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.vector.path
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.amap.api.location.AMapLocationClient
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.MapsInitializer
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.MyLocationStyle
import java.util.Locale

private data class LocationSnapshot(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
)

private data class DestinationPoint(
    val latitude: Double,
    val longitude: Double,
    val sourceLabel: String,
)

private enum class DestinationInputMode(val label: String) {
    LatLon("经纬度"),
    Utm("UTM"),
}

private val CurrentLocationIcon: ImageVector
    get() = Builder(
        name = "CurrentLocation",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 8f)
            curveTo(9.79f, 8f, 8f, 9.79f, 8f, 12f)
            reflectiveCurveTo(9.79f, 16f, 12f, 16f)
            reflectiveCurveTo(16f, 14.21f, 16f, 12f)
            reflectiveCurveTo(14.21f, 8f, 12f, 8f)
            close()
            moveTo(20.94f, 11f)
            curveTo(20.48f, 6.83f, 17.17f, 3.52f, 13f, 3.06f)
            verticalLineTo(1f)
            horizontalLineTo(11f)
            verticalLineTo(3.06f)
            curveTo(6.83f, 3.52f, 3.52f, 6.83f, 3.06f, 11f)
            horizontalLineTo(1f)
            verticalLineTo(13f)
            horizontalLineTo(3.06f)
            curveTo(3.52f, 17.17f, 6.83f, 20.48f, 11f, 20.94f)
            verticalLineTo(23f)
            horizontalLineTo(13f)
            verticalLineTo(20.94f)
            curveTo(17.17f, 20.48f, 20.48f, 17.17f, 20.94f, 13f)
            horizontalLineTo(23f)
            verticalLineTo(11f)
            horizontalLineTo(20.94f)
            close()
            moveTo(12f, 19f)
            curveTo(8.13f, 19f, 5f, 15.87f, 5f, 12f)
            reflectiveCurveTo(8.13f, 5f, 12f, 5f)
            reflectiveCurveTo(19f, 8.13f, 19f, 12f)
            reflectiveCurveTo(15.87f, 19f, 12f, 19f)
            close()
        }
    }.build()

@Composable
fun AmapLocationMap(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val settings = remember { SettingsStore.load(context) }
    val ellipsoid = settings.ellipsoidParameters()
    val utmInputMode = settings.utmInputMode
    MapsInitializer.updatePrivacyShow(context, true, true)
    MapsInitializer.updatePrivacyAgree(context, true)
    AMapLocationClient.updatePrivacyShow(context, true, true)
    AMapLocationClient.updatePrivacyAgree(context, true)

    var hasLocationPermission by rememberSaveable {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED,
        )
    }
    var location by remember { mutableStateOf<LocationSnapshot?>(null) }
    var statusText by rememberSaveable { mutableStateOf(if (hasLocationPermission) "正在获取当前位置" else "等待定位授权") }
    var inputMode by rememberSaveable {
        mutableStateOf(
            when (settings.defaultMapInput) {
                DefaultMapInput.LatLon -> DestinationInputMode.LatLon
                DefaultMapInput.Utm -> DestinationInputMode.Utm
            },
        )
    }
    var targetLat by rememberSaveable { mutableStateOf("") }
    var targetLon by rememberSaveable { mutableStateOf("") }
    var targetZone by rememberSaveable { mutableStateOf("") }
    var targetNorthing by rememberSaveable { mutableStateOf("") }
    var targetEasting by rememberSaveable { mutableStateOf("") }
    var targetHemisphere by rememberSaveable { mutableStateOf(Hemisphere.North) }
    var destinationLat by rememberSaveable { mutableStateOf<Double?>(null) }
    var destinationLon by rememberSaveable { mutableStateOf<Double?>(null) }
    var destinationSource by rememberSaveable { mutableStateOf("") }
    val destination = if (destinationLat != null && destinationLon != null) {
        DestinationPoint(destinationLat!!, destinationLon!!, destinationSource)
    } else {
        null
    }
    fun setDestination(point: DestinationPoint?) {
        destinationLat = point?.latitude
        destinationLon = point?.longitude
        destinationSource = point?.sourceLabel.orEmpty()
    }
    var routeText by rememberSaveable { mutableStateOf("请输入目的坐标后显示目的地") }
    var navigationText by rememberSaveable { mutableStateOf("") }
    var pickModeEnabled by rememberSaveable { mutableStateOf(false) }
    var recenterRequest by rememberSaveable { mutableStateOf(0) }
    var destinationFocusRequest by rememberSaveable { mutableStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        statusText = if (hasLocationPermission) "正在获取当前位置" else "未授予定位权限"
    }

    fun requestRecenter() {
        if (location == null) {
            navigationText = "当前位置尚未获取"
        } else {
            recenterRequest += 1
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MapArea(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.48f),
            hasLocationPermission = hasLocationPermission,
            currentLocation = location,
            destination = destination,
            pickModeEnabled = pickModeEnabled,
            recenterRequest = recenterRequest,
            destinationFocusRequest = destinationFocusRequest,
            onRecenter = ::requestRecenter,
            onMapPicked = { latitude, longitude ->
                if (!pickModeEnabled) return@MapArea
                val picked = DestinationPoint(latitude, longitude, "地图选点")
                setDestination(picked)
                pickModeEnabled = false
                when (inputMode) {
                    DestinationInputMode.LatLon -> {
                        targetLat = fmt(latitude, 7)
                        targetLon = fmt(longitude, 7)
                    }

                    DestinationInputMode.Utm -> {
                        val utm = CoordinateMath.latLonToUtm(latitude, longitude, ellipsoid = ellipsoid)
                        targetZone = if (utmInputMode == UtmInputMode.SplitZone) utm.zone.toString() else ""
                        targetNorthing = fmt(utm.northing, 3)
                        targetEasting = if (utmInputMode == UtmInputMode.SplitZone) {
                            fmt(utm.easting - utm.zone * 1_000_000.0, 3)
                        } else {
                            fmt(utm.easting, 3)
                        }
                        targetHemisphere = utm.hemisphere
                    }
                }
                routeText = "目的地已显示，点击打开高德地图后规划路径"
                destinationFocusRequest += 1
            },
            onLocationChanged = { latitude, longitude, accuracy ->
                location = LocationSnapshot(latitude, longitude, accuracy)
                statusText = "定位成功"
            },
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.52f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DestinationSearchPanel(
                inputMode = inputMode,
                onInputModeChange = {
                    inputMode = it
                    routeText = "请输入目的坐标后显示目的地"
                },
                targetLat = targetLat,
                onTargetLatChange = { targetLat = it },
                targetLon = targetLon,
                onTargetLonChange = { targetLon = it },
                targetZone = targetZone,
                onTargetZoneChange = { targetZone = it },
                targetNorthing = targetNorthing,
                onTargetNorthingChange = { targetNorthing = it },
                targetEasting = targetEasting,
                onTargetEastingChange = { targetEasting = it },
                targetHemisphere = targetHemisphere,
                onTargetHemisphereChange = { targetHemisphere = it },
                utmInputMode = utmInputMode,
                onPlanRoute = {
                    val parsed = parseDestination(
                        inputMode = inputMode,
                        latitudeText = targetLat,
                        longitudeText = targetLon,
                        zoneText = targetZone,
                        northingText = targetNorthing,
                        eastingText = targetEasting,
                        hemisphere = targetHemisphere,
                        ellipsoid = ellipsoid,
                        utmInputMode = utmInputMode,
                    )
                    if (parsed.isSuccess) {
                        setDestination(parsed.getOrThrow())
                        routeText = "目的地已显示，点击打开高德地图后规划路径"
                        destinationFocusRequest += 1
                    } else {
                        routeText = parsed.exceptionOrNull()?.message ?: "目的坐标输入有误"
                    }
                },
                pickModeEnabled = pickModeEnabled,
                onPickModeChange = { enabled ->
                    pickModeEnabled = enabled
                    routeText = if (enabled) "选点模式已开启，请在地图上点击目的位置" else "选点模式已关闭"
                },
            )

            if (!hasLocationPermission) {
                Button(
                    onClick = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                            ),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("授权定位并显示当前位置")
                }
            }

            LocationInfoPanel(
                location = location,
                destination = destination,
                statusText = statusText,
                routeText = routeText,
                navigationText = navigationText,
                ellipsoid = ellipsoid,
                onNavigate = {
                    val target = destination
                    if (target == null) {
                        navigationText = "请先输入目的坐标"
                    } else {
                        navigationText = startAmapNavigation(
                            context = context,
                            start = location,
                            destination = target,
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun DestinationSearchPanel(
    inputMode: DestinationInputMode,
    onInputModeChange: (DestinationInputMode) -> Unit,
    targetLat: String,
    onTargetLatChange: (String) -> Unit,
    targetLon: String,
    onTargetLonChange: (String) -> Unit,
    targetZone: String,
    onTargetZoneChange: (String) -> Unit,
    targetNorthing: String,
    onTargetNorthingChange: (String) -> Unit,
    targetEasting: String,
    onTargetEastingChange: (String) -> Unit,
    targetHemisphere: Hemisphere,
    onTargetHemisphereChange: (Hemisphere) -> Unit,
    utmInputMode: UtmInputMode,
    onPlanRoute: () -> Unit,
    pickModeEnabled: Boolean,
    onPickModeChange: (Boolean) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "目的坐标搜索",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DestinationInputMode.entries.forEach { mode ->
                    FilterChip(
                        selected = inputMode == mode,
                        onClick = { onInputModeChange(mode) },
                        label = { Text(mode.label) },
                    )
                }
            }
            when (inputMode) {
                DestinationInputMode.LatLon -> {
                    CoordinateInputRow(
                        firstLabel = "纬度",
                        firstValue = targetLat,
                        onFirstChange = onTargetLatChange,
                        secondLabel = "经度",
                        secondValue = targetLon,
                        onSecondChange = onTargetLonChange,
                    )
                }

                DestinationInputMode.Utm -> {
                    if (utmInputMode == UtmInputMode.SplitZone) {
                        OutlinedTextField(
                            value = targetZone,
                            onValueChange = onTargetZoneChange,
                            label = { Text("带号") },
                            placeholder = { Text("例：50") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = MaterialTheme.typography.bodySmall,
                        )
                    }
                    CoordinateInputRow(
                        firstLabel = "北向 X",
                        firstValue = targetNorthing,
                        onFirstChange = onTargetNorthingChange,
                        secondLabel = "东向 Y",
                        secondValue = targetEasting,
                        onSecondChange = onTargetEastingChange,
                    )
                    Text(
                        text = if (utmInputMode == UtmInputMode.SplitZone) {
                            "UTM 输入方式：带号拆分，东向 Y 输入带内值"
                        } else {
                            "UTM 输入方式：带号合并，东向 Y 输入带号编码值"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Hemisphere.entries.forEach { hemisphere ->
                            FilterChip(
                                selected = targetHemisphere == hemisphere,
                                onClick = { onTargetHemisphereChange(hemisphere) },
                                label = { Text(hemisphere.label) },
                            )
                        }
                    }
                }
            }
            Button(
                onClick = onPlanRoute,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("显示目的地位置")
            }
            Button(
                onClick = { onPickModeChange(!pickModeEnabled) },
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = if (pickModeEnabled) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                ),
            ) {
                Text(if (pickModeEnabled) "选点模式已开启" else "从地图选点")
            }
        }
    }
}

@Composable
private fun CoordinateInputRow(
    firstLabel: String,
    firstValue: String,
    onFirstChange: (String) -> Unit,
    secondLabel: String,
    secondValue: String,
    onSecondChange: (String) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = firstValue,
            onValueChange = onFirstChange,
            label = { Text(firstLabel) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = MaterialTheme.typography.bodySmall,
        )
        OutlinedTextField(
            value = secondValue,
            onValueChange = onSecondChange,
            label = { Text(secondLabel) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun LocationInfoPanel(
    location: LocationSnapshot?,
    destination: DestinationPoint?,
    statusText: String,
    routeText: String,
    navigationText: String,
    ellipsoid: EllipsoidParameters,
    onNavigate: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        SelectionContainer {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "当前位置",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (location == null) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val utm = runCatching {
                    CoordinateMath.latLonToUtm(location.latitude, location.longitude, ellipsoid = ellipsoid)
                }.getOrNull()
                Text(
                    text = buildString {
                        append("纬度：${fmt(location.latitude, 7)}°\n")
                        append("经度：${fmt(location.longitude, 7)}°")
                        if (location.accuracy > 0f) {
                            append("\n精度：${fmt(location.accuracy.toDouble(), 1)} m")
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (utm != null) {
                    Text(
                        text = buildString {
                            append("UTM 北向 X：${fmt(utm.northing, 3)} m\n")
                            append("UTM 东向 Y：${fmt(utm.easting, 3)} m\n")
                            append("带号：${utm.zone}${CoordinateMath.latitudeBand(location.latitude) ?: '-'}  ${utm.hemisphere.label}")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text(
                text = "目的位置",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp),
            )
            if (destination == null) {
                Text(
                    text = "未设置",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val utm = runCatching {
                    CoordinateMath.latLonToUtm(destination.latitude, destination.longitude, ellipsoid = ellipsoid)
                }.getOrNull()
                Text(
                    text = buildString {
                        append("纬度：${fmt(destination.latitude, 7)}°\n")
                        append("经度：${fmt(destination.longitude, 7)}°\n")
                        if (utm != null) {
                            append("UTM 北向 X：${fmt(utm.northing, 3)} m\n")
                            append("UTM 东向 Y：${fmt(utm.easting, 3)} m\n")
                            append("带号：${utm.zone}${CoordinateMath.latitudeBand(destination.latitude) ?: '-'}  ${utm.hemisphere.label}\n")
                        }
                        append("来源：${destination.sourceLabel}")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = "目的状态",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp),
            )
            SelectionContainer {
                Text(
                    text = routeText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = onNavigate,
                enabled = destination != null,
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Text("打开高德地图导航")
            }
            if (navigationText.isNotBlank()) {
                SelectionContainer {
                    Text(
                        text = navigationText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun MapArea(
    modifier: Modifier = Modifier,
    hasLocationPermission: Boolean,
    currentLocation: LocationSnapshot?,
    destination: DestinationPoint?,
    pickModeEnabled: Boolean,
    recenterRequest: Int,
    destinationFocusRequest: Int,
    onRecenter: () -> Unit,
    onMapPicked: (latitude: Double, longitude: Double) -> Unit,
    onLocationChanged: (latitude: Double, longitude: Double, accuracy: Float) -> Unit,
) {
    Box(modifier = modifier.clip(RoundedCornerShape(8.dp))) {
        AmapMapView(
            hasLocationPermission = hasLocationPermission,
            currentLocation = currentLocation,
            destination = destination,
            pickModeEnabled = pickModeEnabled,
            recenterRequest = recenterRequest,
            destinationFocusRequest = destinationFocusRequest,
            onMapPicked = onMapPicked,
            onLocationChanged = onLocationChanged,
        )
        Surface(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 10.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            tonalElevation = 4.dp,
            shadowElevation = 4.dp,
        ) {
            IconButton(
            onClick = onRecenter,
            enabled = currentLocation != null,
            modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = CurrentLocationIcon,
                    contentDescription = "回到当前位置",
                    modifier = Modifier.size(20.dp),
                    tint = if (currentLocation != null) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

@Composable
private fun AmapMapView(
    hasLocationPermission: Boolean,
    currentLocation: LocationSnapshot?,
    destination: DestinationPoint?,
    pickModeEnabled: Boolean,
    recenterRequest: Int,
    destinationFocusRequest: Int,
    onMapPicked: (latitude: Double, longitude: Double) -> Unit,
    onLocationChanged: (latitude: Double, longitude: Double, accuracy: Float) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var destinationMarker by remember { mutableStateOf<Marker?>(null) }
    var lastDestinationKey by remember { mutableStateOf<String?>(null) }
    var handledRecenterRequest by remember { mutableStateOf(0) }
    var handledDestinationFocusRequest by remember { mutableStateOf(0) }
    var appliedLocationPermission by remember { mutableStateOf<Boolean?>(null) }
    val latestPickModeEnabled by rememberUpdatedState(pickModeEnabled)
    val latestOnMapPicked by rememberUpdatedState(onMapPicked)
    val latestOnLocationChanged by rememberUpdatedState(onLocationChanged)
    val mapView = remember {
        MapView(context)
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            mapView.onResume()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    AndroidView(
        factory = {
            mapView.apply {
                onCreate(Bundle())
                val amap = map
                amap.uiSettings.isZoomControlsEnabled = true
                amap.uiSettings.isMyLocationButtonEnabled = false
                amap.uiSettings.isCompassEnabled = true
                amap.uiSettings.isScaleControlsEnabled = true
                amap.myLocationStyle = MyLocationStyle()
                    .myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER)
                    .interval(2000L)
                amap.setOnMapClickListener { latLng ->
                    if (latestPickModeEnabled) {
                        latestOnMapPicked(latLng.latitude, latLng.longitude)
                    }
                }
                amap.setOnMyLocationChangeListener { location ->
                    if (location != null) {
                        latestOnLocationChanged(location.latitude, location.longitude, location.accuracy)
                    }
                }
                fun requestParentDisallowIntercept(disallow: Boolean) {
                    var parentView: ViewParent? = parent
                    while (parentView != null) {
                        parentView.requestDisallowInterceptTouchEvent(disallow)
                        parentView = parentView.parent
                    }
                }
                setOnTouchListener { _, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN,
                        MotionEvent.ACTION_MOVE,
                        MotionEvent.ACTION_POINTER_DOWN,
                        MotionEvent.ACTION_POINTER_UP -> requestParentDisallowIntercept(true)
                        MotionEvent.ACTION_UP,
                        MotionEvent.ACTION_CANCEL -> requestParentDisallowIntercept(false)
                    }
                    false
                }
            }
        },
        modifier = Modifier
            .fillMaxSize(),
        update = { view ->
            val amap = view.map
            if (appliedLocationPermission != hasLocationPermission) {
                appliedLocationPermission = hasLocationPermission
                amap.isMyLocationEnabled = hasLocationPermission
            }

            if (destination != null) {
                val target = LatLng(destination.latitude, destination.longitude)
                val destinationKey = listOf(
                    destination.latitude.roundKey(),
                    destination.longitude.roundKey(),
                    destination.sourceLabel,
                ).joinToString("|")
                if (destinationKey != lastDestinationKey) {
                    lastDestinationKey = destinationKey
                    destinationMarker?.remove()
                    destinationMarker = amap.addMarker(
                        MarkerOptions()
                            .position(target)
                            .title("目的位置")
                            .snippet(destination.sourceLabel),
                    )
                }
                if (destinationFocusRequest > handledDestinationFocusRequest) {
                    handledDestinationFocusRequest = destinationFocusRequest
                    amap.animateCamera(CameraUpdateFactory.newLatLngZoom(target, 16f))
                }
            } else {
                destinationMarker?.remove()
                destinationMarker = null
                lastDestinationKey = null
                handledDestinationFocusRequest = destinationFocusRequest
            }

            if (currentLocation != null && recenterRequest > handledRecenterRequest) {
                handledRecenterRequest = recenterRequest
                amap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(currentLocation.latitude, currentLocation.longitude),
                        17f,
                    ),
                )
            }
        },
    )
}

private fun fmt(value: Double, digits: Int): String {
    return String.format(Locale.US, "%.${digits}f", value)
}

private fun parseDestination(
    inputMode: DestinationInputMode,
    latitudeText: String,
    longitudeText: String,
    zoneText: String,
    northingText: String,
    eastingText: String,
    hemisphere: Hemisphere,
    ellipsoid: EllipsoidParameters,
    utmInputMode: UtmInputMode,
): Result<DestinationPoint> {
    return runCatching {
        when (inputMode) {
            DestinationInputMode.LatLon -> {
                val latitude = latitudeText.toDouble()
                val longitude = longitudeText.toDouble()
                require(latitude in -90.0..90.0) { "纬度范围应为 -90 到 90" }
                require(longitude in -180.0..180.0) { "经度范围应为 -180 到 180" }
                DestinationPoint(latitude, longitude, "经纬度输入")
            }

            DestinationInputMode.Utm -> {
                val rawEasting = eastingText.toDouble()
                val encodedEasting = when (utmInputMode) {
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
                val coordinate = CoordinateMath.utmToLatLon(
                    northing = northingText.toDouble(),
                    easting = encodedEasting,
                    hemisphere = hemisphere,
                    ellipsoid = ellipsoid,
                )
                DestinationPoint(coordinate.latitude, coordinate.longitude, "UTM 输入")
            }
        }
    }
}

private fun Double.roundKey(): String = String.format(Locale.US, "%.6f", this)

private fun startAmapNavigation(
    context: android.content.Context,
    start: LocationSnapshot?,
    destination: DestinationPoint,
): String {
    val uri = buildString {
        append("androidamap://route/plan/?sourceApplication=工程量算助手")
        if (start != null) {
            append("&slat=${start.latitude}")
            append("&slon=${start.longitude}")
            append("&sname=当前位置")
        }
        append("&dlat=${destination.latitude}")
        append("&dlon=${destination.longitude}")
        append("&dname=目的位置")
        append("&dev=0")
        append("&t=0")
    }
    return try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            setPackage("com.autonavi.minimap")
        }
        context.startActivity(intent)
        "已打开高德导航"
    } catch (error: ActivityNotFoundException) {
        "未找到高德地图 App，请先安装高德地图"
    }
}
