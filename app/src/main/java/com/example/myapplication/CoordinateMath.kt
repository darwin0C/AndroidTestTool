package com.example.myapplication

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan
import java.util.Locale

data class UtmCoordinate(
    val northing: Double,
    val easting: Double,
    val zone: Int,
    val hemisphere: Hemisphere,
)

data class GeoCoordinate(
    val latitude: Double,
    val longitude: Double,
)

data class DistanceResult(
    val horizontalDistance: Double,
    val spatialDistance: Double,
    val azimuthDegrees: Double,
    val elevationDegrees: Double,
)

data class ForwardSolveResult(
    val x: Double,
    val y: Double,
    val h: Double,
    val horizontalDistance: Double,
    val spatialDistance: Double,
    val azimuthDegrees: Double,
    val elevationDegrees: Double,
)

data class IntersectionResult(
    val x: Double,
    val y: Double,
    val distanceFromA: Double,
    val distanceFromB: Double,
    val azimuthFromADegrees: Double,
    val azimuthFromBDegrees: Double,
)

enum class Hemisphere(val label: String) {
    North("北半球"),
    South("南半球"),
}

enum class ChecksumMode(val label: String) {
    Xor8("XOR8"),
    Sum8("SUM8"),
}

enum class MilScale(val label: String, val units: Int) {
    Mil6000("6000 密位", 6000),
    Mil6400("6400 密位", 6400),
}

object CoordinateMath {
    fun decimalToDms(decimal: Double): Double {
        val sign = if (decimal < 0) -1 else 1
        val value = abs(decimal)
        val degrees = floor(value)
        val minutesFull = (value - degrees) * 60.0
        val minutes = floor(minutesFull)
        val seconds = (minutesFull - minutes) * 60.0
        return sign * (degrees + minutes / 100.0 + seconds / 10000.0)
    }

    fun decimalToDmsText(decimal: Double, secondsDigits: Int = 3): String {
        val sign = if (decimal < 0) "-" else ""
        val value = abs(decimal)
        var degrees = floor(value).toInt()
        val minutesFull = (value - degrees) * 60.0
        var minutes = floor(minutesFull).toInt()
        var seconds = (minutesFull - minutes) * 60.0

        val scale = 10.0.pow(secondsDigits)
        seconds = (seconds * scale).roundToInt() / scale
        if (seconds >= 60.0) {
            seconds = 0.0
            minutes += 1
        }
        if (minutes >= 60) {
            minutes = 0
            degrees += 1
        }

        val secondsText = "%0${secondsDigits + 3}.${secondsDigits}f".format(Locale.US, seconds)
        return "$sign$degrees°$minutes′$secondsText″"
    }

    fun dmsToDecimal(dms: Double): Double {
        val sign = if (dms < 0) -1 else 1
        val value = abs(dms)
        val degrees = floor(value)
        val minutesFull = (value - degrees) * 100.0
        val minutes = floor(minutesFull)
        val seconds = (minutesFull - minutes) * 100.0
        return sign * (degrees + minutes / 60.0 + seconds / 3600.0)
    }

