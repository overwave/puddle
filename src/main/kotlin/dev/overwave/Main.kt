package dev.overwave

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_INT_ARGB
import java.io.File
import kotlin.math.*

val mapper = jacksonObjectMapper().registerKotlinModule()

const val LAT_600M = 0.005543 / 2
const val LON_600M = 0.010216 / 2

var millis = 0

fun main() {
    for (i in 0..19) {
        val tileCoordinate = toGlobalPixels(WorldCoordinate(55.733776, 37.587936), i)
        val worldCoordinate = fromGlobalPixels(tileCoordinate)

//        println(tileCoordinate)
//        println(worldCoordinate)
    }

    val polygons = readPolygonsFromFile()
    val tilesToRefresh = getTilesToRefresh(polygons)

    for (tile in tilesToRefresh) {
        refreshTile(tile, polygons)
    }

    System.err.println("$millis millis spent")
}

fun refreshTile(tile: TileCoordinate, polygons: List<Poly>) {
    val time = System.currentTimeMillis()

    val filteredPolygons = polygons.filter {
        val leftBoundary = it.center - WorldCoordinate(LAT_600M, LON_600M)
        val rightBoundary = it.center + WorldCoordinate(LAT_600M, LON_600M)
        it.center.lat in leftBoundary.lat..rightBoundary.lat && it.center.lon in leftBoundary.lon..rightBoundary.lon
    }
    millis += 400 // поход в базу с фильтром

    val xFrom = tile.x / 256 * 256
    val yFrom = tile.y / 256 * 256

    val image = BufferedImage(256, 256, TYPE_INT_ARGB)
    var dirty = false

    for (x in 0..255) {
        points@ for (y in 0..255) {
            for (polygon in filteredPolygons) {
                if (polygon.contains((xFrom + x).toDouble(), (yFrom + y).toDouble())) {
                    dirty = true

                    if (polygon.type == "COMB") {
                        image.setRGB(x, y, 123)
                    } else if (polygon.type == "PUDDLE") {
                        image.setRGB(x, y, 5678)
                        continue@points
                    }
                }
            }
        }
    }

    if (dirty) {
        millis += 1000 // сохранение PNG в S3
    }
    val seconds = (System.currentTimeMillis() - time) / 1000
    println("saved a tile, took $seconds")
}

private fun getTilesToRefresh(polygons: List<Poly>): Set<TileCoordinate> {
    val zoomFrom = 0
    val zoomTo = 19

    val tilesToRefresh = mutableSetOf<TileCoordinate>()
    for ((index, poly) in polygons.withIndex()) {

        val leftCorner = poly.center - WorldCoordinate(LAT_600M, LON_600M)
        val rightCorner = poly.center + WorldCoordinate(LAT_600M, LON_600M)

        for (zoom in zoomFrom..zoomTo) {
            val leftTile = toGlobalPixels(leftCorner, zoom)
            val rightTile = toGlobalPixels(rightCorner, zoom)

            val xRange = min(leftTile.x / 256, rightTile.x / 256)..max(leftTile.x / 256, rightTile.x / 256)
            val yRange = min(leftTile.y / 256, rightTile.y / 256)..max(leftTile.y / 256, rightTile.y / 256)

            for (x in xRange) {
                for (y in yRange) {
                    tilesToRefresh.add(TileCoordinate(x, y, zoom))
                }
            }
        }

        println("$index/${polygons.size}, ${tilesToRefresh.size} added")
    }
    return tilesToRefresh
}

private fun readPolygonsFromFile(): List<Poly> {
    val puddlesFile: String = File("./puddles_fixed.json").readText()
    val puddleJsons = mapper.readValue(puddlesFile, object : TypeReference<List<PuddleJson>>() {})
    val puddles = puddleJsons.map {
        val points = it.geometry.map { (x, y) -> WorldCoordinate(x, y) }
        Poly("PUDDLE", it.id, WorldCoordinate(it.lat, it.lon), points)
    }

    val combsFile: String = File("./combs_fixed.json").readText()
    val combJsons = mapper.readValue(combsFile, object : TypeReference<List<CombJson>>() {})
    val combs = combJsons.map {
        val points = it.geometry.first().map { (x, y) -> WorldCoordinate(x, y) }
        val center = WorldCoordinate(
            points.map(WorldCoordinate::lat).average(),
            points.map(WorldCoordinate::lon).average()
        )
        Poly("COMB", it.hexIndex, center, points)
    }

    return puddles + combs
}