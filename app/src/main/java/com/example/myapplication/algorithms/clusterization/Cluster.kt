package com.example.myapplication.algorithms.clusterization

data class Cluster(val id: Int, var centroid: Point,
                   val points: MutableList<Point> = mutableListOf())