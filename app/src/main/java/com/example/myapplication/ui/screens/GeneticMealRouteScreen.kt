package com.example.myapplication.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.AStarAlgorithm
import com.example.myapplication.data.map.MapData
import com.example.myapplication.data.map.MapRendering
import com.example.myapplication.data.venues.FoodItem
import com.example.myapplication.data.venues.FoodItemCategory
import com.example.myapplication.data.venues.breakfastPreset
import com.example.myapplication.data.venues.dinnerPreset
import com.example.myapplication.data.venues.foodVenues
import com.example.myapplication.data.venues.lunchPreset
import com.example.myapplication.data.venues.userStartMapPoint
import com.example.myapplication.features.genetic.GeneticIterationUpdate
import com.example.myapplication.features.genetic.GeneticMealRouteResult
import com.example.myapplication.features.genetic.MealRouteGeneticAlgorithm
import com.example.myapplication.features.genetic.RouteStop
import com.example.myapplication.features.path.CampusPathPlanner
import com.example.myapplication.features.path.CampusRoutingContext
import com.example.myapplication.ui.TGU_Blue
import com.example.myapplication.ui.TGU_Gold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneticMealRouteScreen(mapData: MapData) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()

    val astar = remember(mapData) { AStarAlgorithm(mapData) }
    val routing = remember(mapData) {
        CampusPathPlanner.buildContext(
            mapData = mapData,
            algorithm = astar,
            rawStartOffset = userStartMapPoint,
            venues = foodVenues
        )
    }

    var selectedItems by remember { mutableStateOf<Set<FoodItem>>(breakfastPreset) }
    var isRunning by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var progressUpdate by remember { mutableStateOf<GeneticIterationUpdate?>(null) }
    var finalResult by remember { mutableStateOf<GeneticMealRouteResult?>(null) }
    var statusText by remember { mutableStateOf("Откройте фильтр, выберите блюда и постройте путь") }
    var runningJob by remember { mutableStateOf<Job?>(null) }
    var pathPolyline by remember { mutableStateOf<List<Offset>>(emptyList()) }

    val displayedRoute = finalResult?.route ?: progressUpdate?.route.orEmpty()
    val displayedMissing = finalResult?.missingItems ?: progressUpdate?.missingItems.orEmpty()
    val displayedCollected = finalResult?.collectedItems ?: progressUpdate?.collectedItems.orEmpty()
    val generation = progressUpdate?.generation ?: 0
    val totalGenerations = progressUpdate?.totalGenerations ?: 0
    val progress = if (totalGenerations > 0) generation / totalGenerations.toFloat() else 0f

    LaunchedEffect(displayedRoute, routing, mapData) {
        if (displayedRoute.isEmpty()) {
            pathPolyline = emptyList()
            return@LaunchedEffect
        }
        val ids = displayedRoute.map { it.venue.id }
        val poly = withContext(Dispatchers.Default) {
            CampusPathPlanner.buildFullPathPolyline(
                mapData = mapData,
                algorithm = astar,
                orderedVenueIds = ids,
                context = routing
            )
        }
        pathPolyline = poly
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Маршрут питания на карте",
                style = MaterialTheme.typography.titleLarge,
                color = TGU_Blue,
                fontWeight = FontWeight.SemiBold
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                GeneticRouteMap(
                    route = displayedRoute,
                    pathPolyline = pathPolyline,
                    routing = routing
                )
            }

            Text(statusText, color = TGU_Blue)

            if (totalGenerations > 0) {
                LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth())
                Text("Лучшая особь на поколении: $generation / $totalGenerations")
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val minutes = finalResult?.bestTravelMinutes ?: progressUpdate?.bestTravelMinutes ?: 0
                    Text("Лучший маршрут: $minutes мин, остановок: ${displayedRoute.size}")
                    Text("Собрано: ${displayedCollected.joinToString { it.title }}")
                    if (displayedMissing.isNotEmpty()) {
                        Text(
                            text = "Не найдено: ${displayedMissing.joinToString { it.title }}",
                            color = Color(0xFFB00020)
                        )
                    }
                }
            }

            Text("Поминутное расписание", fontWeight = FontWeight.SemiBold)
            if (displayedRoute.isEmpty()) {
                Text("Путь пока не построен")
            } else {
                displayedRoute.forEachIndexed { index, stop ->
                    RouteRow(index = index + 1, stop = stop)
                }
            }
            Spacer(modifier = Modifier.height(64.dp))
        }

        FloatingActionButton(
            onClick = { showFilterSheet = true },
            containerColor = TGU_Blue,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, bottom = 20.dp)
        ) {
            Icon(Icons.Default.FilterList, contentDescription = "Фильтры")
        }
    }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            FilterPanelContent(
                selectedItems = selectedItems,
                isRunning = isRunning,
                onSelectItems = { selectedItems = it },
                onBreakfast = { selectedItems = breakfastPreset },
                onLunch = { selectedItems = lunchPreset },
                onDinner = { selectedItems = dinnerPreset },
                onBuildClick = {
                    if (selectedItems.isEmpty()) {
                        statusText = "Нужно выбрать хотя бы одно блюдо"
                        return@FilterPanelContent
                    }

                    runningJob?.cancel()
                    isRunning = true
                    statusText = "Идет поиск маршрута..."
                    progressUpdate = null
                    finalResult = null
                    pathPolyline = emptyList()
                    showFilterSheet = false

                    val calendar = Calendar.getInstance()
                    val minuteOfDay =
                        calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

                    runningJob = scope.launch {
                        val result = withContext(Dispatchers.Default) {
                            MealRouteGeneticAlgorithm.optimize(
                                venues = foodVenues,
                                requiredItems = selectedItems,
                                routing = routing,
                                currentMinuteOfDay = minuteOfDay
                            ) { iteration ->
                                scope.launch {
                                    progressUpdate = iteration
                                }
                            }
                        }

                        finalResult = result
                        isRunning = false
                        statusText = if (result.missingItems.isEmpty()) {
                            "Маршрут построен: ${result.route.size} точек, ${result.bestTravelMinutes} мин."
                        } else {
                            "Не все блюда доступны: не найдено ${result.missingItems.size}"
                        }
                    }
                },
                onResetClick = {
                    runningJob?.cancel()
                    isRunning = false
                    progressUpdate = null
                    finalResult = null
                    pathPolyline = emptyList()
                    statusText = "Состояние очищено"
                    showFilterSheet = false
                }
            )
        }
    }
}

