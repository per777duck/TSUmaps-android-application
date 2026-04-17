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
    val capacity: Int? = null,
)

val listOfVenues: List<Venue> = listOf(
    Venue(0, "Коворкинг в ГК", 148, 89, VenueType.COWORKING, MetricType.EUCLIDEAN, capacity = 8),
    Venue(1, "Коворкинг в ЦК", 123, 99, VenueType.COWORKING, MetricType.EUCLIDEAN, capacity = 10),
    Venue(2, "Коворкинг Научной библиотеки", 179, 151, VenueType.COWORKING, MetricType.EUCLIDEAN, capacity = 60),
    Venue(3, "Коворкинг VK", 96, 112, VenueType.COWORKING, MetricType.EUCLIDEAN, capacity = 25),
    Venue(4, "Профессорам В.М. Флоринскому и Д.И. Менделееву", 157, 100, VenueType.SIGHTSEEING, MetricType.EUCLIDEAN),
    Venue(5, "Камень, символизирующий геофизический центр Евразии", 184, 93, VenueType.SIGHTSEEING, MetricType.EUCLIDEAN),
    Venue(6, "Памятник павшим за Родину сотрудникам и студентам ТГУ", 169, 118, VenueType.SIGHTSEEING, MetricType.EUCLIDEAN),
    Venue(7, "Памятник Г.Н. Потанину", 184, 111, VenueType.SIGHTSEEING, MetricType.EUCLIDEAN),
    Venue(8, "Скульптура профессор Белкин", 158, 75, VenueType.SIGHTSEEING, MetricType.EUCLIDEAN),
    Venue(9, "Граффити", 60, 162, VenueType.SIGHTSEEING, MetricType.EUCLIDEAN),
    Venue(10, "Декоративный объект Домик", 129, 234, VenueType.SIGHTSEEING, MetricType.EUCLIDEAN),
    Venue(11, "Памятник профессору П.Н.Крылову и профессору Л.П.Сергиевской", 129, 192, VenueType.SIGHTSEEING, MetricType.EUCLIDEAN),
    Venue(12, "Памятная доска С.П.Карпову", 198, 177, VenueType.SIGHTSEEING, MetricType.EUCLIDEAN),
    Venue(13, "Коворкинг HITS", 96, 115, VenueType.COWORKING, MetricType.EUCLIDEAN, capacity = 12),
    Venue(14, "Коворкинг IDO", 96, 116, VenueType.COWORKING, MetricType.EUCLIDEAN, capacity = 20),
    Venue(15, "Аудитория №220", 96, 117, VenueType.COWORKING, MetricType.EUCLIDEAN, capacity = 30),
    Venue(16, "Аудитория №302", 96, 116, VenueType.COWORKING, MetricType.EUCLIDEAN, capacity = 120),










    )

