package com.example.myapplication.features.clustering

import com.example.myapplication.algorithms.KmeansAlgorithm
import com.example.myapplication.algorithms.models.Cluster
import com.example.myapplication.algorithms.models.AStarMetric
import com.example.myapplication.algorithms.models.EuclideanMetric
import com.example.myapplication.algorithms.models.Point
import com.example.myapplication.data.map.MapData
import com.example.myapplication.data.venues.MetricType
import com.example.myapplication.data.venues.Venue
import com.example.myapplication.data.venues.VenueType

object ClusteringCoordinator {
    suspend fun findingClusters(
        venues: List<Venue>,
        selectedType: VenueType?,
        selectedMetric: MetricType?,
        clusterCount: Int,
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

        val limitedClusterCount = clusterCount
            .coerceIn(2, 10)
            .coerceAtMost(points.size)
            .coerceAtLeast(1)

        val primaryResult = KmeansAlgorithm(limitedClusterCount).run(points, metric1)
        val primaryClusters = mapVenuesToClusterIds(filteredVenues, primaryResult)

        if (!isComparisonMode) {
            return primaryClusters to emptyList()
        }

        val metric2 = when (selectedMetric) {
            MetricType.EUCLIDEAN -> EuclideanMetric()
            MetricType.ASTAR -> AStarMetric(mapData)
            else -> EuclideanMetric()
        }

        val secondaryResult = KmeansAlgorithm(limitedClusterCount).run(points, metric2)
        val secondaryClusters = mapVenuesToClusterIds(filteredVenues, secondaryResult)
        return primaryClusters to secondaryClusters
    }

    private fun mapVenuesToClusterIds(
        venues: List<Venue>,
        clusters: List<Cluster>
    ): List<Pair<Venue, Int>> {
        return venues.map { venue ->
            venue to (clusters.find { cluster ->
                cluster.points.any { point ->
                    point.id == venue.id } }?.id ?: -1)
        }
    }
}
