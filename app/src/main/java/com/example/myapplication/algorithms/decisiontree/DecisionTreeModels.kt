package com.example.myapplication.algorithms.decisiontree

sealed class DecisionTreeNode {
    data class Leaf(val label: String) : DecisionTreeNode()

    data class Internal(
        val attribute: String,
        val children: Map<String, DecisionTreeNode>,
        val fallbackLabel: String
    ) : DecisionTreeNode()
}

data class TrainingSet(
    val featureNames: List<String>,
    val targetName: String,
    val rows: List<Map<String, String>>
)

data class PredictionResult(
    val label: String?,
    val pathSteps: List<String>,
    val unresolvedReason: String? = null
)
