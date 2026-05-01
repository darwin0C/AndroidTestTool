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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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
import com.amap.api.maps.model.Polyline
import com.amap.api.maps.model.PolylineOptions
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.route.BusRouteResult
import com.amap.api.services.route.DriveRouteResult
import com.amap.api.services.route.RideRouteResult
import com.amap.api.services.route.RouteSearch
import com.amap.api.services.route.WalkRouteResult
import java.util.Locale
import kotlin.math.roundToInt

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

@Composable
fun AmapLocationMap() {
    val context = LocalContext.current
    MapsInitializer.updatePrivacyShow(context, true, true)
    MapsInitializer.updatePrivacyAgree(context, true)
    AMapLocationClient.updatePrivacyShow(context, true, true)
    AMapLocationClient.updatePrivacyAgree(context, true)

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED,
        )
    }
    var location by remember { mutableStateOf<LocationSnapshot?>(null) }
    var statusText by remember { mutableStateOf(if (hasLocationPermission) "正在获取当前位置" else "等待定位授权") }
    var inputMode by remember { mutableStateOf(DestinationInputMode.LatLon) }
    var targetLat by remember { mutableStateOf("") }
    var targetLon by remember { mutableStateOf("") }
    var targetNorthing by remember { mutableStateOf("") }
    var targetEasting by remember { mutableStateOf("") }
    var targetHemisphere by remember { mutableStateOf(Hemisphere.North) }
    var destination by remember { mutableStateOf<DestinationPoint?>(null) }
    var routeText by remember { mutableStateOf("请输入目的坐标后规划路线") }
    var navigationText by remember { mutableStateOf("") }
    var pickModeEnabled by remember { mutableStateOf(false) }
    var recenterRequest by remember { mutableStateOf(0) }

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
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.30f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "高德地图",
                style = MaterialTheme.typography.titleMedium,
            )

            DestinationSearchPanel(
                inputMode = inputMode,
                onInputModeChange = {
                    inputMode = it
                    routeText = "请输入目的坐标后规划路线"
                },
                targetLat = targetLat,
                onTargetLatChange = { targetLat = it },
                targetLon = targetLon,
                onTargetLonChange = { targetLon = it },
                targetNorthing = targetNorthing,
                onTargetNorthingChange = { targetNorthing = it },
                targetEasting = targetEasting,
                onTargetEastingChange = { targetEasting = it },
                targetHemisphere = targetHemisphere,
                onTargetHemisphereChange = { targetHemisphere = it },
                onPlanRoute = {
                    val parsed = parseDestination(
                        inputMode = inputMode,
                        latitudeText = targetLat,
                        longitudeText = targetLon,
                        northingText = targetNorthing,
                        eastingText = targetEasting,
                        hemisphere = targetHemisphere,
                    )
                    if (parsed.isSuccess) {
                        destination = parsed.getOrThrow()
                        routeText = if (location == null) {
                            "目标点已标记，等待当前位置后自动规划路线"
                        } else {
                            "正在规划路线"
                        }
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
        }

        MapArea(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.42f),
            hasLocationPermission = hasLocationPermission,
            currentLocation = location,
            destination = destination,
            pickModeEnabled = pickModeEnabled,
            recenterRequest = recenterRequest,
            onRecenter = ::requestRecenter,
            onMapPicked = { latitude, longitude ->
                if (!pickModeEnabled) return@MapArea
                val picked = DestinationPoint(latitude, longitude, "地图选点")
                destination = picked
                pickModeEnabled = false
                when (inputMode) {
                    DestinationInputMode.LatLon -> {
                        targetLat = fmt(latitude, 7)
                        targetLon = fmt(longitude, 7)
                    }

                    DestinationInputMode.Utm -> {
                        val utm = CoordinateMath.latLonToUtm(latitude, longitude)
                        targetNorthing = fmt(utm.northing, 3)
                        targetEasting = fmt(utm.easting, 3)
                        targetHemisphere = utm.hemisphere
                    }
                }
                routeText = if (location == null) {
                    "地图选点已标记，等待当前位置后自动规划路线"
                } else {
                    "正在规划路线"
                }
            },
            onLocationChanged = { latitude, longitude, accuracy ->
                location = LocationSnapshot(latitude, longitude, accuracy)
                statusText = "定位成功"
            },
            onRouteStatusChanged = { routeText = it },
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.28f)
                .verticalScroll(rememberScrollState()),
        ) {
            LocationInfoPanel(
                location = location,
                destination = destination,
                statusText = statusText,
                routeText = routeText,
                navigationText = navigationText,
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
    targetNorthing: String,
    onTargetNorthingChange: (String) -> Unit,
    targetEasting: String,
    onTargetEastingChange: (String) -> Unit,
    targetHemisphere: Hemisphere,
    onTargetHemisphereChange: (Hemisphere) -> Unit,
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
                    CoordinateInputRow(
                        firstLabel = "北向 X",
                        firstValue = targetNorthing,
                        onFirstChange = onTargetNorthingChange,
                        secondLabel = "东向 Y",
                        secondValue = targetEasting,
                        onSecondChange = onTargetEastingChange,
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
                Text("显示目的位置并规划路线")
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
        )
        OutlinedTextField(
            value = secondValue,
            onValueChange = onSecondChange,
            label = { Text(secondLabel) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
    onNavigate: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
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
                    CoordinateMath.latLonToUtm(location.latitude, location.longitude)
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
                    CoordinateMath.latLonToUtm(destination.latitude, destination.longitude)
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
                text = "路线规划",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = routeText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = onNavigate,
                enabled = destination != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("打开高德导航")
            }
            if (navigationText.isNotBlank()) {
                Text(
                    text = navigationText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
    onRecenter: () -> Unit,
    onMapPicked: (latitude: Double, longitude: Double) -> Unit,
    onLocationChanged: (latitude: Double, longitude: Double, accuracy: Float) -> Unit,
    onRouteStatusChanged: (String) -> Unit,
) {
    Box(modifier = modifier.clip(RoundedCornerShape(8.dp))) {
        AmapMapView(
            hasLocationPermission = hasLocationPermission,
            currentLocation = currentLocation,
            destination = destination,
            pickModeEnabled = pickModeEnabled,
            recenterRequest = recenterRequest,
            onMapPicked = onMapPicked,
            onLocationChanged = onLocationChanged,
            onRouteStatusChanged = onRouteStatusChanged,
        )
        Button(
            onClick = onRecenter,
            enabled = currentLocation != null,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp),
        ) {
            Text("回到当前位置")
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
    onMapPicked: (latitude: Double, longitude: Double) -> Unit,
    onLocationChanged: (latitude: Double, longitude: Double, accuracy: Float) -> Unit,
    onRouteStatusChanged: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var destinationMarker by remember { mutableStateOf<Marker?>(null) }
    var routePolyline by remember { mutableStateOf<Polyline?>(null) }
    var lastRouteKey by remember { mutableStateOf<String?>(null) }
    var handledRecenterRequest by remember { mutableStateOf(0) }
    val mapView = remember {
        MapView(context).apply {
            onCreate(Bundle())
        }
    }
    val routeSearch = remember {
        RouteSearch(context).apply {
            setRouteSearchListener(object : RouteSearch.OnRouteSearchListener {
                override fun onBusRouteSearched(result: BusRouteResult?, errorCode: Int) = Unit

                override fun onDriveRouteSearched(result: DriveRouteResult?, errorCode: Int) {
                    if (errorCode != 1000) {
                        onRouteStatusChanged("路线规划失败，错误码：$errorCode")
                        return
                    }
                    val path = result?.paths?.firstOrNull()
                    if (path == null) {
                        onRouteStatusChanged("未找到可用路线")
                        return
                    }
                    val points = path.steps
                        .flatMap { step -> step.polyline.orEmpty() }
                        .map { point -> LatLng(point.latitude, point.longitude) }
                    if (points.isEmpty()) {
                        onRouteStatusChanged("路线返回为空")
                        return
                    }

                    val amap = mapView.map
                    routePolyline?.remove()
                    routePolyline = amap.addPolyline(
                        PolylineOptions()
                            .addAll(points)
                            .color(0xFF1E6BFF.toInt())
                            .width(14f),
                    )
                    onRouteStatusChanged(
                        "驾车距离：${formatDistance(path.distance.toDouble())}\n预计时间：${formatDuration(path.duration.toLong())}",
                    )
                }

                override fun onWalkRouteSearched(result: WalkRouteResult?, errorCode: Int) = Unit

                override fun onRideRouteSearched(result: RideRouteResult?, errorCode: Int) = Unit
            })
        }
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
            amap.uiSettings.isZoomControlsEnabled = true
            amap.uiSettings.isMyLocationButtonEnabled = false
            amap.uiSettings.isCompassEnabled = true
            amap.uiSettings.isScaleControlsEnabled = true
            amap.setOnMapClickListener { latLng ->
                if (pickModeEnabled) {
                    onMapPicked(latLng.latitude, latLng.longitude)
                }
            }

            val style = MyLocationStyle()
                .myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE)
                .interval(2000L)
            amap.myLocationStyle = style
            amap.setOnMyLocationChangeListener { location ->
                if (location != null) {
                    onLocationChanged(location.latitude, location.longitude, location.accuracy)
                }
            }
            amap.isMyLocationEnabled = false
            amap.isMyLocationEnabled = hasLocationPermission

            if (destination != null) {
                val target = LatLng(destination.latitude, destination.longitude)
                destinationMarker?.remove()
                destinationMarker = amap.addMarker(
                    MarkerOptions()
                        .position(target)
                        .title("目的位置")
                        .snippet(destination.sourceLabel),
                )
                if (currentLocation == null) {
                    routePolyline?.remove()
                    routePolyline = null
                    lastRouteKey = null
                }
            } else {
                destinationMarker?.remove()
                destinationMarker = null
                routePolyline?.remove()
                routePolyline = null
                lastRouteKey = null
            }

            if (currentLocation != null && destination != null) {
                val routeKey = listOf(
                    currentLocation.latitude.roundKey(),
                    currentLocation.longitude.roundKey(),
                    destination.latitude.roundKey(),
                    destination.longitude.roundKey(),
                ).joinToString("|")
                if (routeKey != lastRouteKey) {
                    lastRouteKey = routeKey
                    onRouteStatusChanged("正在规划路线")
                    val fromAndTo = RouteSearch.FromAndTo(
                        LatLonPoint(currentLocation.latitude, currentLocation.longitude),
                        LatLonPoint(destination.latitude, destination.longitude),
                    )
                    val query = RouteSearch.DriveRouteQuery(
                        fromAndTo,
                        RouteSearch.DrivingDefault,
                        null,
                        null,
                        "",
                    )
                    routeSearch.calculateDriveRouteAsyn(query)
                }
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
    northingText: String,
    eastingText: String,
    hemisphere: Hemisphere,
): Result<DestinationPoint> {
    return runCatching {
        when (inputMode) {
            DestinationInputMode.LatLon -> {
                val latitude = latitudeText.toDouble()
                val longitude = longitudeText.toDouble()
                require(latitude in -90.0..90.0) { "纬度范围应为 -90 到 90" }
                require(longitude >= -180.0 && longitude < 180.0) { "经度范围应为 -180 到 180" }
                DestinationPoint(latitude, longitude, "经纬度输入")
            }

            DestinationInputMode.Utm -> {
                val coordinate = CoordinateMath.utmToLatLon(
                    northing = northingText.toDouble(),
                    easting = eastingText.toDouble(),
                    hemisphere = hemisphere,
                )
                DestinationPoint(coordinate.latitude, coordinate.longitude, "UTM 输入")
            }
        }
    }
}

private fun Double.roundKey(): String = String.format(Locale.US, "%.6f", this)

private fun formatDistance(distanceMeters: Double): String {
    return if (distanceMeters >= 1000.0) {
        "${fmt(distanceMeters / 1000.0, 2)} km"
    } else {
        "${fmt(distanceMeters, 0)} m"
    }
}

private fun formatDuration(seconds: Long): String {
    val minutes = (seconds / 60.0).roundToInt()
    if (minutes < 60) return "${minutes} 分钟"
    val hours = minutes / 60
    val remainMinutes = minutes % 60
    return "${hours} 小时 ${remainMinutes} 分钟"
}

private fun startAmapNavigation(
    context: android.content.Context,
    start: LocationSnapshot?,
    destination: DestinationPoint,
): String {
    val uri = buildString {
        append("androidamap://route/plan/?sourceApplication=工程测算工具")
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
