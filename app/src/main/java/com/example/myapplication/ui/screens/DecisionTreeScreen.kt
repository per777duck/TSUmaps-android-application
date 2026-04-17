package com.example.myapplication.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.myapplication.algorithms.decisiontree.CsvTrainingParser
import com.example.myapplication.algorithms.decisiontree.DecisionTreeNode
import com.example.myapplication.algorithms.decisiontree.Id3DecisionTree
import com.example.myapplication.algorithms.decisiontree.TrainingSet
import com.example.myapplication.data.venues.foodVenueForRecommendedPlace
import com.example.myapplication.ui.TGU_Blue
import com.example.myapplication.ui.TGU_Gold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val BUILTIN_TRAINING_CSV = """location,budget,time_available,food_type,queue_tolerance,weather,recommended_place
main_building,low,medium,full_meal,medium,good,Main_Cafeteria
main_building,low,short,snack,low,good,Yarche
main_building,medium,short,coffee,low,good,Bus_Stop_Coffee
main_building,high,medium,coffee,medium,good,Starbooks
second_building,low,very_short,snack,low,good,Vending_Machine
second_building,medium,short,coffee,medium,good,Second_Building_Cafe
second_building,medium,medium,full_meal,medium,good,Main_Cafeteria
second_building,low,short,snack,low,bad,Vending_Machine
campus_center,medium,short,pancakes,medium,good,Siberian_Pancakes
main_building,low,very_short,coffee,low,good,Bus_Stop_Coffee
main_building,medium,very_short,snack,low,bad,Vending_Machine
main_building,high,short,coffee,high,good,Starbooks
main_building,medium,short,full_meal,high,good,Main_Cafeteria
main_building,low,short,pancakes,medium,good,Siberian_Pancakes
main_building,medium,medium,snack,medium,good,Yarche
second_building,low,very_short,coffee,low,good,Morning_Buffet
second_building,medium,short,full_meal,medium,good,Second_Building_Cafe
second_building,high,medium,full_meal,high,good,Midday_Cafe
second_building,low,short,snack,medium,good,Vending_Machine
second_building,medium,medium,pasta,medium,good,Second_Building_Cafe
second_building,medium,short,snack,low,bad,Vending_Machine
second_building,low,short,coffee,low,bad,Bus_Stop_Coffee
bus_stop,low,very_short,coffee,low,bad,Bus_Stop_Coffee
bus_stop,medium,short,coffee,medium,good,Bus_Stop_Coffee
bus_stop,low,short,snack,low,bad,Vending_Machine
bus_stop,medium,medium,full_meal,medium,good,Main_Cafeteria
campus_center,low,short,pancakes,medium,good,Siberian_Pancakes
campus_center,medium,medium,full_meal,high,good,Midday_Cafe
campus_center,high,short,coffee,medium,good,Starbooks
campus_center,medium,short,coffee,medium,good,Morning_Buffet
campus_center,low,very_short,snack,low,bad,Vending_Machine
campus_center,medium,short,full_meal,medium,bad,Main_Cafeteria
campus_center,medium,short,snack,medium,good,Yarche
main_building,medium,short,coffee,medium,bad,Morning_Buffet
main_building,low,very_short,snack,low,good,Vending_Machine
second_building,high,medium,coffee,medium,good,Starbooks
bus_stop,medium,short,snack,medium,good,Yarche
bus_stop,high,medium,full_meal,high,good,Midday_Cafe"""