    fun dmsTextToDecimal(text: String): Double {
        val value = text.trim()
        require(value.isNotEmpty()) { "请输入度分秒" }

        val sign = if (value.startsWith("-")) -1.0 else 1.0
        val unsigned = value.removePrefix("+").removePrefix("-")
        val parts = unsigned
            .replace("度", "°")
            .replace("分", "′")
            .replace("'", "′")
            .replace("’", "′")
            .replace("′", " ")
            .replace("秒", "″")
            .replace("\"", "″")
            .replace("”", "″")
            .replace("″", " ")
            .replace("°", " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        require(parts.size in 1..3) { "格式应为 11°22′33.444″" }
        val degrees = parts[0].toDoubleOrNull() ?: throw IllegalArgumentException("度格式有误")
        val minutes = parts.getOrNull(1)?.toDoubleOrNull() ?: 0.0
        val seconds = parts.getOrNull(2)?.toDoubleOrNull() ?: 0.0
        require(minutes in 0.0..<60.0) { "分应小于 60" }
        require(seconds in 0.0..<60.0) { "秒应小于 60" }
        return sign * (degrees + minutes / 60.0 + seconds / 3600.0)
    }

    fun latLonToUtm(
        latitude: Double,
        longitude: Double,
        targetZone: Int? = null,
        ellipsoid: EllipsoidParameters = EllipsoidParameters.Wgs84,
    ): UtmCoordinate {
        require(latitude in -90.0..90.0) { "纬度范围应为 -90 到 90" }
        require(longitude in -180.0..180.0) { "经度范围应为 -180 到 180" }
        if (targetZone != null) {
            require(targetZone in 1..60) { "基准带号应为 1 到 60" }
        }
        require(ellipsoid.semiMajorAxis > 0.0) { "椭球长半轴应大于 0" }
        require(ellipsoid.inverseFlattening > 0.0) { "椭球扁率倒数应大于 0" }

        val falseNorthing = if (latitude >= 0.0) 0.0 else 10000000.0
        val lat = abs(latitude)
        val semiMajorAxis = ellipsoid.semiMajorAxis
        val semiMinorAxis = ellipsoid.semiMinorAxis
        val flattening = (semiMajorAxis - semiMinorAxis) / semiMajorAxis
        val eccentricitySquared = 2 * flattening - flattening * flattening
        val scale = 0.9996

        val rib = targetZone ?: zoneNumber(longitude)
        val longitudeOrigin = rib * 6.0 - 183.0

        val latRad = lat * PI / 180.0
        val lonRad = longitude * PI / 180.0
        val lonOriginRad = longitudeOrigin * PI / 180.0
        val secondEccentricitySquared = eccentricitySquared / (1.0 - eccentricitySquared)

        val v = semiMajorAxis / sqrt(1.0 - eccentricitySquared * sin(latRad).pow(2.0))
        val t = tan(latRad).pow(2.0)
        val c = secondEccentricitySquared * cos(latRad).pow(2.0)
        val a = cos(latRad) * (lonRad - lonOriginRad)
        val m = semiMajorAxis * (
            (1.0 - eccentricitySquared / 4.0 - 3.0 * eccentricitySquared.pow(2.0) / 64.0 - 5.0 * eccentricitySquared.pow(3.0) / 256.0) * latRad -
                (3.0 * eccentricitySquared / 8.0 + 3.0 * eccentricitySquared.pow(2.0) / 32.0 + 45.0 * eccentricitySquared.pow(3.0) / 1024.0) * sin(2.0 * latRad) +
                (15.0 * eccentricitySquared.pow(2.0) / 256.0 + 45.0 * eccentricitySquared.pow(3.0) / 1024.0) * sin(4.0 * latRad) -
                (35.0 * eccentricitySquared.pow(3.0) / 3072.0) * sin(6.0 * latRad)
            )

        val easting = scale * v * (
            a + (1.0 - t + c) * a.pow(3.0) / 6.0 +
                (5.0 - 18.0 * t + t * t + 72.0 * c - 58.0 * secondEccentricitySquared) * a.pow(5.0) / 120.0
            ) + 500000.0 + rib * 1000000.0
        val northing = scale * (
            m + v * tan(latRad) * (
                a.pow(2.0) / 2.0 +
                    (5.0 - t + 9.0 * c + 4.0 * c * c) * a.pow(4.0) / 24.0 +
                    (61.0 - 58.0 * t + t * t + 600.0 * c - 330.0 * secondEccentricitySquared) * a.pow(6.0) / 720.0
                )
            ) + falseNorthing

        return UtmCoordinate(
            northing = northing,
            easting = easting,
            zone = rib,
            hemisphere = if (latitude >= 0.0) Hemisphere.North else Hemisphere.South,
        )
    }

    fun convertUtmToZone(
        northing: Double,
        easting: Double,
        hemisphere: Hemisphere,
        targetZone: Int,
        ellipsoid: EllipsoidParameters = EllipsoidParameters.Wgs84,
    ): UtmCoordinate {
        require(targetZone in 1..60) { "基准带号应为 1 到 60" }
        val geo = utmToLatLon(northing, easting, hemisphere, ellipsoid)
        return latLonToUtm(geo.latitude, geo.longitude, targetZone, ellipsoid)
    }

    fun utmToLatLon(
        northing: Double,
        easting: Double,
        hemisphere: Hemisphere,
        ellipsoid: EllipsoidParameters = EllipsoidParameters.Wgs84,
    ): GeoCoordinate {
        require(isProjectedCoordinateValid(northing, easting)) { "投影坐标超出常用 UTM 范围" }
        require(ellipsoid.semiMajorAxis > 0.0) { "椭球长半轴应大于 0" }
        require(ellipsoid.inverseFlattening > 0.0) { "椭球扁率倒数应大于 0" }

        var x = northing
        if (hemisphere == Hemisphere.South) {
            x = 10000000.0 - x
        }
        val semiMajorAxis = ellipsoid.semiMajorAxis
        val semiMinorAxis = ellipsoid.semiMinorAxis
        val rib = (easting / 1000000.0).toInt()
        require(rib in 1..60) { "UTM 带号应为 1 到 60" }
        val longitudeOrigin = ((rib - 30) * 6.0 - 3.0) * PI / 180.0
        val y = 500000.0 + rib * 1000000.0 - easting
        val scale = 0.9996
        val eccentricity = sqrt(1.0 - (semiMinorAxis * semiMinorAxis) / (semiMajorAxis * semiMajorAxis))
        val meridionalArc = x / scale
        val mu = meridionalArc / (semiMajorAxis * (1.0 - eccentricity.pow(2.0) / 4.0 - 3.0 * eccentricity.pow(4.0) / 64.0 - 5.0 * eccentricity.pow(6.0) / 256.0))
        val e1 = (1.0 - sqrt(1.0 - eccentricity.pow(2.0))) / (1.0 + sqrt(1.0 - eccentricity.pow(2.0)))
        val footpoint = mu +
            (3.0 * e1 / 2.0 - 27.0 * e1.pow(3.0) / 32.0) * sin(2.0 * mu) +
            (21.0 * e1.pow(2.0) / 16.0 - 55.0 * e1.pow(4.0) / 32.0) * sin(4.0 * mu) +
            (151.0 * e1.pow(3.0) / 96.0) * sin(6.0 * mu) +
            (1097.0 * e1.pow(4.0) / 512.0) * sin(8.0 * mu)

        val e2 = eccentricity.pow(2.0) / (1.0 - eccentricity.pow(2.0))
        val c1 = e2 * cos(footpoint).pow(2.0)
        val t1 = tan(footpoint).pow(2.0)
        val r1 = semiMajorAxis * (1.0 - eccentricity.pow(2.0)) / (1.0 - (eccentricity * sin(footpoint)).pow(2.0)).pow(1.5)
        val n1 = semiMajorAxis / sqrt(1.0 - (eccentricity * sin(footpoint)).pow(2.0))
        val d = y / (n1 * scale)

        var latitude = footpoint - n1 * tan(footpoint) / r1 * (
            d.pow(2.0) / 2.0 -
                (5.0 + 3.0 * t1 + 10.0 * c1 - 4.0 * c1 * c1 - 9.0 * e2) * d.pow(4.0) / 24.0 +
                (61.0 + 90.0 * t1 + 298.0 * c1 + 45.0 * t1 * t1 - 3.0 * c1 * c1 - 252.0 * e2) * d.pow(6.0) / 720.0
            )
        if (hemisphere == Hemisphere.South) {
            latitude *= -1.0
        }

        val longitude = longitudeOrigin - (
            d -
                (1.0 + 2.0 * t1 + c1) * d.pow(3.0) / 6.0 +
                (5.0 - 2.0 * c1 + 28.0 * t1 - 3.0 * c1 * c1 + 8.0 * e2 + 24.0 * t1 * t1) * d.pow(5.0) / 120.0
            ) / cos(footpoint)

        return GeoCoordinate(
            latitude = latitude * 180.0 / PI,
            longitude = longitude * 180.0 / PI,
        )
    }

    fun zoneNumber(longitude: Double): Int {
        val zone = if (longitude >= 0.0) {
            (longitude / 6.0).toInt() + 31
        } else {
            ((longitude + 180.0) / 6.0).toInt() + 1
        }
        return zone.coerceIn(1, 60)
    }

    fun latitudeBand(latitude: Double): Char? {
        if (latitude < -80.0 || latitude > 84.0) return null
        if (latitude >= 72.0) return 'X'
        val raw = ((latitude + 80.0) / 8.0).toInt() + 'C'.code
        val adjusted = when {
            raw < 'I'.code -> raw
            raw < 'N'.code -> raw + 1
            else -> raw + 2
        }
        return adjusted.toChar()
    }

    fun distance(
        x1: Double,
        y1: Double,
        h1: Double,
        x2: Double,
        y2: Double,
        h2: Double,
    ): DistanceResult {
        val dx = x2 - x1
        val dy = y2 - y1
        val dh = h2 - h1
        val horizontal = sqrt(dx * dx + dy * dy)
        val spatial = sqrt(dx * dx + dy * dy + dh * dh)
        val azimuth = normalizeDegrees(atan2(dy, dx) * 180.0 / PI)
        val elevation = atan2(dh, horizontal) * 180.0 / PI
        return DistanceResult(horizontal, spatial, azimuth, elevation)
    }

    fun gridConvergenceDegrees(
        northing: Double,
        easting: Double,
        hemisphere: Hemisphere = Hemisphere.North,
        ellipsoid: EllipsoidParameters = EllipsoidParameters.Wgs84,
    ): Double {
        val geo = utmToLatLon(northing, easting, hemisphere, ellipsoid)
        val zone = (easting / 1000000.0).toInt()
        require(zone in 1..60) { "UTM 带号应为 1 到 60" }

        val latitudeRad = geo.latitude * PI / 180.0
        val longitudeRad = geo.longitude * PI / 180.0
        val centralMeridianRad = (zone * 6.0 - 183.0) * PI / 180.0
        val convergenceRad = atan(tan(longitudeRad - centralMeridianRad) * sin(latitudeRad))
        return convergenceRad * 180.0 / PI
    }

    fun degreesToMils(degrees: Double, scale: MilScale, signed: Boolean = false): Double {
        val normalized = if (signed) degrees else normalizeDegrees(degrees)
        return normalized / 360.0 * scale.units
    }

    fun milsToDegrees(mils: Double, scale: MilScale, signed: Boolean = false): Double {
        val degrees = mils / scale.units * 360.0
        return if (signed && degrees > 180.0) {
            degrees - 360.0
        } else if (signed) {
            degrees
        } else {
            normalizeDegrees(degrees)
        }
    }

    fun forwardSolveByMils(
        x: Double,
        y: Double,
        h: Double,
        azimuthMil: Double,
        elevationMil: Double,
        distance: Double,
        scale: MilScale,
    ): ForwardSolveResult {
        require(distance >= 0.0) { "距离应大于或等于 0" }
        require(azimuthMil in 0.0..scale.units.toDouble()) { "方位密位应在 0 到 ${scale.units} 范围内" }
        val elevationLimit = scale.units / 4.0
        require(elevationMil in -elevationLimit..elevationLimit) { "高低密位应在 -${elevationLimit.toInt()} 到 ${elevationLimit.toInt()} 范围内" }
        val azimuthDegrees = milsToDegrees(azimuthMil, scale)
        val elevationDegrees = milsToDegrees(elevationMil, scale, signed = true)
        require(elevationDegrees in -90.0..90.0) { "高低角范围应为 -90° 到 90°" }

        val azimuthRad = azimuthDegrees * PI / 180.0
        val elevationRad = elevationDegrees * PI / 180.0
        val horizontal = distance * cos(elevationRad)
        val dx = horizontal * cos(azimuthRad)
        val dy = horizontal * sin(azimuthRad)
        val dh = distance * sin(elevationRad)

        return ForwardSolveResult(
            x = x + dx,
            y = y + dy,
            h = h + dh,
            horizontalDistance = horizontal,
            spatialDistance = distance,
            azimuthDegrees = azimuthDegrees,
            elevationDegrees = elevationDegrees,
        )
    }

    fun intersectionByMils(
        ax: Double,
        ay: Double,
        aAzimuthMil: Double,
        bx: Double,
        by: Double,
        bAzimuthMil: Double,
        scale: MilScale,
    ): IntersectionResult {
        require(aAzimuthMil in 0.0..scale.units.toDouble()) { "A 点观测方位密位应在 0 到 ${scale.units} 范围内" }
        require(bAzimuthMil in 0.0..scale.units.toDouble()) { "B 点观测方位密位应在 0 到 ${scale.units} 范围内" }

        val aAzimuthDegrees = milsToDegrees(aAzimuthMil, scale)
        val bAzimuthDegrees = milsToDegrees(bAzimuthMil, scale)
        val aRad = aAzimuthDegrees * PI / 180.0
        val bRad = bAzimuthDegrees * PI / 180.0
        val auX = cos(aRad)
        val auY = sin(aRad)
        val buX = cos(bRad)
        val buY = sin(bRad)
        val det = cross(auX, auY, buX, buY)
        require(abs(det) > 1.0e-10) { "两条观测方位线平行或接近平行，无法交会" }

        val dx = bx - ax
        val dy = by - ay
        val distanceFromA = cross(dx, dy, buX, buY) / det
        val distanceFromB = cross(dx, dy, auX, auY) / det
        require(distanceFromA >= 0.0 && distanceFromB >= 0.0) { "交会点不在两点观测方位的前方" }

        return IntersectionResult(
            x = ax + distanceFromA * auX,
            y = ay + distanceFromA * auY,
            distanceFromA = distanceFromA,
            distanceFromB = distanceFromB,
            azimuthFromADegrees = aAzimuthDegrees,
            azimuthFromBDegrees = bAzimuthDegrees,
        )
    }

    fun checksum(input: String, mode: ChecksumMode): String {
        val bytes = parseHexBytes(input)
        val value = when (mode) {
            ChecksumMode.Xor8 -> bytes.fold(0) { acc, byte -> acc xor byte }
            ChecksumMode.Sum8 -> bytes.sum() and 0xFF
        }
        return value.toString(16).uppercase().padStart(2, '0')
    }

    fun isProjectedCoordinateValid(northing: Double, easting: Double): Boolean {
        val zone = (easting.roundToInt() / 1000000)
        val inZone = zone in 1..60
        val eastingOffset = abs(easting.roundToInt() % 1000000 - 500000)
        return northing.isFinite() && easting.isFinite() && inZone && eastingOffset <= 335000
    }

    private fun normalizeDegrees(value: Double): Double {
        var normalized = value % 360.0
        if (normalized < 0.0) normalized += 360.0
        return normalized
    }

    private fun cross(ax: Double, ay: Double, bx: Double, by: Double): Double {
        return ax * by - ay * bx
    }

    private fun parseHexBytes(input: String): List<Int> {
        val compact = input.filterNot { it.isWhitespace() || it == ',' || it == ':' || it == '-' }
        require(compact.isNotBlank()) { "请输入十六进制数据" }
        require(compact.length % 2 == 0) { "十六进制字符数量必须为偶数" }
        return compact.chunked(2).map { chunk ->
            chunk.toIntOrNull(16) ?: throw IllegalArgumentException("包含非十六进制字符")
        }
    }

    private fun crc16Modbus(bytes: List<Int>): Int {
        var crc = 0xFFFF
        bytes.forEach { byte ->
            crc = crc xor byte
            repeat(8) {
                crc = if ((crc and 0x0001) != 0) {
                    (crc ushr 1) xor 0xA001
                } else {
                    crc ushr 1
                }
            }
        }
        return crc and 0xFFFF
    }
}
