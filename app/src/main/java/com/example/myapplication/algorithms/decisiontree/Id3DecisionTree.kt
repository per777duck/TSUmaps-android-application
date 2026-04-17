package com.example.myapplication.algorithms.decisiontree

import kotlin.math.ln

object Id3DecisionTree {

    fun build(training: TrainingSet, maxDepth: Int = Int.MAX_VALUE): DecisionTreeNode {
        val attrs = training.featureNames.toList()
        return buildSubtree(
            rows = training.rows,
            attributeNames = attrs,
            targetName = training.targetName,
            maxDepth = maxDepth,
            currentDepth = 0
        )
    }

    fun predict(features: Map<String, String>, root: DecisionTreeNode): PredictionResult {
        val path = mutableListOf<String>()
        var node: DecisionTreeNode = root
        while (node is DecisionTreeNode.Internal) {
            val attr = node.attribute
            val value = features[attr]
            if (value == null) {
                return PredictionResult(
                    label = node.fallbackLabel,
                    pathSteps = path,
                    unresolvedReason = "Не задан признак: $attr"
                )
            }
            path.add("$attr = $value")
            val next = node.children[value]
            if (next == null) {
                return PredictionResult(
                    label = node.fallbackLabel,
                    pathSteps = path,
                    unresolvedReason = "Значение «$value» не встречалось в обучении для узла $attr"
                )
            }
            node = next
        }
        return PredictionResult(label = (node as DecisionTreeNode.Leaf).label, pathSteps = path)
    }

    fun compressRedundant(node: DecisionTreeNode): DecisionTreeNode {
        return when (node) {
            is DecisionTreeNode.Leaf -> node
            is DecisionTreeNode.Internal -> {
                val compressed = node.children.mapValues { compressRedundant(it.value) }
                val onlyLeaves = compressed.values.all { it is DecisionTreeNode.Leaf }
                if (onlyLeaves) {
                    val labels = compressed.values.map { (it as DecisionTreeNode.Leaf).label }.distinct()
                    if (labels.size == 1) {
                        return DecisionTreeNode.Leaf(labels.first())
                    }
                }
                node.copy(children = compressed)
            }
        }
    }

    fun formatTree(node: DecisionTreeNode, indent: String = ""): String {
        return when (node) {
            is DecisionTreeNode.Leaf -> "${indent}→ ${node.label}\n"
            is DecisionTreeNode.Internal -> buildString {
                appendLine("$indent[${node.attribute}]")
                val entries = node.children.entries.toList()
                entries.forEachIndexed { index, (value, child) ->
                    val last = index == entries.lastIndex
                    val branch = if (last) "└─ " else "├─ "
                    val nextIndent = indent + if (last) "   " else "│  "
                    append(indent).append(branch).append(value).append('\n')
                    when (child) {
                        is DecisionTreeNode.Leaf -> append(nextIndent).append("→ ").appendLine(child.label)
                        is DecisionTreeNode.Internal -> append(formatTree(child, nextIndent))
                    }
                }
            }
        }
    }

    fun countNodes(node: DecisionTreeNode): Int {
        return when (node) {
            is DecisionTreeNode.Leaf -> 1
            is DecisionTreeNode.Internal -> 1 + node.children.values.sumOf { countNodes(it) }
        }
    }

    private fun buildSubtree(
        rows: List<Map<String, String>>,
        attributeNames: List<String>,
        targetName: String,
        maxDepth: Int,
        currentDepth: Int
    ): DecisionTreeNode {
        val labels = rows.map { it[targetName]!! }.distinct()
        if (labels.size == 1) {
            return DecisionTreeNode.Leaf(labels.first())
        }
        if (attributeNames.isEmpty() || currentDepth >= maxDepth) {
            return DecisionTreeNode.Leaf(majorityLabel(rows, targetName))
        }
        val best = bestAttribute(rows, attributeNames, targetName)
            ?: return DecisionTreeNode.Leaf(majorityLabel(rows, targetName))
        val values = rows.map { it[best]!! }.distinct().sorted()
        val remaining = attributeNames.filter { it != best }
        val fallback = majorityLabel(rows, targetName)
        val children = LinkedHashMap<String, DecisionTreeNode>()
        for (v in values) {
            val subset = rows.filter { it[best] == v }
            children[v] = if (subset.isEmpty()) {
                DecisionTreeNode.Leaf(fallback)
            } else {
                buildSubtree(subset, remaining, targetName, maxDepth, currentDepth + 1)
            }
        }
        return DecisionTreeNode.Internal(attribute = best, children = children, fallbackLabel = fallback)
    }

    private fun majorityLabel(rows: List<Map<String, String>>, targetName: String): String {
        return rows.groupingBy { it[targetName]!! }.eachCount().maxBy { it.value }.key
    }

    private fun entropy(rows: List<Map<String, String>>, targetName: String): Double {
        if (rows.isEmpty()) return 0.0
        val n = rows.size.toDouble()
        return rows.groupingBy { it[targetName]!! }.eachCount().values.sumOf { c ->
            val p = c / n
            if (p <= 0.0) 0.0 else -p * (ln(p) / ln(2.0))
        }
    }

    private fun bestAttribute(
        rows: List<Map<String, String>>,
        attrs: List<String>,
        targetName: String
    ): String? {
        if (attrs.isEmpty()) return null
        val baseH = entropy(rows, targetName)
        var best: String? = null
        var bestGain = -1.0
        val n = rows.size.toDouble()
        for (a in attrs) {
            val values = rows.map { it[a]!! }.distinct()
            var remainder = 0.0
            for (v in values) {
                val subset = rows.filter { it[a] == v }
                remainder += subset.size / n * entropy(subset, targetName)
            }
            val gain = baseH - remainder
            if (gain > bestGain + 1e-12) {
                bestGain = gain
                best = a
            }
        }
        return best
    }
}
