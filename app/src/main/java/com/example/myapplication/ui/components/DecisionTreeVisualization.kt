package com.example.myapplication.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.algorithms.decisiontree.DecisionTreeNode

@Composable
fun DecisionTreeVisual(
    node: DecisionTreeNode,
    modifier: Modifier = Modifier,
    depth: Int = 0
) {
    val pad = (depth * 12).dp
    when (node) {
        is DecisionTreeNode.Leaf -> {
            Text(
                text = "→ ${uiPlaceTitle(node.label)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TGU_Blue,
                modifier = modifier.padding(start = pad, top = 4.dp, bottom = 4.dp)
            )
        }

        is DecisionTreeNode.Internal -> {
            Column(modifier = modifier
                .padding(start = pad)
                .fillMaxWidth()) {
                Text(
                    text = uiFeatureTitle(node.attribute),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp))
                node.children.entries.forEach { (value, child) ->
                    Column(Modifier.padding(bottom = 8.dp)) {
                        Text(
                            text = "если ${uiValueLabel(node.attribute, value)}:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        DecisionTreeVisual(child, Modifier, depth + 1)
                    }
                }
            }
        }
    }
}
