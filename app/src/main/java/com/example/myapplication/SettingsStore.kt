package com.example.myapplication

import android.content.Context

data class EllipsoidParameters(
    val semiMajorAxis: Double,
    val semiMinorAxis: Double,
    val inverseFlattening: Double,
) {
    companion object {
        val Wgs84 = EllipsoidParameters(6378137.0, 6356752.314245179, 298.257223563)
    }
}

enum class EllipsoidPreset(
    val label: String,
    val semiMajorAxis: Double,
    val semiMinorAxis: Double,
    val inverseFlattening: Double,
) {
    Wgs84("WGS84", 6378137.0, 6356752.314245179, 298.257223563),
    NorthSahara1959("North Sahara 1959", 6378249.145, 6356514.869549776, 293.465),
    Sk42("SK42", 6378245.0, 6356863.018773047, 298.3),
    Custom("自定义", 6378137.0, 6356752.314245179, 298.257223563),
}

enum class ProjectionMode(val label: String) {
    Utm("UTM"),
    GaussKruger("高斯投影"),
}

enum class DefaultMapInput(val label: String) {
    Utm("UTM"),
    LatLon("经纬度"),
}

enum class UtmInputMode(val label: String) {
    SplitZone("带号拆分"),
    MergedZone("带号合并"),
}

enum class NorthReference(val label: String) {
    CoordinateNorth("坐标北"),
    GridNorth("网格北"),
}

data class AppSettings(
    val ellipsoidPreset: EllipsoidPreset = EllipsoidPreset.Wgs84,
    val semiMajorAxis: Double = EllipsoidPreset.Wgs84.semiMajorAxis,
    val semiMinorAxis: Double = EllipsoidPreset.Wgs84.semiMinorAxis,
    val inverseFlattening: Double = EllipsoidPreset.Wgs84.inverseFlattening,
    val projectionMode: ProjectionMode = ProjectionMode.Utm,
    val defaultMilScale: MilScale = MilScale.Mil6000,
    val defaultMapInput: DefaultMapInput = DefaultMapInput.Utm,
    val utmInputMode: UtmInputMode = UtmInputMode.SplitZone,
    val northReference: NorthReference = NorthReference.GridNorth,
    val historyLimit: Int = 100,
) {
    fun ellipsoidParameters(): EllipsoidParameters {
        return EllipsoidParameters(semiMajorAxis, semiMinorAxis, inverseFlattening)
    }
}

object SettingsStore {
    private const val PREF_NAME = "tool_settings"
    private const val KEY_ELLIPSOID_PRESET = "ellipsoid_preset"
    private const val KEY_SEMI_MAJOR_AXIS = "semi_major_axis"
    private const val KEY_SEMI_MINOR_AXIS = "semi_minor_axis"
    private const val KEY_INVERSE_FLATTENING = "inverse_flattening"
    private const val KEY_PROJECTION_MODE = "projection_mode"
    private const val KEY_DEFAULT_MIL_SCALE = "default_mil_scale"
    private const val KEY_DEFAULT_MAP_INPUT = "default_map_input"
    private const val KEY_UTM_INPUT_MODE = "utm_input_mode"
    private const val KEY_NORTH_REFERENCE = "north_reference"
    private const val KEY_HISTORY_LIMIT = "history_limit"

    fun load(context: Context): AppSettings {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return AppSettings(
            ellipsoidPreset = EllipsoidPreset.Wgs84,
            semiMajorAxis = EllipsoidPreset.Wgs84.semiMajorAxis,
            semiMinorAxis = EllipsoidPreset.Wgs84.semiMinorAxis,
            inverseFlattening = EllipsoidPreset.Wgs84.inverseFlattening,
            projectionMode = prefs.getString(KEY_PROJECTION_MODE, null).toEnumOrDefault(ProjectionMode.Utm),
            defaultMilScale = prefs.getString(KEY_DEFAULT_MIL_SCALE, null).toEnumOrDefault(MilScale.Mil6000),
            defaultMapInput = prefs.getString(KEY_DEFAULT_MAP_INPUT, null).toEnumOrDefault(DefaultMapInput.Utm),
            utmInputMode = prefs.getString(KEY_UTM_INPUT_MODE, null).toEnumOrDefault(UtmInputMode.SplitZone),
            northReference = prefs.getString(KEY_NORTH_REFERENCE, null).toEnumOrDefault(NorthReference.GridNorth),
            historyLimit = prefs.getInt(KEY_HISTORY_LIMIT, 100).coerceIn(1, 10000),
        )
    }

    fun save(context: Context, settings: AppSettings) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ELLIPSOID_PRESET, EllipsoidPreset.Wgs84.name)
            .putString(KEY_SEMI_MAJOR_AXIS, EllipsoidPreset.Wgs84.semiMajorAxis.toString())
            .putString(KEY_SEMI_MINOR_AXIS, EllipsoidPreset.Wgs84.semiMinorAxis.toString())
            .putString(KEY_INVERSE_FLATTENING, EllipsoidPreset.Wgs84.inverseFlattening.toString())
            .putString(KEY_PROJECTION_MODE, settings.projectionMode.name)
            .putString(KEY_DEFAULT_MIL_SCALE, settings.defaultMilScale.name)
            .putString(KEY_DEFAULT_MAP_INPUT, settings.defaultMapInput.name)
            .putString(KEY_UTM_INPUT_MODE, settings.utmInputMode.name)
            .putString(KEY_NORTH_REFERENCE, settings.northReference.name)
            .putInt(KEY_HISTORY_LIMIT, settings.historyLimit.coerceIn(1, 10000))
            .apply()
    }
}

private inline fun <reified T : Enum<T>> String?.toEnumOrDefault(default: T): T {
    if (this == null) return default
    return runCatching { enumValueOf<T>(this) }.getOrDefault(default)
}
