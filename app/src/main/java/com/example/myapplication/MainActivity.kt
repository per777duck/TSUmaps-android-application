package com.example.myapplication

import android.os.Bundle
import android.view.animation.OvershootInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf("splash") }

    Crossfade(
        targetState = currentScreen,
        animationSpec = tween(durationMillis = 800, easing = LinearOutSlowInEasing),
        label = "screen_transition"
    ) { screen ->
        when (screen) {
            "splash" -> SplashScreen(onFinished = { currentScreen = "main" })
            "main" -> MainMenuScreen()
        }
    }
}

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val introAlpha = remember { Animatable(0f) }
    val introScale = remember { Animatable(0.8f) }

    // Анимация змейки (без изменений, исправленная версия)
    val infiniteTransition = rememberInfiniteTransition(label = "worm_loader")

    val baseRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "base_rotation"
    )

    val headAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2500
                0f at 0 with FastOutSlowInEasing
                360f at 2500
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "head"
    )

    val tailAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2500
                0f at 600 with FastOutSlowInEasing
                360f at 2500
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "tail"
    )

    LaunchedEffect(Unit) {
        launch { introAlpha.animateTo(1f, tween(1000)) }
        launch { introScale.animateTo(1f, tween(1200, easing = OvershootInterpolator(1.5f).toEasing())) }

        delay(3000)
        onFinished()
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(300.dp)
                .alpha(introAlpha.value)
                .scale(introScale.value),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.tgu_logo),
                contentDescription = null,
                modifier = Modifier.size(130.dp)
            )

            Canvas(modifier = Modifier.size(230.dp)) {
                rotate(baseRotation) {
                    val sweep = if (headAngle >= tailAngle) {
                        headAngle - tailAngle
                    } else {
                        (360f - tailAngle) + headAngle
                    }

                    drawArc(
                        color = Color(0xFF003D7C),
                        startAngle = tailAngle - 90f,
                        sweepAngle = sweep.coerceAtLeast(8f),
                        useCenter = false,
                        style = Stroke(width = 14f, cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}

@Composable
fun MainMenuScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Главное меню Навигатора", style = MaterialTheme.typography.headlineLarge)
    }
}

fun android.view.animation.Interpolator.toEasing() = Easing { x -> getInterpolation(x) }