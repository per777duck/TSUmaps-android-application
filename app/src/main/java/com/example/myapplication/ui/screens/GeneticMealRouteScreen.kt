package com.example.myapplication.ui.screens

import android.graphics.Paint
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.R
import com.example.myapplication.algorithms.routes.AStarAlgorithm
import com.example.myapplication.algorithms.routes.GeneticIterationUpdate
import com.example.myapplication.algorithms.routes.GeneticMealRouteResult
import com.example.myapplication.algorithms.routes.MealRouteGeneticAlgorithm
import com.example.myapplication.algorithms.routes.RouteStop
import com.example.myapplication.data.map.MapData
import com.example.myapplication.data.map.MapRendering
import com.example.myapplication.data.venues.FoodItem
import com.example.myapplication.data.venues.FoodItemCategory
import com.example.myapplication.data.venues.breakfastPreset
import com.example.myapplication.data.venues.dinnerPreset
import com.example.myapplication.data.venues.foodVenues
import com.example.myapplication.data.venues.lunchPreset
import com.example.myapplication.data.venues.userStartMapPoint
import com.example.myapplication.features.path.CampusPathPlanner
import com.example.myapplication.features.path.CampusRoutingContext
import com.example.myapplication.ui.components.TGU_Blue
import com.example.myapplication.ui.components.TGU_Gold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneticMealRouteScreen(mapData: MapData) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()
    val initialStatus = stringResource(R.string.genetic_status_open_filter)
    val statusPreparingGraph = stringResource(R.string.genetic_status_prepare_graph)
    val legendText = stringResource(R.string.genetic_legend)
    val statusGraphNotReady = stringResource(R.string.genetic_status_graph_not_ready)
    val statusSelectOneItem = stringResource(R.string.genetic_status_select_one_item)
    val statusSearching = stringResource(R.string.genetic_status_searching)
    val statusCleared = stringResource(R.string.genetic_status_cleared)

    val astar = remember(mapData) { AStarAlgorithm(mapData) }
    var routing by remember(mapData) { mutableStateOf<CampusRoutingContext?>(null) }
    var isRoutingLoading by remember(mapData) { mutableStateOf(true) }

    var selectedItems by remember { mutableStateOf<Set<FoodItem>>(breakfastPreset) }
    var isRunning by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var progressUpdate by remember { mutableStateOf<GeneticIterationUpdate?>(null) }
    var finalResult by remember { mutableStateOf<GeneticMealRouteResult?>(null) }
    var statusText by remember(initialStatus) { mutableStateOf(initialStatus) }
    var runningJob by remember { mutableStateOf<Job?>(null) }
    var pathSegments by remember { mutableStateOf<List<List<Offset>>>(emptyList()) }

    LaunchedEffect(mapData) {
        isRoutingLoading = true
        statusText = statusPreparingGraph
        routing = withContext(Dispatchers.Default) {
            CampusPathPlanner.buildContext(
                mapData = mapData,
                algorithm = astar,
                rawStartOffset = userStartMapPoint,
                venues = foodVenues
            )
        }
        isRoutingLoading = false
        if (!isRunning) {
            statusText = initialStatus
        }
    }

    val displayedRoute = finalResult?.route ?: progressUpdate?.route.orEmpty()
    val displayedMissing = finalResult?.missingItems ?: progressUpdate?.missingItems.orEmpty()
    val displayedCollected = finalResult?.collectedItems ?: progressUpdate?.collectedItems.orEmpty()
    val generation = progressUpdate?.generation ?: 0
    val totalGenerations = progressUpdate?.totalGenerations ?: 0
    val progress = if (totalGenerations > 0) generation / totalGenerations.toFloat() else 0f

    LaunchedEffect(displayedRoute, routing, mapData) {
        val routingContext = routing
        if (routingContext == null || displayedRoute.isEmpty()) {
            pathSegments = emptyList()
            return@LaunchedEffect
        }
        val segments = withContext(Dispatchers.Default) {
            val built = mutableListOf<List<Offset>>()
            var current = routingContext.startNode
            displayedRoute.forEach { stop ->
                val target = routingContext.venueNodes[stop.venue.id] ?: return@forEach
                val segment = astar.findPathSync(current, target)
                val points = if (segment != null && segment.isNotEmpty()) {
                    segment.map { node -> CampusPathPlanner.nodeToMapOffset(mapData, node) }
                } else {
                    listOf(
                        routingContext.venueDisplayOffsets[stop.venue.id] ?: stop.venue.mapPosition
                    )
                }
                if (points.size >= 2) {
                    built += points
                }
                current = target
            }
            built
        }
        pathSegments = segments
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
                text = stringResource(R.string.genetic_title),
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
                    mapData = mapData,
                    route = displayedRoute,
                    pathSegments = pathSegments,
                    routing = routing
                )
            }
            Text(
                legendText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(statusText, color = TGU_Blue)

            if (totalGenerations > 0) {
                LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth())
                Text(stringResource(R.string.genetic_best_generation, generation, totalGenerations))
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val minutes =
                        finalResult?.bestTravelMinutes ?: progressUpdate?.bestTravelMinutes ?: 0
                    Text(stringResource(R.string.genetic_best_route, minutes, displayedRoute.size))
                    Text(
                        stringResource(
                            R.string.genetic_collected,
                            displayedCollected.joinToString { it.title })
                    )
                    if (displayedMissing.isNotEmpty()) {
                        Text(
                            text = stringResource(
                                R.string.genetic_missing,
                                displayedMissing.joinToString { it.title }),
                            color = Color(0xFFB00020)
                        )
                    }
                }
            }

            Text(stringResource(R.string.genetic_schedule_title), fontWeight = FontWeight.SemiBold)
            if (displayedRoute.isEmpty()) {
                Text(stringResource(R.string.genetic_route_not_built))
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
            Icon(
                Icons.Default.FilterList,
                contentDescription = stringResource(R.string.content_desc_genetic_filters)
            )
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
                    val routingContext = routing
                    if (isRoutingLoading || routingContext == null) {
                        statusText = statusGraphNotReady
                        return@FilterPanelContent
                    }
                    if (selectedItems.isEmpty()) {
                        statusText = statusSelectOneItem
                        return@FilterPanelContent
                    }

                    runningJob?.cancel()
                    isRunning = true
                    statusText = statusSearching
                    progressUpdate = null
                    finalResult = null
                    pathSegments = emptyList()
                    showFilterSheet = false

                    val calendar = Calendar.getInstance()
                    val minuteOfDay =
                        calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

                    runningJob = scope.launch {
                        val result = withContext(Dispatchers.Default) {
                            MealRouteGeneticAlgorithm.optimize(
                                venues = foodVenues,
                                requiredItems = selectedItems,
                                routing = routingContext,
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
                            context.getString(
                                R.string.genetic_status_built,
                                result.route.size,
                                result.bestTravelMinutes
                            )
                        } else {
                            context.getString(
                                R.string.genetic_status_not_all_available,
                                result.missingItems.size
                            )
                        }
                    }
                },
                onResetClick = {
                    runningJob?.cancel()
                    isRunning = false
                    progressUpdate = null
                    finalResult = null
                    pathSegments = emptyList()
                    statusText = statusCleared
                    showFilterSheet = false
                }
            )
        }
    }
}

