package com.example.myapplication.data.venues

import androidx.compose.ui.geometry.Offset
import kotlin.math.hypot

enum class FoodItemCategory {
    BREAKFAST,
    LUNCH,
    DINNER,
    DRINK,
    SERVICE,
    SNACK
}

enum class FoodItem(
    val title: String,
    val category: FoodItemCategory
) {
    PANCAKES("Блины", FoodItemCategory.BREAKFAST),
    OMELETTE("Омлет", FoodItemCategory.BREAKFAST),
    PORRIDGE("Каша", FoodItemCategory.BREAKFAST),
    CHEESECAKES("Сырники", FoodItemCategory.BREAKFAST),
    SANDWICH("Сэндвич", FoodItemCategory.BREAKFAST),
    CAPPUCCINO("Капучино", FoodItemCategory.DRINK),
    AMERICANO("Американо", FoodItemCategory.DRINK),
    BLACK_TEA("Черный чай", FoodItemCategory.DRINK),
    GREEN_TEA("Зеленый чай", FoodItemCategory.DRINK),
    SOUP("Суп", FoodItemCategory.LUNCH),
    BORSCH("Борщ", FoodItemCategory.LUNCH),
    PUREE_WITH_CUTLET("Пюре с котлетой", FoodItemCategory.LUNCH),
    BUCKWHEAT_WITH_CHICKEN("Гречка с курицей", FoodItemCategory.LUNCH),
    PILAF("Плов", FoodItemCategory.LUNCH),
    PASTA("Паста", FoodItemCategory.LUNCH),
    SALAD_CAESAR("Салат Цезарь", FoodItemCategory.LUNCH),
    DESSERT("Десерт", FoodItemCategory.DINNER),
    BOTTLED_WATER("Вода", FoodItemCategory.SERVICE),
    NAPKINS("Салфетки", FoodItemCategory.SERVICE),
    DISPOSABLE_CUTLERY("Одноразовая посуда", FoodItemCategory.SERVICE),
    PACKET("Пакет", FoodItemCategory.SERVICE),
    APPLE("Яблоко", FoodItemCategory.SNACK),
    BANANA("Банан", FoodItemCategory.SNACK)
}

data class GeoPoint(
    val xKm: Double,
    val yKm: Double
) {
    fun distanceTo(other: GeoPoint): Double = hypot(xKm - other.xKm, yKm - other.yKm)
}

data class FoodVenue(
    val id: Int,
    val name: String,
    val point: GeoPoint,
    val mapPosition: Offset,
    val menu: Set<FoodItem>,
    val openFromMinutes: Int,
    val closeAtMinutes: Int,
    var userRating: Int? = null
)

val breakfastPreset: Set<FoodItem> = setOf(
    FoodItem.OMELETTE,
    FoodItem.AMERICANO,
    FoodItem.SANDWICH,
    FoodItem.BOTTLED_WATER
)

val lunchPreset: Set<FoodItem> = setOf(
    FoodItem.BORSCH,
    FoodItem.PUREE_WITH_CUTLET,
    FoodItem.SALAD_CAESAR,
    FoodItem.BOTTLED_WATER,
    FoodItem.DISPOSABLE_CUTLERY
)

val dinnerPreset: Set<FoodItem> = setOf(
    FoodItem.PASTA,
    FoodItem.BLACK_TEA,
    FoodItem.DESSERT,
    FoodItem.BOTTLED_WATER,
    FoodItem.NAPKINS
)

const val CAMPUS_MAP_WIDTH_PX = 784f
const val CAMPUS_MAP_HEIGHT_PX = 757f
const val CAMPUS_WIDTH_KM = 2.4
const val CAMPUS_HEIGHT_KM = 2.2

private fun mapToGeo(position: Offset): GeoPoint {
    val xKm = (position.x / CAMPUS_MAP_WIDTH_PX) * CAMPUS_WIDTH_KM
    val yKm = (position.y / CAMPUS_MAP_HEIGHT_PX) * CAMPUS_HEIGHT_KM
    return GeoPoint(xKm = xKm, yKm = yKm)
}

val userStartMapPoint = Offset(300f, 410f)
val userStartPoint = mapToGeo(userStartMapPoint)

