package com.example.myapplication.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.myapplication.data.map.MapData
import com.example.myapplication.data.map.MapRendering.TguMapWrapper
import com.example.myapplication.data.venues.MetricType
import com.example.myapplication.data.venues.Venue
import com.example.myapplication.data.venues.VenueType
import com.example.myapplication.features.clustering.ClusteringCoordinator
import com.example.myapplication.ui.components.ClusterColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MapClusteringScreen(
    venues: List<Venue>,
    selectedType: VenueType?,
    selectedMetric: MetricType?,
    isComparisonMode: Boolean = false,
    mapData: MapData
) {
    var primaryClusters by remember { mutableStateOf<List<Pair<Venue, Int>>>(emptyList()) }
    var secondaryClusters by remember { mutableStateOf<List<Pair<Venue, Int>>>(emptyList()) }

    LaunchedEffect(venues, selectedMetric, selectedType, isComparisonMode) {
        val (primary, secondary) = ClusteringCoordinator.computeClusters(
            venues = venues,
            selectedType = selectedType,
            selectedMetric = selectedMetric,
            isComparisonMode = isComparisonMode,
            mapData = mapData
        )
        primaryClusters = primary
        secondaryClusters = secondary
    }

    TguMapWrapper(
        mapData = mapData,
        modifier = Modifier.fillMaxSize()
    ) { currentScale, _, _ ->
        Canvas(modifier = Modifier.fillMaxSize()) {
            primaryClusters.forEach { (venue, clusterId) ->
                val color1 = if (clusterId != -1) {
                    ClusterColors[clusterId % ClusterColors.size]
                } else {
                    Color.Gray
                }

                if (isComparisonMode) {
                    val clusterId2 = secondaryClusters.find { it.first.id == venue.id }?.second ?: -1
                    val color2 = if (clusterId2 != -1) {
                        ClusterColors[clusterId2 % ClusterColors.size]
                    } else {
                        Color.Gray
                    }

                    drawCircle(
                        color = color2,
                        radius = 22f / currentScale.coerceAtLeast(1f),
                        center = venue.position,
                        style = Stroke(width = 6f / currentScale.coerceAtLeast(1f))
                    )
                    drawCircle(
                        color = color1,
                        radius = 12f / currentScale.coerceAtLeast(1f),
                        center = venue.position
                    )
                } else {
                    drawCircle(
                        color = color1,
                        radius = 15f / currentScale.coerceAtLeast(1f),
                        center = venue.position
                    )
                }
            }
        }
    }
}
