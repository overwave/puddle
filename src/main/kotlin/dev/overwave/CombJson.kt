package dev.overwave

import com.fasterxml.jackson.annotation.JsonAlias

data class CombJson(
    val city: String,

    val geometry: List<List<List<Double>>>,

    @JsonAlias("hex_index")
    val hexIndex: String,

    @JsonAlias("location_id")
    val locationId: String,

    val region: String
)
