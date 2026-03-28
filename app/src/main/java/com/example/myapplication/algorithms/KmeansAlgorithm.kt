package com.example.myapplication.algorithms

import com.example.myapplication.algorithms.models.IDistanceMetrics
import com.example.myapplication.algorithms.models.Point
import com.example.myapplication.algorithms.models.Cluster
import kotlin.math.pow
import kotlin.math.sqrt

class KmeansAlgorithm(
    private val k: Int,
    private val maxIterations: Int = 100
) {
    private fun convergence(
        oldCentroids: List<Point>,
        newCentroids: List<Point>,
        epsilon: Double = 1e-6
    ): Boolean {
        return oldCentroids.zip(newCentroids).all {(old, new) ->
            val dist = sqrt((old.x - new.x).pow(2) + (old.y - new.y).pow(2))
            dist < epsilon
        }
    }

    fun run(points: List<Point>, metric: IDistanceMetrics): List<Cluster>{
        var centroids = points.shuffled().take(k).map { Point(it.id, it.x, it.y) }
        val clusters = List(k) { ind -> Cluster(ind, centroids[ind]) }

        repeat(maxIterations){
            clusters.forEach { it.points.clear() }

            points.forEach { point ->
                var minDist = Double.MAX_VALUE
                var bestClusterId = 0
                clusters.forEachIndexed { index, cluster ->
                    val dist = metric.CalculatingDistance(cluster.centroid, point)
                    if (minDist > dist){
                        minDist = dist
                        bestClusterId = index
                    }
                }
                clusters[bestClusterId].points.add(point)
            }

            val newCentroids = clusters.map {cluster ->
                if (cluster.points.isEmpty()) {
                    return@map cluster.centroid
                }
                val avX = cluster.points.map { it.x }.average()
                val avY = cluster.points.map { it.y }.average()
                Point(-1, avX, avY)
            }

            if (convergence(centroids, newCentroids)) return@repeat
            centroids = newCentroids
            clusters.forEachIndexed { index, cluster -> cluster.centroid = centroids[index] }
        }
        return clusters
    }
}