package dev.overwave

data class WorldCoordinate(
    val lat: Double,
    val lon: Double
) {
    operator fun minus(other: WorldCoordinate) = WorldCoordinate(lat - other.lat, lon - other.lon)
    operator fun plus(other: WorldCoordinate) = WorldCoordinate(lat + other.lat, lon + other.lon)
}