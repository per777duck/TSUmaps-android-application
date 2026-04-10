package com.example.myapplication.algorithms.models

import kotlin.math.pow
import kotlin.math.sqrt

class EuclideanMetric : IDistanceMetrics {
    override suspend fun calculatingDistance(p1: Point, p2: Point): Double {
        return sqrt((p1.x - p2.x).pow(2) + (p1.y - p2.y).pow(2))
    }
}