@Composable
private fun StepHeader(number: Int, title: String, subtitle: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(TGU_Blue.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "$number",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TGU_Blue
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TGU_Blue)
            subtitle?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DecisionTreeScreen(
    onOpenPlaceOnMap: (Offset) -> Unit = {}
) {
    var csvText by remember { mutableStateOf("") }
    var maxDepth by remember { mutableStateOf(8f) }
    var compressForDisplay by remember { mutableStateOf(true) }
    var training by remember { mutableStateOf<TrainingSet?>(null) }
    var tree by remember { mutableStateOf<DecisionTreeNode?>(null) }
    var treeText by remember { mutableStateOf("") }
    var nodeCount by remember { mutableStateOf(0) }
    var parseError by remember { mutableStateOf<String?>(null) }
    var recommendedRaw by remember { mutableStateOf<String?>(null) }
    var pathStepsHuman by remember { mutableStateOf<List<String>>(emptyList()) }
    var pathNote by remember { mutableStateOf<String?>(null) }
    var featureChoices by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }
    var selectedFeatures by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var trainingSectionExpanded by remember { mutableStateOf(false) }
    var showTechnicalTree by remember { mutableStateOf(false) }
    var treeDiagramCollapsed by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()

    fun applyBuiltState(ts: TrainingSet, root: DecisionTreeNode, formatted: String, nodes: Int) {
        training = ts
        tree = root
        treeText = formatted
        nodeCount = nodes
        parseError = null
        val choices = ts.featureNames.associateWith { fname ->
            ts.rows.mapNotNull { it[fname] }.distinct().sorted()
        }
        featureChoices = choices
        selectedFeatures = choices.mapValues { (_, vals) -> vals.firstOrNull() ?: "" }
        recommendedRaw = null
        pathStepsHuman = emptyList()
        pathNote = null
    }

    fun rebuildFromCurrentCsv() {
        scope.launch {
            parseError = null
            val source = csvText.trim().ifBlank { BUILTIN_TRAINING_CSV }
            val result = withContext(Dispatchers.Default) { CsvTrainingParser.parse(source) }
            result.fold(
                onSuccess = { ts ->
                    val built = withContext(Dispatchers.Default) {
                        Id3DecisionTree.build(ts, maxDepth = maxDepth.toInt().coerceAtLeast(1))
                    }
                    val displayTree = if (compressForDisplay) {
                        Id3DecisionTree.compressRedundant(built)
                    } else {
                        built
                    }
                    val text = withContext(Dispatchers.Default) { Id3DecisionTree.formatTree(displayTree) }
                    val nodes = Id3DecisionTree.countNodes(displayTree)
                    applyBuiltState(ts, displayTree, text, nodes)
                },
                onFailure = { e ->
                    parseError = e.message ?: "Не удалось разобрать таблицу"
                    tree = null
                    treeText = ""
                    nodeCount = 0
                    featureChoices = emptyMap()
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        parseError = null
        val result = withContext(Dispatchers.Default) { CsvTrainingParser.parse(BUILTIN_TRAINING_CSV) }
        result.fold(
            onSuccess = { ts ->
                val built = withContext(Dispatchers.Default) {
                    Id3DecisionTree.build(ts, maxDepth = maxDepth.toInt().coerceAtLeast(1))
                }
                val displayTree = if (compressForDisplay) {
                    Id3DecisionTree.compressRedundant(built)
                } else {
                    built
                }
                val text = withContext(Dispatchers.Default) { Id3DecisionTree.formatTree(displayTree) }
                val nodes = Id3DecisionTree.countNodes(displayTree)
                applyBuiltState(ts, displayTree, text, nodes)
            },
            onFailure = { e ->
                parseError = e.message ?: "Ошибка загрузки"
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = TGU_Blue.copy(alpha = 0.06f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Обед: подсказка по выборке",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TGU_Blue
                )
                Text(
                    "Приложение построило дерево решений по примерам и ведёт вас по веткам, как по дорожке: сначала смотрите схему, затем ответьте про себя — в конце покажем заведение и путь по узлам.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = TGU_Gold, modifier = Modifier.size(20.dp))
                    Text(
                        "Вкладка «Карта» внизу — маршрут по кампусу. Здесь — только выбор места для еды.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StepHeader(
                    number = 1,
                    title = "Схема дерева решений",
                    subtitle = "Так модель различает ситуации по вашим примерам"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { treeDiagramCollapsed = !treeDiagramCollapsed }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (treeDiagramCollapsed) "Показать схему" else "Свернуть схему",
                        style = MaterialTheme.typography.labelLarge,
                        color = TGU_Blue
                    )
                    Icon(
                        if (treeDiagramCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                        contentDescription = null,
                        tint = TGU_Blue
                    )
                }
                AnimatedVisibility(visible = !treeDiagramCollapsed) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        when {
                            parseError != null && tree == null -> {
                                Text(parseError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                            }
                            tree == null -> {
                                Text("Строим дерево…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            else -> {
                                tree?.let { root ->
                                    DecisionTreeVisual(node = root)
                                    training?.let { ts ->
                                        Text(
                                            "${ts.rows.size} примеров в выборке · в схеме $nodeCount узлов",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StepHeader(
                    number = 2,
                    title = "Ваш случай",
                    subtitle = "Отметьте варианты — так мы пройдём по дереву до листа"
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TouchApp, contentDescription = null, tint = TGU_Blue, modifier = Modifier.size(22.dp))
                    Text(
                        "Новые данные для обеда",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (featureChoices.isEmpty()) {
                    Text(
                        if (parseError != null) parseError!! else "Загружаем вопросы…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (parseError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    featureChoices.forEach { (name, values) ->
                        key(name) {
                            Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                Text(
                                    uiFeatureTitle(name),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                values.forEach { option ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(MaterialTheme.shapes.small)
                                            .clickable {
                                                selectedFeatures = selectedFeatures + (name to option)
                                            }
                                            .padding(vertical = 4.dp, horizontal = 2.dp)
                                    ) {
                                        RadioButton(
                                            selected = selectedFeatures[name] == option,
                                            onClick = { selectedFeatures = selectedFeatures + (name to option) },
                                            colors = RadioButtonDefaults.colors(selectedColor = TGU_Blue)
                                        )
                                        Text(
                                            uiValueLabel(name, option),
                                            style = MaterialTheme.typography.bodyLarge,
                                            modifier = Modifier.padding(start = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Button(
            onClick = {
                val root = tree ?: return@Button
                val result = Id3DecisionTree.predict(selectedFeatures, root)
                recommendedRaw = result.label
                pathStepsHuman = result.pathSteps.map { uiHumanizePathStep(it) }
                pathNote = result.unresolvedReason?.let { reason ->
                    when {
                        reason.contains("Не задан признак") ->
                            "Не хватает ответа по одному из пунктов — показан запасной вариант."
                        reason.contains("не встречалось") ->
                            "Такая комбинация не встречалась в примерах — показан ближайший подходящий вариант."
                        else -> reason
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            enabled = tree != null && featureChoices.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(containerColor = TGU_Blue),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.size(8.dp))
            Text("Получить рекомендацию и путь по дереву", style = MaterialTheme.typography.titleSmall)
        }

        recommendedRaw?.let { raw ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = TGU_Gold.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(Modifier.padding(18.dp)) {
                    StepHeader(
                        number = 3,
                        title = "Результат",
                        subtitle = "Заведение и маршрут по узлам дерева"
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Рекомендуемое место",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        uiPlaceTitle(raw),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TGU_Blue,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            val venueForMap = foodVenueForRecommendedPlace(raw)
            if (venueForMap != null) {
                OutlinedButton(
                    onClick = { onOpenPlaceOnMap(venueForMap.mapPosition) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TGU_Blue),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Открыть на карте кампуса", style = MaterialTheme.typography.titleSmall)
                }
            } else {
                Text(
                    "Для этой метки пока нет точки на карте приложения.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (pathStepsHuman.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Psychology, contentDescription = null, tint = TGU_Blue, modifier = Modifier.size(22.dp))
                            Text(
                                "Путь по узлам дерева",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TGU_Blue
                            )
                        }
                        Text(
                            "Каждый шаг — это переход по ветке после ответа на вопрос в узле.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        pathStepsHuman.forEachIndexed { index, line ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(TGU_Blue.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "${index + 1}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TGU_Blue
                                    )
                                }
                                Text(
                                    line,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        pathNote?.let { note ->
                            Divider()
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.HelpOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    note,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { trainingSectionExpanded = !trainingSectionExpanded }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = TGU_Blue)
                Column {
                    Text(
                        "Своя выборка и бонус: компактное дерево",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = TGU_Blue
                    )
                    Text(
                        "Для задания: CSV, глубина, упрощение",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                if (trainingSectionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = TGU_Blue
            )
        }

        AnimatedVisibility(
            visible = trainingSectionExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F5F9)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Подсказка уже работает. Вставьте свою таблицу CSV, если нужно для отчёта: первая строка — названия столбцов, последний столбец — итоговое место.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = csvText,
                        onValueChange = { csvText = it },
                        modifier = Modifier.fillMaxWidth().height(140.dp),
                        textStyle = MaterialTheme.typography.bodySmall,
                        minLines = 5,
                        label = { Text("Своя таблица CSV") },
                        placeholder = {
                            Text(
                                "Необязательно — оставьте пустым",
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                    Text(
                        "Бонус: удобнее смотреть на телефоне",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TGU_Blue
                    )
                    Text(
                        "Ограничьте глубину и включите упрощение — дерево станет меньше и аккуратнее на экране.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Глубина", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(72.dp))
                        Slider(
                            value = maxDepth,
                            onValueChange = { maxDepth = it },
                            valueRange = 1f..12f,
                            steps = 10,
                            modifier = Modifier.weight(1f)
                        )
                        Text("${maxDepth.toInt()}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = compressForDisplay, onCheckedChange = { compressForDisplay = it })
                        Text(
                            "Упростить дерево (склеить лишнее для экрана)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Button(
                        onClick = { rebuildFromCurrentCsv() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TGU_Blue)
                    ) {
                        Text("Применить и пересчитать дерево")
                    }
                    parseError?.let { err ->
                        Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    training?.let { ts ->
                        Text(
                            "Строк в данных: ${ts.rows.size} · узлов в схеме: $nodeCount",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTechnicalTree = !showTechnicalTree }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Текстовая схема (для отчёта)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            if (showTechnicalTree) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    AnimatedVisibility(visible = showTechnicalTree) {
                        Text(
                            text = treeText.ifBlank { "—" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
