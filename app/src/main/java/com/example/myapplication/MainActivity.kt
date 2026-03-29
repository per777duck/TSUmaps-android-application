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
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

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
                AlgorithmTab.values().forEach { tab ->
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
                title = { Text("Навигатор ТГУ", fontWeight = FontWeight.Bold, color = TGU_Blue)},
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )

            Box(modifier = Modifier.fillMaxSize().padding(16.dp))
            {
                AnimatedContent(
                    targetState = selectedTab,
                    label = "tab_transition"
                )
                { targetTab ->
                    when (targetTab) {
                        AlgorithmTab.Navigation -> {
                            NavigatorScreen()
                        }

                        else -> {
                            AlgorithmCard(targetTab)
                        }
                    }
                }
            }
        }
    }
}

fun android.view.animation.Interpolator.toEasing() = Easing { x -> getInterpolation(x) }
@Composable
fun AlgorithmCard(tab: AlgorithmTab) {
    Card(
        modifier = Modifier.fillMaxSize(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
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

fun parseNode(input: String): Node {
    return try {
        val parts = input.split(",")
        Node(parts[0].trim().toInt(), parts[1].trim().toInt())
    } catch (e: Exception) {
        Node(0, 0) // Значение по умолчанию при ошибке
    }
}

@Composable
fun InputSection(
    fromText: String, onFromChange: (String) -> Unit,
    toText: String, onToChange: (String)->Unit,
    onBuildClick: ()->Unit)
{
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
    {
        Column(modifier = Modifier.padding(10.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Text("Построить маршрут", style = MaterialTheme.typography.titleLarge)

            Row(verticalAlignment = Alignment.CenterVertically)
            {
                Text("Откуда: ", modifier = Modifier.width(70.dp))
                TextField(
                    value = fromText,
                    onValueChange = onFromChange,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically)
            {
                Text("Куда: ", modifier = Modifier.width(70.dp))
                TextField(
                    value = toText,
                    onValueChange = onToChange,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Button(
                onClick = onBuildClick,
                modifier = Modifier.fillMaxWidth().height(30.dp)
            )
            {
                Text("ПУСК")
            }
        }
    }
}
@Composable
fun NavigatorScreen()
{
    val scope = rememberCoroutineScope()

    var fromText by remember {mutableStateOf("0,0")}
    var toText by remember {mutableStateOf("50,50")}

    var currentPath by remember {mutableStateOf <List<Node>>(emptyList())}

    val grid = remember { Array(100) { IntArray(100) { 1 } } }
    val algorithm = remember { AStarAlgorithm(grid) }

    Column(modifier = Modifier.fillMaxSize())
    {
        MapSection(path = currentPath,modifier = Modifier.weight(1f))
        InputSection(
            fromText = fromText,
            onFromChange = {fromText = it},
            toText =toText,
            onToChange = {toText = it},
            onBuildClick = {
                scope.launch(Dispatchers.Default) {
                    val startNode = parseNode(fromText)
                    val endNode = parseNode(toText)

                    algorithm.findPath(
                        start = startNode,
                        end = endNode,
                        speedMs = 30L
                    ) { state ->
                        currentPath = state.currentPath
                    }
                }
            }
        )
    }
}

@Composable
fun MapSection(path: List<Node>,modifier: Modifier = Modifier)
{
    val mapShape = RoundedCornerShape(24.dp)
    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp),
        shape = mapShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        var scale by remember { mutableStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        Box(
            modifier = modifier
                .fillMaxSize()
                .clip(RectangleShape)
                .pointerInput(Unit)
                {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 5f)
                        offset += pan * scale
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
            )
            {
                Image(
                    painter = painterResource(id = R.drawable.map_original),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds

                )

                Canvas(modifier = modifier.fillMaxSize())
                {
                    if (path.isNotEmpty()) {
                        val scaleX = size.width / 100f
                        val scaleY = size.height / 100f

                        val androidPath = androidx.compose.ui.graphics.Path().apply {

                            moveTo(path[0].x * scaleX, path[0].y * scaleY)
                            path.forEach { node ->
                                lineTo(node.x * scaleX, node.y * scaleY)
                            }
                        }
                        drawPath(
                            path = androidPath,
                            color = Color.Red,
                            style = Stroke(width = 5f / scale)
                        )
                    }
                }
            }
        }
    }
}
