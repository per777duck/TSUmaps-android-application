package com.example.myapplication.algorithms.models

import com.example.myapplication.algorithms.AStarAlgorithm
import com.example.myapplication.algorithms.Node

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

    fun buildPolyline(orderedPoints: List<Point>): List<Point> {
        if (orderedPoints.isEmpty()) return emptyList()
        if (orderedPoints.size == 1) return orderedPoints

        val polyline = mutableListOf<Point>()
        for (i in 0 until orderedPoints.lastIndex) {
            val from = orderedPoints[i]
            val to = orderedPoints[i + 1]
            val segment = pathBetween(from, to)

            if (segment != null && segment.isNotEmpty()) {
                segment.forEach { node ->
                    val pathPoint = Point(id = -1, x = node.x.toDouble(), y = node.y.toDouble())
                    val isDuplicate = polyline.lastOrNull()?.let {
                        it.x == pathPoint.x && it.y == pathPoint.y
                    } ?: false
                    if (!isDuplicate) {
                        polyline.add(pathPoint)
                    }
                }
            } else {
                if (polyline.isEmpty()) {
                    polyline.add(from)
                } else {
                    val last = polyline.last()
                    if (last.x != from.x || last.y != from.y) {
                        polyline.add(from)
                    }
                }

                val last = polyline.last()
                if (last.x != to.x || last.y != to.y) {
                    polyline.add(to)
                }
            }
        }

        return polyline
    }

    private fun pathBetween(p1: Point, p2: Point): List<Node>? {
        val startNode = algorithm.nearestWalkable(p1.x.toInt(), p1.y.toInt()) ?: return null
        val endNode = algorithm.nearestWalkable(p2.x.toInt(), p2.y.toInt()) ?: return null
        if (startNode == endNode) return listOf(startNode)

        val key = SegmentKey(startNode.x, startNode.y, endNode.x, endNode.y)
        val cached = pathCache[key]
        if (cached != null || pathCache.containsKey(key)) {
            return cached
        }

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