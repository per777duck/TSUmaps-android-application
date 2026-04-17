package com.example.myapplication.features.clustering

import com.example.myapplication.algorithms.clusterization.DBScan
import com.example.myapplication.algorithms.clusterization.KmeansAlgorithm
import com.example.myapplication.algorithms.clusterization.Cluster
import com.example.myapplication.algorithms.clusterization.AStarMetric
import com.example.myapplication.algorithms.clusterization.EuclideanMetric
import com.example.myapplication.algorithms.clusterization.Point
import com.example.myapplication.data.map.MapData
import com.example.myapplication.data.venues.MetricType
import com.example.myapplication.data.venues.Venue
import com.example.myapplication.data.venues.VenueType

enum class ClusterAlgorithmType{
    KMEANS,
    DBSCAN
}

object ClusteringCoordinator {
    private const val DEFAULT_EPS = 20.0
    private const val DEFAULT_MIN_POINTS = 4

    suspend fun findingClusters(
        venues: List<Venue>,
        selectedType: VenueType?,
        selectedMetric: MetricType?,
        selectedAlgorithm: ClusterAlgorithmType,
        clusterCount: Int,
        isComparisonMode: Boolean,
        mapData: MapData
    ): Pair<List<Pair<Venue, Int>>, List<Pair<Venue, Int>>> {
        val filteredVenues = if (selectedType == null) {
            venues
        }
        else {
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

        val primaryMetricType = selectedMetric ?: MetricType.EUCLIDEAN
        val metric1 =  when (primaryMetricType) {
            MetricType.EUCLIDEAN -> EuclideanMetric()
            MetricType.ASTAR -> AStarMetric(mapData)
        }

        val limitedClusterCount = clusterCount
            .coerceIn(2, 10)
            .coerceAtMost(points.size)
            .coerceAtLeast(1)

        val primaryResult = when (selectedAlgorithm) {
            ClusterAlgorithmType.KMEANS -> {
                KmeansAlgorithm(limitedClusterCount).run(points, metric1)
            }
            ClusterAlgorithmType.DBSCAN -> {
                DBScan(DEFAULT_EPS, DEFAULT_MIN_POINTS).run(points, metric1)
            }
        }
        val primaryClusters = mapVenuesToClusterIds(filteredVenues, primaryResult)

        if (!isComparisonMode) {
            return primaryClusters to emptyList()
        }

        val secondaryMetricType = when (primaryMetricType) {
            MetricType.EUCLIDEAN -> MetricType.ASTAR
            MetricType.ASTAR -> MetricType.EUCLIDEAN
        }
        val metric2 = when (secondaryMetricType) {
            MetricType.EUCLIDEAN -> EuclideanMetric()
            MetricType.ASTAR -> AStarMetric(mapData)
        }

        val secondaryResult = when (selectedAlgorithm) {
            ClusterAlgorithmType.KMEANS -> {
                KmeansAlgorithm(limitedClusterCount).run(points, metric2)
            }
            ClusterAlgorithmType.DBSCAN -> {
                DBScan(DEFAULT_EPS, DEFAULT_MIN_POINTS).run(points, metric2)
            }
        }
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