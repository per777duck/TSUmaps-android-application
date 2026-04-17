package com.example.myapplication.ui.components

import android.view.animation.Interpolator
import android.view.animation.OvershootInterpolator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.SwapCalls
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.annotation.StringRes
import com.example.myapplication.R
import com.example.myapplication.data.map.MapData
import com.example.myapplication.data.map.MapMatrixLoader
import com.example.myapplication.data.venues.MetricType
import com.example.myapplication.data.venues.VenueType
import com.example.myapplication.data.venues.listOfVenues
import com.example.myapplication.ui.screens.AntsScreen
import com.example.myapplication.ui.screens.ClusteringScreen
import com.example.myapplication.ui.screens.DecisionTreeScreen
import com.example.myapplication.ui.screens.GeneticMealRouteScreen
import com.example.myapplication.ui.screens.NavigatorScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf("splash") }

    if (currentScreen == "splash") {
        SplashScreen(onFinished = { currentScreen = "main" })
    } else {
        MainScreenWithNavigation()
    }
}

enum class AlgorithmTab(@StringRes val titleRes: Int, val icon: ImageVector) {
    Navigation(R.string.tab_navigation, Icons.Default.LocationOn),
    Clustering(R.string.tab_clustering, Icons.Default.GridView),
    Genetic(R.string.tab_genetic, Icons.Default.SwapCalls),
    Ants(R.string.tab_ants, Icons.Default.BugReport),
    DecisionTree(R.string.tab_decision_tree, Icons.Default.AccountTree),
    NeuralNet(R.string.tab_neural_net, Icons.Default.Edit)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenWithNavigation() {
    var selectedTab by remember { mutableStateOf(AlgorithmTab.Navigation) }
    var pendingVenueOnMap by remember { mutableStateOf<Offset?>(null) }
    var isComparisonMode by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf<VenueType?>(null) }
    var selectedMetric by remember { mutableStateOf(MetricType.EUCLIDEAN) }
    var clusterCount by remember { mutableStateOf(2) }

    val context = LocalContext.current
    var mapData by remember { mutableStateOf<MapData?>(null) }
    var mapLoadingFailed by remember { mutableStateOf(false) }

    LaunchedEffect(context) {
        mapLoadingFailed = false
        mapData = runCatching {
            withContext(Dispatchers.Default) {
                MapMatrixLoader(context).mapToMatrix(R.drawable.map_mask)
            }
        }.getOrElse {
            mapLoadingFailed = true
            null
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                AlgorithmTab.entries.forEach { tab ->
                    val tabTitle = stringResource(tab.titleRes)
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tabTitle) },
                        label = { Text(tabTitle, fontSize = 10.sp, maxLines = 1) },
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
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.tgu_logo),
                                contentDescription = stringResource(R.string.content_desc_tgu_logo_en),
                                modifier = Modifier.size(28.dp)
                            )
                            Text(stringResource(R.string.main_title), fontWeight = FontWeight.Bold, color = TGU_Blue)
                        }
                        Text(
                            stringResource(R.string.main_subtitle),
                            fontSize = 11.sp,
                            color = Color(0xFF5C6B7A),
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )

            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                val loadedMapData = mapData
                if (loadedMapData == null) {
                    val loadingText = if (mapLoadingFailed) {
                        stringResource(R.string.map_load_failed)
                    } else {
                        stringResource(R.string.map_loading)
                    }
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = loadingText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (mapLoadingFailed) Color(0xFFB00020) else Color(0xFF5C6B7A),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    when (selectedTab) {
                        AlgorithmTab.Navigation -> NavigatorScreen(
                            mapData = loadedMapData,
                            venueFocusMapPosition = pendingVenueOnMap,
                            onVenueFocusHandled = { pendingVenueOnMap = null }
                        )
                        AlgorithmTab.Clustering -> {
                            AlgorithmCard(
                                tab = selectedTab,
                                venueType = selectedType,
                                metricType = selectedMetric,
                                clusterCount = clusterCount,
                                isComparisonMode = isComparisonMode,
                                mapData = loadedMapData,
                                onVenueTypeChange = { selectedType = it },
                                onMetricChange = { selectedMetric = it },
                                onClusterCountChange = { clusterCount = it },
                                onComparisonModeChange = { isComparisonMode = it }
                            )
                        }
                        else -> AlgorithmCard(
                            selectedTab,
                            mapData = loadedMapData,
                            onOpenRecommendedPlaceOnMap = { offset ->
                                pendingVenueOnMap = offset
                                selectedTab = AlgorithmTab.Navigation
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AlgorithmCard(
    tab: AlgorithmTab,
    venueType: VenueType? = null,
    metricType: MetricType? = null,
    clusterCount: Int = 2,
    isComparisonMode: Boolean = false,
    mapData: MapData,
    onVenueTypeChange: (VenueType?) -> Unit = {},
    onMetricChange: (MetricType) -> Unit = {},
    onClusterCountChange: (Int) -> Unit = {},
    onComparisonModeChange: (Boolean) -> Unit = {},
    onOpenRecommendedPlaceOnMap: (Offset) -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxSize(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        when (tab) {
            AlgorithmTab.Clustering -> ClusteringScreen(
                venues = listOfVenues,
                selectedType = venueType,
                selectedMetric = metricType,
                clusterCount = clusterCount,
                isComparisonMode = isComparisonMode,
                mapData = mapData,
                onVenueTypeChange = onVenueTypeChange,
                onMetricChange = onMetricChange,
                onClusterCountChange = onClusterCountChange,
                onComparisonModeChange = onComparisonModeChange
            )
            AlgorithmTab.Ants -> AntsScreen(mapData = mapData)
            AlgorithmTab.Genetic -> GeneticMealRouteScreen(mapData = mapData)
            AlgorithmTab.DecisionTree -> DecisionTreeScreen(
                onOpenPlaceOnMap = onOpenRecommendedPlaceOnMap
            )
            else -> Column(
                modifier = Modifier.padding(24.dp).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val tabTitle = stringResource(tab.titleRes)
                Text(tabTitle, style = MaterialTheme.typography.headlineSmall, color = TGU_Blue)
                Spacer(modifier = Modifier.height(20.dp))
                Text(stringResource(R.string.algorithm_placeholder, tabTitle), color = Color.Gray)
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
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing))
    )
    val headAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(keyframes { durationMillis = 2500; 0f at 0; 360f at 2500 })
    )
    val tailAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(keyframes { durationMillis = 2500; 0f at 600; 360f at 2500 })
    )

    LaunchedEffect(Unit) {
        launch { introAlpha.animateTo(1f, tween(1000)) }
        launch {
            introScale.animateTo(
                1f,
                tween(1200, easing = OvershootInterpolator(1.5f).toEasing())
            )
        }
        delay(3000)
        onFinished()
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .alpha(introAlpha.value)
                .scale(introScale.value)
        ) {
            Box(
                modifier = Modifier.size(300.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.tgu_logo),
                    contentDescription = stringResource(R.string.content_desc_tgu_logo),
                    modifier = Modifier.size(130.dp)
                )
                Canvas(modifier = Modifier.size(230.dp)) {
                    rotate(baseRotation) {
                        val sweep =
                            if (headAngle >= tailAngle) headAngle - tailAngle else (360f - tailAngle) + headAngle
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
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                stringResource(R.string.splash_university_title),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TGU_Blue
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                stringResource(R.string.splash_subtitle),
                fontSize = 13.sp,
                color = Color(0xFF5C6B7A),
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun Interpolator.toEasing() = Easing { x -> getInterpolation(x) }
