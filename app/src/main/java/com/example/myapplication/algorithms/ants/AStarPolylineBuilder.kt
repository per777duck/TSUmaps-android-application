package com.example.myapplication.algorithms.ants

import com.example.myapplication.algorithms.clusterization.AStarMetric
import com.example.myapplication.algorithms.clusterization.Point

fun buildPolyline(metric: AStarMetric, orderedPoints: List<Point>): List<Point> {
    if (orderedPoints.isEmpty()) return emptyList()
    if (orderedPoints.size == 1) return orderedPoints

    val polyline = mutableListOf<Point>()
    for (i in 0 until orderedPoints.lastIndex) {
        val from = orderedPoints[i]
        val to = orderedPoints[i + 1]
        val segment = metric.pathBetween(from, to)

        if (segment != null && segment.isNotEmpty()) {
            segment.forEach { node ->
                val pathPoint = Point(id = -1, x = node.x.toDouble(), y = node.y.toDouble())
                val isDuplicate = polyline.lastOrNull()?.let {
                    it.x == pathPoint.x && it.y == pathPoint.y
                } ?: false
                if (!isDuplicate) polyline.add(pathPoint)
            }
        } else {
            val fromDuplicate = polyline.lastOrNull()?.let { it.x == from.x && it.y == from.y } ?: false
            if (!fromDuplicate) polyline.add(from)

            val toDuplicate = polyline.lastOrNull()?.let { it.x == to.x && it.y == to.y } ?: false
            if (!toDuplicate) polyline.add(to)
        }
    }

    return polyline
}
