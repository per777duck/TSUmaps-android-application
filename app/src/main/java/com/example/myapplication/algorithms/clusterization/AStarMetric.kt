package com.example.myapplication.algorithms.clusterization

import com.example.myapplication.algorithms.routes.AStarAlgorithm
import com.example.myapplication.algorithms.routes.Node

class AStarMetric(private val algorithm: AStarAlgorithm) : IDistanceMetrics {
    private val distanceCache = mutableMapOf<SegmentKey, Double>()
    private val pathCache = mutableMapOf<SegmentKey, List<Node>?>()

    override suspend fun calculatingDistance(p1: Point, p2: Point): Double {
        val startNode = algorithm.nearestWalkable(p1.x.toInt(), p1.y.toInt()) ?: return Double.POSITIVE_INFINITY
        val endNode = algorithm.nearestWalkable(p2.x.toInt(), p2.y.toInt()) ?: return Double.POSITIVE_INFINITY

        if (startNode == endNode) return 0.0

        val key = SegmentKey(startNode.x, startNode.y, endNode.x, endNode.y)
        return distanceCache[key] ?: computeAndCache(startNode, endNode)
    }

    fun pathBetween(p1: Point, p2: Point): List<Node>? {
        val startNode = algorithm.nearestWalkable(p1.x.toInt(), p1.y.toInt()) ?: return null
        val endNode = algorithm.nearestWalkable(p2.x.toInt(), p2.y.toInt()) ?: return null
        if (startNode == endNode) return listOf(startNode)

        val key = SegmentKey(startNode.x, startNode.y, endNode.x, endNode.y)
        val cached = pathCache[key]
        if (cached != null || pathCache.containsKey(key)) return cached

        computeAndCache(startNode, endNode)
        return pathCache[key]
    }

    private fun computeAndCache(startNode: Node, endNode: Node): Double {
        val directKey = SegmentKey(startNode.x, startNode.y, endNode.x, endNode.y)
        val reverseKey = SegmentKey(endNode.x, endNode.y, startNode.x, startNode.y)
        val path = algorithm.findPathSync(startNode, endNode)
        val distance = path?.lastOrNull()?.currentCost ?: Double.POSITIVE_INFINITY

        distanceCache[directKey] = distance
        distanceCache[reverseKey] = distance
        pathCache[directKey] = path
        pathCache[reverseKey] = path?.asReversed()

        return distance
    }

    private data class SegmentKey(
        val fromX: Int,
        val fromY: Int,
        val toX: Int,
        val toY: Int
    )
}