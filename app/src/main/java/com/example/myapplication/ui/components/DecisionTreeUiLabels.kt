package com.example.myapplication.ui.components

internal fun uiFeatureTitle(internalName: String): String = when (internalName) {
    "location" -> "Где вы сейчас"
    "budget" -> "Бюджет"
    "time_available" -> "Сколько есть времени"
    "food_type" -> "Что хочется"
    "queue_tolerance" -> "Готовы ждать в очереди"
    "weather" -> "Погода"
    else -> internalName.replace('_', ' ').replaceFirstChar { it.uppercase() }
}

internal fun uiValueLabel(featureKey: String, raw: String): String {
    return when (featureKey) {
        "location" -> when (raw) {
            "main_building" -> "Главный корпус"
            "second_building" -> "Второй корпус"
            "bus_stop" -> "Остановка / транспорт"
            "campus_center" -> "Кампус-центр"
            else -> raw.replace('_', ' ')
        }

        "budget" -> when (raw) {
            "low" -> "Небольшой"
            "medium" -> "Средний"
            "high" -> "Без ограничений"
            else -> raw
        }

        "time_available" -> when (raw) {
            "very_short" -> "Очень мало (пара минут)"
            "short" -> "Немного"
            "medium" -> "Достаточно"
            else -> raw.replace('_', ' ')
        }

        "food_type" -> when (raw) {
            "coffee" -> "Кофе"
            "pancakes" -> "Блины"
            "pasta" -> "Паста"
            "full_meal" -> "Полноценный обед"
            "snack" -> "Перекус"
            else -> raw.replace('_', ' ')
        }

        "queue_tolerance" -> when (raw) {
            "low" -> "Не хочу ждать"
            "medium" -> "Могу немного подождать"
            "high" -> "Готов подождать"
            else -> raw
        }

        "weather" -> when (raw) {
            "good" -> "Хорошая, можно дойти"
            "bad" -> "Плохая, ближе к выходу"
            else -> raw
        }

        else -> raw.replace('_', ' ')
    }
}

internal fun uiPlaceTitle(raw: String): String = when (raw) {
    "Main_Cafeteria" -> "Главная столовая"
    "Yarche" -> "«Ярче»"
    "Bus_Stop_Coffee" -> "Кофе у остановки"
    "Starbooks" -> "Starbooks"
    "Vending_Machine" -> "Торговый автомат"
    "Second_Building_Cafe" -> "Кафе во втором корпусе"
    "Siberian_Pancakes" -> "«Сибирские блины»"
    "Morning_Buffet" -> "Утренний буфет"
    "Midday_Cafe" -> "Кафе «Полдень»"
    else -> raw.replace('_', ' ')
}

internal fun uiHumanizePathStep(step: String): String {
    val idx = step.indexOf('=')
    if (idx <= 0) return step.trim()
    val key = step.take(idx).trim()
    val value = step.drop(idx + 1).trim()
    return "${uiFeatureTitle(key)}: ${uiValueLabel(key, value)}"
}
