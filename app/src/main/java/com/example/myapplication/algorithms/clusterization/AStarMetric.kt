package com.example.myapplication.algorithms.clusterization

import com.example.myapplication.algorithms.routes.AStarAlgorithm;
import com.example.myapplication.algorithms.routes.Node
import com.example.myapplication.data.map.MapData

class AStarMetric(private val mapData: MapData) : IDistanceMetrics {
    private val algorithm = AStarAlgorithm(mapData)

    override suspend fun calculatingDistance(p1: Point, p2: Point): Double {
        val startNode = Node(p1.x.toInt(), p1.y.toInt())
        val endNode = Node(p2.x.toInt(), p2.y.toInt())

        val path = algorithm.findPath(startNode, endNode, speedMs = 0L) { }

        return path?.lastOrNull()?.currentCost ?: Double.POSITIVE_INFINITY
    }
}