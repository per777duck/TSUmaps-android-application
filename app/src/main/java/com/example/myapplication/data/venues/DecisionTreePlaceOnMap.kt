package com.example.myapplication.data.venues

fun foodVenueForRecommendedPlace(raw: String): FoodVenue? {
    return when (raw.trim()) {
        "Main_Cafeteria" -> foodVenues.find { it.id == 2 }
        "Yarche" -> foodVenues.find { it.id == 4 }
        "Bus_Stop_Coffee" -> foodVenues.find { it.id == 6 }
        "Starbooks" -> foodVenues.find { it.id == 6 }
        "Vending_Machine" -> foodVenues.find { it.id == 3 }
        "Second_Building_Cafe" -> foodVenues.find { it.id == 7 }
        "Siberian_Pancakes" -> foodVenues.find { it.id == 1 }
        "Morning_Buffet" -> foodVenues.find { it.id == 5 }
        "Midday_Cafe" -> foodVenues.find { it.id == 8 }
        else -> null
    }
}