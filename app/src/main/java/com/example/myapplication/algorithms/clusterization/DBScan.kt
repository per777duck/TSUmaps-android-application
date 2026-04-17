package com.example.myapplication.algorithms.clusterization

class DBScan(
    private val eps: Double,
    private val minPoints: Int = 4
) {
    private companion object {
        const val UNVISITED = -2
        const val SINGLE = -1
    }

    suspend fun run(points: List<Point>, metric: IDistanceMetrics): List<Cluster> {
        if (points.isEmpty()) return emptyList()

        val pointStatuses = IntArray(points.size) { UNVISITED }
        var currentClusterId = 0

        for (index in points.indices) {
            if (pointStatuses[index] != UNVISITED) continue

            val neighbors = findingNeighbors(index, points, metric)

            if (neighbors.size < minPoints) {
                pointStatuses[index] = SINGLE
                continue
            }

            expandCluster(
                startIndex = index,
                neighbors = neighbors,
                clusterId = currentClusterId,
                points = points,
                metric = metric,
                pointStatuses = pointStatuses
            )
            currentClusterId += 1
        }

        val multipleClusters = pointStatuses
            .withIndex()
            .filter { (_, pointStatus) -> pointStatus >= 0 }
            .groupBy({ it.value }, { points[it.index] })
            .toSortedMap()
            .values
            .toList()

        val singleClusters = pointStatuses
            .withIndex()
            .filter { (_, pointStatus) -> pointStatus == SINGLE }
            .map { listOf(points[it.index]) }

        val allClusters = multipleClusters + singleClusters
        return allClusters.mapIndexed { clusterId, clusterPoints ->
            Cluster(
                id = clusterId,
                centroid = calculateCentroid(clusterPoints),
                points = clusterPoints.toMutableList()
            )
        }
    }

    private suspend fun expandCluster(
        startIndex: Int,
        neighbors: List<Int>,
        clusterId: Int,
        points: List<Point>,
        metric: IDistanceMetrics,
        pointStatuses: IntArray
    ) {
        val q = ArrayDeque(neighbors)
        pointStatuses[startIndex] = clusterId

        while (q.isNotEmpty()) {
            val currentIndex = q.removeFirst()

            if (pointStatuses[currentIndex] != UNVISITED) continue

            pointStatuses[currentIndex] = clusterId
            val currentNeighbors = findingNeighbors(currentIndex, points, metric)

            if (currentNeighbors.size >= minPoints) {
                q.addAll(currentNeighbors)
            }
        }
    }

    private suspend fun findingNeighbors(
        pointIndex: Int,
        points: List<Point>,
        metric: IDistanceMetrics
    ): List<Int> {
        val neighbors = mutableListOf<Int>()
        val point = points[pointIndex]

        for ((index, candidate) in points.withIndex()) {
            val distance = metric.calculatingDistance(point, candidate)

            if (distance <= eps) {
                neighbors.add(index)
            }
        }

        return neighbors
    }

    private fun calculateCentroid(points: List<Point>): Point {
        if (points.isEmpty()) return Point(-1, 0.0, 0.0)

        val centerX = points.map { it.x }.average()
        val centerY = points.map { it.y }.average()

        return Point(-1, centerX, centerY)
    }
}