package com.example.myapplication.data.venues

enum class MetricType { EUCLIDEAN, ASTAR }

enum class VenueType {
    FOOD,
    COWORKING,
    SIGHTSEEING
}

data class Venue(
    val id: Int,
    val name: String,
    val x: Int,
    val y: Int,
    val type: VenueType,
    val metric: MetricType,
    var userRating: Int? = null
)

val listOfVenues: List<Venue> = listOf(
    Venue(0, "Главный корпус", 100, 120, VenueType.COWORKING, MetricType.EUCLIDEAN),
    Venue(1, "Второй корпус", 87, 126, VenueType.COWORKING, MetricType.EUCLIDEAN),
    Venue(2, "Библиотека", 200, 53, VenueType.FOOD, MetricType.EUCLIDEAN),
    Venue(3, "Старая Библиотека", 210, 53, VenueType.FOOD, MetricType.EUCLIDEAN),
    Venue(4, "Профессорам В.М. Флоринскому и Д.И. Менделееву", 157, 100, VenueType.SIGHTSEEING, MetricType.EUCLIDEAN),
    Venue(5, "Камень, символизирующий геофизический центр Евразии", 184, 93, VenueType.SIGHTSEEING, MetricType.EUCLIDEAN),
    Venue(6, "Памятник павшим за Родину сотрудникам и студентам ТГУ", 169, 118, VenueType.SIGHTSEEING, MetricType.EUCLIDEAN),
    Venue(7, "Памятник Г.Н. Потанину", 184, 111, VenueType.SIGHTSEEING, MetricType.EUCLIDEAN),
    Venue(8, "Скульптура профессор Белкин", 158, 75, VenueType.SIGHTSEEING, MetricType.EUCLIDEAN),
    Venue(9, "Граффити", 60, 162, VenueType.SIGHTSEEING, MetricType.EUCLIDEAN),
    Venue(10, "Декоративный объект Домик", 129, 234, VenueType.SIGHTSEEING, MetricType.EUCLIDEAN),
    Venue(11, "Памятник профессору П.Н.Крылову и профессору Л.П.Сергиевской", 129, 192, VenueType.SIGHTSEEING, MetricType.EUCLIDEAN),
    Venue(12, "Памятная доска С.П.Карпову", 198, 177, VenueType.SIGHTSEEING, MetricType.EUCLIDEAN),






    )

