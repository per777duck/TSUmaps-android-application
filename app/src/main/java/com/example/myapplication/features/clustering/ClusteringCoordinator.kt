package com.example.myapplication.features.clustering

import com.example.myapplication.algorithms.KmeansAlgorithm
import com.example.myapplication.algorithms.models.AStarMetric
import com.example.myapplication.algorithms.models.EuclideanMetric
import com.example.myapplication.algorithms.models.Point
import com.example.myapplication.data.map.MapData
import com.example.myapplication.data.venues.MetricType
import com.example.myapplication.data.venues.Venue
import com.example.myapplication.data.venues.VenueType

object ClusteringCoordinator {
    suspend fun computeClusters(
        venues: List<Venue>,
        selectedType: VenueType?,
        selectedMetric: MetricType?,
        isComparisonMode: Boolean,
        mapData: MapData
    ): Pair<List<Pair<Venue, Int>>, List<Pair<Venue, Int>>> {
        val filteredVenues = if (selectedType == null) {
            venues
        } else {
            venues.filter { it.type == selectedType }
        }

        if (filteredVenues.isEmpty()) {
            return emptyList<Pair<Venue, Int>>() to emptyList()
        }

        val points = filteredVenues.map {
            Point(
                id = it.id,
                x = it.position.x.toDouble(),
                y = it.position.y.toDouble()
            )
        }

        val metric1 = when (selectedMetric) {
            MetricType.EUCLIDEAN -> EuclideanMetric()
            MetricType.ASTAR -> AStarMetric(mapData)
            else -> EuclideanMetric()
        }

        val kMeans = KmeansAlgorithm(2)
        val primaryResult = kMeans.run(points, metric1)
        val primaryClusters = filteredVenues.map { venue ->
            venue to (primaryResult.find { cluster ->
                cluster.points.any { point -> point.id == venue.id }
            }?.id ?: -1)
        }

        if (!isComparisonMode) {
            return primaryClusters to emptyList()
        }

        val metric2 = if (selectedMetric == MetricType.EUCLIDEAN) {
            EuclideanMetric()
        } else {
            AStarMetric(mapData)
        }

        val secondaryResult = kMeans.run(points, metric2)
        val secondaryClusters = filteredVenues.map { venue ->
            venue to (secondaryResult.find { cluster ->
                cluster.points.any { point -> point.id == venue.id }
            }?.id ?: -1)
        }
        return primaryClusters to secondaryClusters
    }
}
