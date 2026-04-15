package com.example.myapplication.data.venues

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
    var userRating: Int? = null
)

val listOfVenues: List<Venue> = listOf(
    Venue(0, "Главный корпус", Offset(100f, 120f), VenueType.COWORKING, MetricType.EUCLIDEAN),
    Venue(1, "Второй корпус", Offset(87f, 126f), VenueType.COWORKING, MetricType.EUCLIDEAN),
    Venue(2, "Библиотека", Offset(200f, 53f), VenueType.FOOD, MetricType.EUCLIDEAN),
    Venue(3, "Старая Библиотека", Offset(210f, 53f), VenueType.FOOD, MetricType.EUCLIDEAN),
    Venue(4, "Профессорам В.М. Флоринскому и Д.И. Менделееву", Offset(157f,100f), VenueType.SIGHTSEEING, MetricType.EUCLIDEAN ),
    Venue(5, "Камень, символизирующий геофизический центр Евразии", Offset(184f,93f), VenueType.SIGHTSEEING, MetricType.EUCLIDEAN ),
    Venue(6, "Памятник павшим за Родину сотрудникам и студентам ТГУ", Offset(169f,118f), VenueType.SIGHTSEEING, MetricType.EUCLIDEAN ),
    Venue(7, "Памятник Г.Н. Потанину", Offset(184f,111f), VenueType.SIGHTSEEING, MetricType.EUCLIDEAN ),
    Venue(8, "Скульптура профессор Белкин", Offset(158f,75f), VenueType.SIGHTSEEING, MetricType.EUCLIDEAN ),
    Venue(9, "Граффити", Offset(60f,162f), VenueType.SIGHTSEEING, MetricType.EUCLIDEAN ),
    Venue(10, "Декоративный объект Домик", Offset(129f,234f), VenueType.SIGHTSEEING, MetricType.EUCLIDEAN ),
    Venue(11, "Памятник профессору П.Н.Крылову и профессору Л.П.Сергиевской", Offset(129f,192f), VenueType.SIGHTSEEING, MetricType.EUCLIDEAN ),
    Venue(12, "Памятная доска С.П.Карпову", Offset(198f,177f), VenueType.SIGHTSEEING, MetricType.EUCLIDEAN ),






    )

