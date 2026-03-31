package com.example.myapplication.data_base

import androidx.compose.ui.geometry.Offset

enum class MetricType { EUCLIDEAN, ASTAR }

enum class VenueType {
    FOOD,
    COWORKING,
    SIGHTSEEING
}

data class Venue(
    val id: Int,
    val name: String,
    val position: Offset,
    val type: VenueType,
    val metric: MetricType,
)

data class VenueWithComparison(
    val venue: Venue,
    val clusterEuclidean: Int,
    val clusterAStar: Int
)

val listOfVenues: List<Venue> = listOf(
        Venue(0, "Главный корпус", Offset(100f, 120f), VenueType.COWORKING, MetricType.EUCLIDEAN),
        Venue(1, "Второй корпус", Offset(87f, 126f),VenueType.COWORKING, MetricType.EUCLIDEAN),
        Venue(2, "Библиотека", Offset(200f, 53f), VenueType.FOOD, MetricType.EUCLIDEAN),
        Venue(3, "Старая Библиотека", Offset(210f, 53f), VenueType.FOOD, MetricType.EUCLIDEAN)
    )