val foodVenues: List<FoodVenue> = listOf(
    FoodVenue(
        id = 1,
        name = "Сибирские блины (корпус ТГУ)",
        mapPosition = Offset(430f, 300f),
        point = mapToGeo(Offset(430f, 300f)),
        menu = setOf(
            FoodItem.PANCAKES, FoodItem.CHEESECAKES, FoodItem.BLACK_TEA,
            FoodItem.GREEN_TEA, FoodItem.BOTTLED_WATER
        ),
        openFromMinutes = 8 * 60,
        closeAtMinutes = 20 * 60
    ),
    FoodVenue(
        id = 2,
        name = "Столовая ТГУ №1",
        mapPosition = Offset(720f, 745f),
        point = mapToGeo(Offset(320f, 245f)),
        menu = setOf(
            FoodItem.SOUP, FoodItem.BORSCH, FoodItem.PUREE_WITH_CUTLET,
            FoodItem.BUCKWHEAT_WITH_CHICKEN, FoodItem.SALAD_CAESAR, FoodItem.BOTTLED_WATER,
            FoodItem.NAPKINS
        ),
        openFromMinutes = 9 * 60,
        closeAtMinutes = 18 * 60
    ),
    FoodVenue(
        id = 3,
        name = "Буфет главного корпуса",
        mapPosition = Offset(350f, 280f),
        point = mapToGeo(Offset(350f, 280f)),
        menu = setOf(
            FoodItem.SANDWICH, FoodItem.DESSERT, FoodItem.CAPPUCCINO,
            FoodItem.AMERICANO, FoodItem.BOTTLED_WATER, FoodItem.BANANA
        ),
        openFromMinutes = 8 * 60,
        closeAtMinutes = 18 * 60 + 30
    ),
    FoodVenue(
        id = 4,
        name = "Минутка",
        mapPosition = Offset(340f, 500f),
        point = mapToGeo(Offset(340f, 500f)),
        menu = setOf(
            FoodItem.SANDWICH, FoodItem.PANCAKES, FoodItem.CAPPUCCINO,
            FoodItem.AMERICANO, FoodItem.BOTTLED_WATER, FoodItem.PACKET
        ),
        openFromMinutes = 8 * 60,
        closeAtMinutes = 21 * 60
    ),
    FoodVenue(
        id = 5,
        name = "Утренний буфет",
        mapPosition = Offset(210f, 325f),
        point = mapToGeo(Offset(210f, 325f)),
        menu = setOf(
            FoodItem.OMELETTE, FoodItem.PORRIDGE, FoodItem.AMERICANO,
            FoodItem.GREEN_TEA, FoodItem.BOTTLED_WATER
        ),
        openFromMinutes = 7 * 60,
        closeAtMinutes = 14 * 60
    ),
    FoodVenue(
        id = 6,
        name = "Coffee Point (ТГУ)",
        mapPosition = Offset(470f, 250f),
        point = mapToGeo(Offset(470f, 250f)),
        menu = setOf(
            FoodItem.CAPPUCCINO, FoodItem.AMERICANO, FoodItem.BLACK_TEA,
            FoodItem.GREEN_TEA, FoodItem.DESSERT, FoodItem.BOTTLED_WATER
        ),
        openFromMinutes = 8 * 60,
        closeAtMinutes = 18 * 60
    ),
    FoodVenue(
        id = 7,
        name = "Столовая ТГУ №2",
        mapPosition = Offset(375f, 360f),
        point = mapToGeo(Offset(375f, 360f)),
        menu = setOf(
            FoodItem.SOUP, FoodItem.PILAF, FoodItem.PASTA,
            FoodItem.BUCKWHEAT_WITH_CHICKEN, FoodItem.BOTTLED_WATER,
            FoodItem.NAPKINS, FoodItem.DISPOSABLE_CUTLERY
        ),
        openFromMinutes = 8 * 60 + 30,
        closeAtMinutes = 19 * 60
    ),
    FoodVenue(
        id = 8,
        name = "Кафе Полдень (ТГУ)",
        mapPosition = Offset(415f, 285f),
        point = mapToGeo(Offset(415f, 285f)),
        menu = setOf(
            FoodItem.SOUP, FoodItem.SALAD_CAESAR, FoodItem.PASTA,
            FoodItem.BLACK_TEA, FoodItem.DESSERT, FoodItem.APPLE
        ),
        openFromMinutes = 10 * 60,
        closeAtMinutes = 20 * 60
    )
)