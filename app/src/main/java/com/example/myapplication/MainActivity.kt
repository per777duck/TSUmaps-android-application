package com.example.myapplication

import android.os.Bundle
import android.view.animation.OvershootInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntSize

val TGU_Blue = Color(0xFF003D7C)
val TGU_Gold = Color(0xFFC5A358)
val TGU_LightGray = Color(0xFFF5F7FA)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TGUTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = TGU_LightGray) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun TGUTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = TGU_Blue,
            secondary = TGU_Gold,
            surface = Color.White
        ),
        shapes = Shapes(
            extraLarge = RoundedCornerShape(32.dp)
        ),
        content = content
    )
}

@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf("splash") }

    if (currentScreen == "splash") {
        SplashScreen(onFinished = { currentScreen = "main" })
    } else {
        MainScreenWithNavigation()
    }
}

enum class AlgorithmTab(val title: String, val icon: ImageVector) {
    Navigation("A*", Icons.Default.LocationOn),
    Clustering("Кластеры", Icons.Default.GridView),
    Genetic("Генетика", Icons.Default.SwapCalls),
    Ants("Муравьи", Icons.Default.BugReport),
    DecisionTree("Дерево", Icons.Default.AccountTree),
    NeuralNet("Нейро", Icons.Default.Edit)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenWithNavigation() {
    var selectedTab by remember { mutableStateOf(AlgorithmTab.Navigation) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                AlgorithmTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title, fontSize = 9.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TGU_Blue,
                            indicatorColor = TGU_Gold.copy(alpha = 0.2f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            CenterAlignedTopAppBar(
                title = { Text("Навигатор ТГУ", fontWeight = FontWeight.Bold, color = TGU_Blue) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )

            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                AnimatedContent(
                    targetState = selectedTab,
                    label = "tab_transition"
                ) { targetTab ->
                    AlgorithmCard(targetTab)
                }
            }
        }
    }
}

@Composable
fun AlgorithmCard(tab: AlgorithmTab) {
    Card(
        modifier = Modifier.fillMaxSize(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
//        РАСКОМЕНТИТЬ

//        if (tab == AlgorithmTab.Clustering){
//            MapClusteringScreen()
//        }
        Column(
            modifier = Modifier.padding(24.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(tab.title, style = MaterialTheme.typography.headlineSmall, color = TGU_Blue)
            Spacer(modifier = Modifier.height(20.dp))

            Text("Интерфейс для алгоритма ${tab.title} будет здесь", color = Color.Gray)
        }
    }
}

enum class MetricType { EUCLIDEAN, PEDESTRIAN }

data class Place(
    val id: Int,
    val name: String,
    val position: Offset,
    val clusterId: Int,
    val metric: MetricType
)

val ClusterColors = listOf(
    Color(0xFFEF5350),
    Color(0xFF42A5F5),
    Color(0xFF66BB6A),
    Color(0xFFFFEE58),
    Color(0xFFAB47BC),
    Color(0xFFFFA726),
    Color(0xFF26C6DA),
    Color(0xFF78909C),
    Color(0xFF8D6E63),
    Color(0xFFEC407A),
    Color(0xFF26A69A),
    Color(0xFFD4E157),
    Color(0xFF5C6BC0),
    Color(0xFFFF7043),
    Color(0xFF9CCC65)
)

@Composable
fun MapClusteringScreen(
    places: List<Place>,
){
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    //ПОМЕНЯТЬ MAP НА MAP_ORIGINAL

    val imageBitmap = ImageBitmap.imageResource(id = R.drawable.map)
    val imageSize = IntSize(imageBitmap.width, imageBitmap.height)

    Box(
        modifier = Modifier.fillMaxSize()
            .pointerInput(Unit){
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(0.5f, 5f)

                    val maxX = (imageSize.width * newScale - size.width).coerceAtLeast(0f)
                    val maxY = (imageSize.height * newScale - size.height).coerceAtLeast(0f)

                    val newOffset = Offset(
                        x = (offset.x + pan.x).coerceIn(-maxX, 0f),
                        y = (offset.y + pan.y).coerceIn(-maxY, 0f)
                    )

                    scale = newScale
                    offset = newOffset
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()){
            withTransform({
                translate(left = offset.x, top = offset.y)
                scale(scale, scale, pivot = Offset.Zero)
            }) {
                drawImage(image = imageBitmap, dstSize = imageSize)
                places.forEach { place ->
                    val clusterColor = ClusterColors[place.clusterId % ClusterColors.size]

                    val basedRadius = 15f
                    val adaptedRadius = basedRadius / scale

                    drawCircle(
                        color = clusterColor,
                        radius = adaptedRadius,
                        center = place.position,
                        alpha = 0.8f
                    )

                    drawCircle(
                        color = Color.Black,
                        radius = adaptedRadius,
                        center = place.position,
                        style = Stroke(width = 2f / scale)
                    )
                }
            }
        }
    }
}

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val introAlpha = remember { Animatable(0f) }
    val introScale = remember { Animatable(0.8f) }
    val infiniteTransition = rememberInfiniteTransition(label = "worm_loader")

    val baseRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing))
    )

    val headAngle by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(keyframes { durationMillis = 2500; 0f at 0; 360f at 2500 })
    )

    val tailAngle by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(keyframes { durationMillis = 2500; 0f at 600; 360f at 2500 })
    )

    LaunchedEffect(Unit) {
        launch { introAlpha.animateTo(1f, tween(1000)) }
        launch { introScale.animateTo(1f, tween(1200, easing = OvershootInterpolator(1.5f).toEasing())) }
        delay(3000)
        onFinished()
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.size(300.dp).alpha(introAlpha.value).scale(introScale.value), contentAlignment = Alignment.Center) {

            Image(
                painter = painterResource(id = R.drawable.tgu_logo),
                contentDescription = "Логотип ТГУ",
                modifier = Modifier.size(130.dp)
            )

            Canvas(modifier = Modifier.size(230.dp)) {
                rotate(baseRotation) {
                    val sweep = if (headAngle >= tailAngle) headAngle - tailAngle else (360f - tailAngle) + headAngle
                    drawArc(
                        color = TGU_Blue,
                        startAngle = tailAngle - 90f,
                        sweepAngle = sweep.coerceAtLeast(8f),
                        useCenter = false,
                        style = Stroke(width = 12f, cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}

fun android.view.animation.Interpolator.toEasing() = Easing { x -> getInterpolation(x) }