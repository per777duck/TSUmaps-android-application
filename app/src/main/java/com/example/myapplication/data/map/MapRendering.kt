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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.myapplication.R
import kotlin.math.min

object MapRendering {
    const val MIN_ZOOM = 1f
    const val MAX_ZOOM = 5f
    private const val PAN_OVERSCROLL_RATIO = 2.0f

    data class MapTransform(
        val scale: Float,
        val offsetX: Float,
        val offsetY: Float,
        val mapWidth: Float,
        val mapHeight: Float
    )

    @SuppressLint("UnusedBoxWithConstraintsScope")
    @Composable
    fun MapSurface(
        mapData: MapData,
        modifier: Modifier = Modifier,
        minZoom: Float = MIN_ZOOM,
        maxZoom: Float = MAX_ZOOM,
        mapDrawableId: Int = R.drawable.map_original,
        content: @Composable (MapTransform) -> Unit = {}
    ) {
        val logicalMapWidth = mapData.width.toFloat().coerceAtLeast(1f)
        val logicalMapHeight = mapData.length.toFloat().coerceAtLeast(1f)

        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .clipToBounds()
                .background(Color(0xFFF0F0F0))
        ) {
            val viewportWidth = maxWidth.value
            val viewportHeight = maxHeight.value
            val baseScale = min(
                viewportWidth / logicalMapWidth,
                viewportHeight / logicalMapHeight
            )

            var userScale by remember { mutableStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }

            val state = rememberTransformableState { zoomChange, offsetChange, _ ->
                val newScale = (userScale * zoomChange).coerceIn(minZoom, maxZoom)
                val tentativeOffset = offset + offsetChange

                val scaledWidth = logicalMapWidth * baseScale * newScale
                val scaledHeight = logicalMapHeight * baseScale * newScale
                val baseMaxX = ((scaledWidth - viewportWidth).coerceAtLeast(0f)) / 2f
                val baseMaxY = ((scaledHeight - viewportHeight).coerceAtLeast(0f)) / 2f
                val extraX = if (newScale > 1f) viewportWidth * PAN_OVERSCROLL_RATIO else 0f
                val extraY = if (newScale > 1f) viewportHeight * PAN_OVERSCROLL_RATIO else 0f
                val maxX = baseMaxX + extraX
                val maxY = baseMaxY + extraY

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
                        .requiredSize(logicalMapWidth.toDp(), logicalMapHeight.toDp())
                        .graphicsLayer(
                            scaleX = finalScale,
                            scaleY = finalScale,
                            translationX = offset.x,
                            translationY = offset.y,
                            transformOrigin = TransformOrigin.Center
                        )
                ) {
                    Image(
                        painter = painterResource(id = mapDrawableId),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )

                    content(
                        MapTransform(
                            scale = finalScale,
                            offsetX = offset.x,
                            offsetY = offset.y,
                            mapWidth = logicalMapWidth,
                            mapHeight = logicalMapHeight
                        )
                    )
                }
            }
        }
    }

    @Composable
    fun TguMapWrapper(
        mapData: MapData,
        modifier: Modifier = Modifier,
        content: @Composable (Float, Float, Float) -> Unit
    ) {
        MapSurface(
            mapData = mapData,
            modifier = modifier
        ) { transform ->
            content(transform.scale, transform.offsetX, transform.offsetY)
        }
    }
}

private fun Float.toDp(): Dp = this.dp