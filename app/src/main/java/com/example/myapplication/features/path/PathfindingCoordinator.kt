package com.example.myapplication.features.path

import com.example.myapplication.AStarAlgorithm
import com.example.myapplication.Node

object PathfindingCoordinator {
    fun parseNode(input: String): Node {
        return try {
            val parts = input.split(",")
            Node(parts[0].trim().toInt(), parts[1].trim().toInt())
        } catch (_: Exception) {
            Node(0, 0)
        }
    }

    suspend fun buildPath(
        algorithm: AStarAlgorithm,
        fromText: String,
        toText: String,
        speedMs: Long = 10L,
        onPathUpdate: (List<Node>) -> Unit
    ) {
        val startNode = parseNode(fromText)
        val endNode = parseNode(toText)
        algorithm.findPath(
            start = startNode,
            end = endNode,
            speedMs = speedMs
        ) { state ->
            onPathUpdate(state.currentPath)
        }
    }
}