@Composable
private fun GeneticRouteMap(
    route: List<RouteStop>,
    pathPolyline: List<Offset>,
    routing: CampusRoutingContext
) {
    MapRendering.TguMapWrapper(
        modifier = Modifier.fillMaxSize(),
        imageContentScale = ContentScale.FillBounds
    ) { currentScale, _, _ ->
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (pathPolyline.size >= 2) {
                val path = Path().apply {
                    moveTo(pathPolyline.first().x, pathPolyline.first().y)
                    pathPolyline.drop(1).forEach { point -> lineTo(point.x, point.y) }
                }
                drawPath(
                    path = path,
                    color = Color(0xFFEF5350),
                    style = Stroke(width = (7f / currentScale).coerceAtLeast(1.5f))
                )
            }

            drawCircle(
                color = TGU_Gold,
                radius = (11f / currentScale).coerceAtLeast(3f),
                center = routing.startDisplayOffset
            )

            route.forEach { stop ->
                val center = routing.venueDisplayOffsets[stop.venue.id] ?: stop.venue.mapPosition
                drawCircle(
                    color = TGU_Blue,
                    radius = (9f / currentScale).coerceAtLeast(2.5f),
                    center = center
                )
            }
        }
    }
}

@Composable
private fun FilterPanelContent(
    selectedItems: Set<FoodItem>,
    isRunning: Boolean,
    onSelectItems: (Set<FoodItem>) -> Unit,
    onBreakfast: () -> Unit,
    onLunch: () -> Unit,
    onDinner: () -> Unit,
    onBuildClick: () -> Unit,
    onResetClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Фильтры маршрута", fontWeight = FontWeight.SemiBold, color = TGU_Blue)
        Text("Скорость пользователя: 5 км/ч", style = MaterialTheme.typography.bodyMedium)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBreakfast, enabled = !isRunning) { Text("Завтрак") }
            OutlinedButton(onClick = onLunch, enabled = !isRunning) { Text("Обед") }
            OutlinedButton(onClick = onDinner, enabled = !isRunning) { Text("Ужин") }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            FoodItem.entries.groupBy { it.category }.forEach { (category, items) ->
                Text(
                    text = categoryLabel(category),
                    style = MaterialTheme.typography.labelLarge,
                    color = TGU_Blue,
                    modifier = Modifier.padding(top = 6.dp)
                )
                items.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = item in selectedItems,
                            enabled = !isRunning,
                            onCheckedChange = { checked ->
                                onSelectItems(
                                    if (checked) selectedItems + item else selectedItems - item
                                )
                            }
                        )
                        Text(item.title)
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onBuildClick,
                enabled = !isRunning,
                colors = ButtonDefaults.buttonColors(containerColor = TGU_Blue),
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isRunning) "ПОИСК..." else "ПОСТРОИТЬ ПУТЬ", color = Color.White)
            }
            OutlinedButton(
                onClick = onResetClick,
                enabled = !isRunning,
                modifier = Modifier.weight(1f)
            ) {
                Text("Сброс")
            }
        }
    }
}

@Composable
private fun RouteRow(index: Int, stop: RouteStop) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("$index. ${stop.venue.name}", fontWeight = FontWeight.SemiBold)
            Text("Покупка: ${stop.purchasedItems.joinToString { it.title }}")
            Text(
                "Прибытие ${formatMinuteOfDay(stop.arrivalMinuteOfDay)} -> " +
                    "выход ${formatMinuteOfDay(stop.departureMinuteOfDay)} " +
                    "(+${stop.minutesFromStart} мин от старта)"
            )
            Text("Режим работы: ${formatMinuteOfDay(stop.venue.openFromMinutes)} - ${formatMinuteOfDay(stop.venue.closeAtMinutes)}")
        }
    }
}

private fun categoryLabel(category: FoodItemCategory): String {
    return when (category) {
        FoodItemCategory.BREAKFAST -> "Завтрак"
        FoodItemCategory.LUNCH -> "Обед"
        FoodItemCategory.DINNER -> "Ужин"
        FoodItemCategory.DRINK -> "Напитки"
        FoodItemCategory.SERVICE -> "Сервис и упаковка"
        FoodItemCategory.SNACK -> "Перекусы"
    }
}

private fun formatMinuteOfDay(minuteOfDay: Int): String {
    val normalized = ((minuteOfDay % (24 * 60)) + (24 * 60)) % (24 * 60)
    val h = normalized / 60
    val m = normalized % 60
    return "%02d:%02d".format(h, m)
}
