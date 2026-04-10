package com.example.myapplication.data.map

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.myapplication.R

object MapRendering {
    @SuppressLint("UnusedBoxWithConstraintsScope")
    @Composable
    fun TguMapWrapper(
        modifier: Modifier = Modifier,
        content: @Composable (Float, Float, Float) -> Unit
    ) {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .clipToBounds()
                .background(Color(0xFFF0F0F0))
        ) {
            val baseScale = maxWidth.value / 784f

            var userScale by remember { mutableStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }

            val state = rememberTransformableState { zoomChange, offsetChange, _ ->
                val newScale = (userScale * zoomChange).coerceIn(1f, 5f)
                val tentativeOffset = offset + offsetChange

                val scaledWidth = 784f * baseScale * newScale
                val scaledHeight = 757f * baseScale * newScale
                val maxX = ((scaledWidth - maxWidth.value).coerceAtLeast(0f)) / 2f
                val maxY = ((scaledHeight - maxHeight.value).coerceAtLeast(0f)) / 2f

                userScale = newScale
                offset = Offset(
                    x = tentativeOffset.x.coerceIn(-maxX, maxX),
                    y = tentativeOffset.y.coerceIn(-maxY, maxY)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .transformable(state = state),
                contentAlignment = Alignment.Center
            ) {
                val finalScale = baseScale * userScale

                Box(
                    modifier = Modifier
                        .requiredSize(784.dp, 757.dp)
                        .graphicsLayer(
                            scaleX = finalScale,
                            scaleY = finalScale,
                            translationX = offset.x,
                            translationY = offset.y,
                            transformOrigin = TransformOrigin.Center
                        )
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.map_original),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )

                    content(finalScale, offset.x, offset.y)
                }
            }
        }
    }
}