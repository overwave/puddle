package dev.overwave

import kotlin.math.*

const val RADIUS = 6378137.0
const val SUBRADIUS = 1 / RADIUS

const val EQUATOR = 2 * Math.PI * RADIUS
const val SUBEQUATOR = 1 / EQUATOR
const val HALF_EQUATOR = EQUATOR / 2

const val E = 0.0818191908426
const val E2 = E * E
const val E4 = E2 * E2
const val E6 = E4 * E2
const val E8 = E4 * E4
const val C_180_PI = 180 / Math.PI
const val D2 = E2 / 2 + 5 * E4 / 24 + E6 / 12 + 13 * E8 / 360
const val D4 = 7 * E4 / 48 + 29 * E6 / 240 + 811 * E8 / 11520
const val D6 = 7 * E6 / 120 + 81 * E8 / 1120
const val D8 = 4279 * E8 / 161280

fun xToLongitude(x: Double) = x * SUBRADIUS * C_180_PI

fun yToLatitude(y: Double): Double {
    val phi = Math.PI * 0.5 - 2 * atan(1 / exp(y * SUBRADIUS))
    val latitude = phi + D2 * sin(2 * phi) + D4 * sin(4 * phi) + D6 * sin(6 * phi) + D8 * sin(8 * phi)
    return latitude * C_180_PI
}

fun fromGlobalPixels(tileCoordinate: TileCoordinate): WorldCoordinate {
    val pixelsPerMeter = 2.0.pow(tileCoordinate.zoom + 8.0) * SUBEQUATOR
    val longitude = xToLongitude(tileCoordinate.x / pixelsPerMeter - HALF_EQUATOR)
    val latitude = yToLatitude(HALF_EQUATOR - tileCoordinate.y / pixelsPerMeter)
    return WorldCoordinate(latitude, longitude)
}

fun toGlobalPixels(point: WorldCoordinate, zoom: Int): TileCoordinate {
    val p = 2.0.pow(zoom + 7.0)

    val beta = Math.PI * point.lat / 180.0
    val phi = (1.0 - E * sin(beta)) / (1.0 + E * sin(beta))
    val theta = tan(Math.PI / 4.0 + beta / 2.0) * phi.pow(E / 2.0)

    val x = p * (1.0 + point.lon / 180.0)
    val y = p * (1.0 - ln(theta) / Math.PI)
    return TileCoordinate(x.roundToLong(), y.roundToLong(), zoom)
}
