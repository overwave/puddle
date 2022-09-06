package dev.overwave

import kotlin.math.*

data class TileCoordinate(
    val x: Long,
    val y: Long,
    val zoom: Int
) {
    fun multiplied() = TileCoordinate(x * 256, y * 256, zoom)
    fun corner() = TileCoordinate(x + 1, y + 1, zoom)
}