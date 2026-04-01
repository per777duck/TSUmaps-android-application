package com.example.myapplication

data class FoodItem(val name: String)

data class Venue(
    val id: Int,
    val name: String,
    val x: Int,
    val y: Int,
    val menu: Set<FoodItem>,
    val closingHour: Int
)

val tguVenues = listOf(
    Venue(0, "Сибирские блины", 20, 30, setOf(FoodItem("Блины")), 21),
    Venue(1, "Starbucks", 85, 15, setOf(FoodItem("Кофе")), 23),
    Venue(2, "Ярче", 50, 50, setOf(FoodItem("Посуда"), FoodItem("Сэндвич")), 22),
    Venue(3, "Столовая ТГУ", 15, 75, setOf(FoodItem("Суп"), FoodItem("Кофе")), 18),
    Venue(4, "Bellissimo", 90, 80, setOf(FoodItem("Пицца")), 22),
    Venue(5, "Университетская роща", 10, 10, setOf(FoodItem("Приборы")), 20)
)