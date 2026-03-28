package com.example.myapplication.algorithms.models

data class Cluster(val id: Int, var centroid: Point,
                   val points: MutableList<Point> = mutableListOf())