@Composable
private fun GeneticRouteMap(
    mapData: MapData,
    route: List<RouteStop>,
    pathSegments: List<List<Offset>>,
    routing: CampusRoutingContext?
) {
    val legColors = listOf(
        Color(0xFFE53935),
        Color(0xFF1E88E5),
        Color(0xFF43A047),
        Color(0xFFF4511E),
        Color(0xFF8E24AA),
        Color(0xFF00897B)
    )

    MapRendering.TguMapWrapper(
        mapData = mapData,
        modifier = Modifier.fillMaxSize(),
    ) { currentScale, _, _ ->
        Canvas(modifier = Modifier.fillMaxSize()) {
            pathSegments.forEachIndexed { index, segment ->
                if (segment.size < 2) return@forEachIndexed
                val path = Path().apply {
                    moveTo(segment.first().x, segment.first().y)
                    segment.drop(1).forEach { point -> lineTo(point.x, point.y) }
                }
                drawPath(
                    path = path,
                    color = legColors[index % legColors.size],
                    style = Stroke(width = (6f / currentScale).coerceAtLeast(1.6f))
                )
            }

            routing?.let { ctx ->
                drawCircle(
                    color = TGU_Gold,
                    radius = (11f / currentScale).coerceAtLeast(3f),
                    center = ctx.startDisplayOffset
                )
            }

            val labelPaint = Paint().apply {
                color = android.graphics.Color.WHITE
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
                textSize = (11f / currentScale).coerceAtLeast(7f)
            }

            route.forEachIndexed { idx, stop ->
                val center =
                    routing?.venueDisplayOffsets?.get(stop.venue.id) ?: stop.venue.mapPosition
                drawCircle(
                    color = Color.White,
                    radius = (10f / currentScale).coerceAtLeast(3f),
                    center = center
                )
                drawCircle(
                    color = TGU_Blue,
                    radius = (8.5f / currentScale).coerceAtLeast(2.5f),
                    center = center
                )
                drawContext.canvas.nativeCanvas.drawText(
                    (idx + 1).toString(),
                    center.x,
                    center.y + (3f / currentScale).coerceAtLeast(1.2f),
                    labelPaint
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
        Text(
            stringResource(R.string.genetic_filter_title),
            fontWeight = FontWeight.SemiBold,
            color = TGU_Blue
        )
        Text(
            stringResource(R.string.genetic_user_speed),
            style = MaterialTheme.typography.bodyMedium
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onBreakfast,
                enabled = !isRunning
            ) { Text(stringResource(R.string.food_category_breakfast)) }
            OutlinedButton(
                onClick = onLunch,
                enabled = !isRunning
            ) { Text(stringResource(R.string.food_category_lunch)) }
            OutlinedButton(
                onClick = onDinner,
                enabled = !isRunning
            ) { Text(stringResource(R.string.food_category_dinner)) }
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
                    text = stringResource(categoryLabelRes(category)),
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

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onBuildClick,
                enabled = !isRunning,
                colors = ButtonDefaults.buttonColors(containerColor = TGU_Blue),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    if (isRunning) stringResource(R.string.common_searching_upper) else stringResource(
                        R.string.common_build_route_upper
                    ),
                    color = Color.White
                )
            }
            OutlinedButton(
                onClick = onResetClick,
                enabled = !isRunning,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.common_reset))
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
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("$index. ${stop.venue.name}", fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(
                    R.string.genetic_purchase,
                    stop.purchasedItems.joinToString { it.title })
            )
            Text(
                stringResource(
                    R.string.genetic_arrival_departure,
                    formatMinuteOfDay(stop.arrivalMinuteOfDay),
                    formatMinuteOfDay(stop.departureMinuteOfDay),
                    stop.minutesFromStart
                )
            )
            Text(
                stringResource(
                    R.string.genetic_working_hours,
                    formatMinuteOfDay(stop.venue.openFromMinutes),
                    formatMinuteOfDay(stop.venue.closeAtMinutes)
                )
            )
        }
    }
}

private fun categoryLabelRes(category: FoodItemCategory): Int {
    return when (category) {
        FoodItemCategory.BREAKFAST -> R.string.food_category_breakfast
        FoodItemCategory.LUNCH -> R.string.food_category_lunch
        FoodItemCategory.DINNER -> R.string.food_category_dinner
        FoodItemCategory.DRINK -> R.string.food_category_drink
        FoodItemCategory.SERVICE -> R.string.food_category_service
        FoodItemCategory.SNACK -> R.string.food_category_snack
    }
}

private fun formatMinuteOfDay(minuteOfDay: Int): String {
    val normalized = ((minuteOfDay % (24 * 60)) + (24 * 60)) % (24 * 60)
    val h = normalized / 60
    val m = normalized % 60
    return "%02d:%02d".format(h, m)
}
