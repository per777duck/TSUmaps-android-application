package com.example.myapplication.algorithms.clusterization

interface IDistanceMetrics {
    suspend fun calculatingDistance(p1: Point, p2: Point): Double
}