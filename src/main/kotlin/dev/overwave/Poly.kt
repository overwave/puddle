package dev.overwave

data class Poly(
    val type: String,
    val id: String,
    val center: WorldCoordinate,
    val points: List<WorldCoordinate>,
) {
    fun contains(lat: Double, lon: Double): Boolean {
        if (lon > 30 && lon < 60)
            println("")
        var result = false
        var i = 0
        var j = points.size - 1
        while (i < points.size) {
            if (points[i].lon > lon != points[j].lon > lon && lat < (points[j].lat - points[i].lat) * (lon - points[i].lon) / (points[j].lon - points[i].lon) + points[i].lat) {
                result = !result
            }
            j = i++
        }
        return result
    }
}