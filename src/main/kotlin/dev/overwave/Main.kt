package dev.overwave

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.awt.Color
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_INT_ARGB
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.max
import kotlin.math.min

val mapper = jacksonObjectMapper().registerKotlinModule()

const val LAT_600M = 0.005543 / 2
const val LON_600M = 0.010216 / 2

var millis = 0
var saves = 0

fun main() {
    for (i in 0..19) {
//        val tileCoordinate = toGlobalPixels(WorldCoordinate(55.733776, 37.587936), i)
//        val worldCoordinate = fromGlobalPixels(tileCoordinate)
//
//        println(tileCoordinate)
//        println(worldCoordinate)
    }

    val polygons = readPolygonsFromFile()
    polygons.stream()

    val tilesToRefresh = getTilesToRefresh(polygons)

    for (tile in tilesToRefresh) {
        refreshTile(tile, polygons)
    }

    System.err.println("$millis millis spent")
    System.err.println("$saves")
}

private fun getTilesToRefresh(polygons: List<Poly>): Set<TileCoordinate> {
    val zoomFrom = 9
    val zoomTo = 18

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

//    val puddlesF = puddles.filter { it.center.lat < 55.962257 && it.center.lat > 55.551666 && it.center.lon < 37.881505 && it.center.lon > 37.288189 }
//        .map { it.points }

//    val writeValueAsString = jacksonObjectMapper().writeValueAsString(puddlesF)
//    File("dima.json").writeText(writeValueAsString)

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

    return combs + puddles
}

fun refreshTile(tile: TileCoordinate, polygons: List<Poly>) {
    val time = System.currentTimeMillis()
    val upperLeftTileCoordinate = fromGlobalPixels(tile.x * 256, tile.y * 256, tile.zoom)
    val lowerRightTileCoordinate = fromGlobalPixels((tile.x + 1) * 256, (tile.y + 1) * 256, tile.zoom)

    val filteredPolygons = polygons.filter {
        val leftBoundary = it.center - WorldCoordinate(LAT_600M, LON_600M)
        val rightBoundary = it.center + WorldCoordinate(LAT_600M, LON_600M)

        max(lowerRightTileCoordinate.lat, leftBoundary.lat) <
                min(upperLeftTileCoordinate.lat, rightBoundary.lat) &&
                max(upperLeftTileCoordinate.lon, leftBoundary.lon) <
                min(lowerRightTileCoordinate.lon, rightBoundary.lon)
    }

    millis += 400 // поход в базу с фильтром

//    if (filteredPolygons.isEmpty()) {
//        return
//    }

    val xFrom = tile.x / 256 * 256
    val yFrom = tile.y / 256 * 256

    val image = BufferedImage(256, 256, TYPE_INT_ARGB)
    var dirty = false

//    for (x in 0..255) {
//        points@ for (y in 0..255) {
    for (polygon in filteredPolygons) {
        val mapped = polygon.points.map { toGlobalPixels(it, tile.zoom) }
        val lats = mapped.stream().mapToInt { (it.x - tile.x * 256).toInt() }.toArray()
        val lons = mapped.stream().mapToInt { (it.y - tile.y * 256).toInt() }.toArray()
        val graphics = image.graphics
        graphics.color = when (polygon.type) {
            "COMB" -> Color(255, 200, 0, 150)
            else -> Color(102, 190, 255, 200)
        }
        graphics.fillPolygon(lats, lons, lats.size)
//                val (lat, lon) = fromGlobalPixels(xFrom + x, yFrom + y, tile.zoom)
//                if (polygon.contains(lat, lon)) {
//                    dirty = true
//
//                    if (polygon.type == "COMB") {
//                        image.setRGB(x, y, 123)
//                        println("COMB")
//                    } else if (polygon.type == "PUDDLE") {
//                        image.setRGB(x, y, 5678)
//                        println("PUDDLE")
//                        continue@points
//                    }
//                }
    }
//        }
//    }

    outer@ for (x in 0..255) {
        for (y in 0..255) {
            if (image.getRGB(x, y) != 0) {
                dirty = true
                break
            }
        }
    }

//    val (a, r, g, b) = image.raster.getPixel(0, 0, null as IntArray?)
//    if (a + r + g + b != 0) {
//        System.err.println(image)
//    }

    if (dirty) {
        val file = File("tiles/${tile.zoom}/${tile.x}/$tile.png")
        file.mkdirs()
        ImageIO.write(image, "png", file)
        millis += 400 // сохранение PNG в S3
        saves++
    } else {
        println("empty tile")
    }
//    val seconds = (System.currentTimeMillis() - time) / 1000
//    println("saved a tile, took $seconds")
}
