package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Allocation
import com.example.data.DailyActivity
import com.example.data.TimeInsight

// Color palette
val SlateBackground = Color(0xFF0F172A)
val CardDark = Color(0xFF1E293B)
val TextLight = Color(0xFFF8FAFC)
val TextGray = Color(0xFF94A3B8)
val NeonMint = Color(0xFF34D399)
val NeonCoral = Color(0xFFFB7185)
val BrightBlue = Color(0xFF38BDF8)
val WarmGold = Color(0xFFFBBF24)

// Global list of weekdays
val weekDaysList = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(
    viewModel: AllocationsViewModel,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf("routines") } // "routines" or "allocations"
    var showAddDialog by remember { mutableStateOf(false) }

    val allActivities by viewModel.allActivities.collectAsStateWithLifecycle()
    val allAllocations by viewModel.allAllocations.collectAsStateWithLifecycle()
    val currentDayActivities by viewModel.currentDayActivities.collectAsStateWithLifecycle()
    val latestInsight by viewModel.latestInsight.collectAsStateWithLifecycle()
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()
    val selectedDay by viewModel.selectedDay.collectAsStateWithLifecycle()

    // Keep track of which days have any activities logged
    val calendarActivityMap = remember(allActivities) {
        allActivities.groupBy { it.dayOfWeek }.mapValues { it.value.isNotEmpty() }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(SlateBackground),
        containerColor = SlateBackground,
        bottomBar = {
            // Elegant navigation bar conforming to our frontend design instructions
            NavigationBar(
                containerColor = CardDark,
                tonalElevation = 8.dp,
                modifier = Modifier.navigationBarsPadding()
            ) {
                NavigationBarItem(
                    selected = activeTab == "routines",
                    onClick = { activeTab = "routines" },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Daily Routing Log") },
                    label = { Text("Daily Logs") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonMint,
                        unselectedIconColor = TextGray,
                        selectedTextColor = NeonMint,
                        unselectedTextColor = TextGray,
                        indicatorColor = SlateBackground
                    )
                )
                NavigationBarItem(
                    selected = activeTab == "allocations",
                    onClick = { activeTab = "allocations" },
                    icon = { Icon(Icons.Default.Home, contentDescription = "AI Allocator & Study Scheduler") },
                    label = { Text("AI Allocation") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonMint,
                        unselectedIconColor = TextGray,
                        selectedTextColor = NeonMint,
                        unselectedTextColor = TextGray,
                        indicatorColor = SlateBackground
                    )
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
        ) {
            // Header panel with metallic feeling design
            HeaderPanel(
                daysTrackedCount = calendarActivityMap.filter { it.value }.size,
                reclaimedHoursStr = latestInsight?.reclaimedWeeklyHours ?: "0.0 Hrs"
            )

            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    slideInHorizontally { width -> if (targetState == "allocations") width else -width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> if (targetState == "allocations") -width else width } + fadeOut()
                },
                label = "TabTransition"
            ) { tab ->
                when (tab) {
                    "routines" -> {
                        RoutineLogTab(
                            selectedDay = selectedDay,
                            onDaySelected = { viewModel.selectedDay.value = it },
                            calendarActivityMap = calendarActivityMap,
                            currentActivities = currentDayActivities,
                            onAddActivityClick = { showAddDialog = true },
                            onDeleteActivity = { viewModel.deleteActivity(it) },
                            onLoadDemoClick = { viewModel.loadSample7DayRoutine() },
                            onResetClick = { viewModel.resetAllData() }
                        )
                    }
                    "allocations" -> {
                        AIAllocationTab(
                            viewModel = viewModel,
                            aiState = aiState,
                            allAllocations = allAllocations,
                            latestInsight = latestInsight,
                            onToggleComplete = { id, finished -> viewModel.toggleAllocationComplete(id, finished) },
                            hasLogs = allActivities.isNotEmpty()
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            AddActivityDialog(
                currentDay = selectedDay,
                onDismiss = { showAddDialog = false },
                onSave = { name, start, end, cat ->
                    viewModel.addActivity(selectedDay, name, start, end, cat)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun HeaderPanel(
    daysTrackedCount: Int,
    reclaimedHoursStr: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    // Modern gradient hairline stroke
                    val borderGradient = Brush.linearGradient(
                        colors = listOf(NeonMint, BrightBlue),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f)
                    )
                    drawLine(
                        brush = borderGradient,
                        start = Offset(0f, size.height - 2f),
                        end = Offset(size.width, size.height - 2f),
                        strokeWidth = 4f
                    )
                }
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CHRONOS TIME AI",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonMint,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Routine Optimizer",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextLight,
                        fontFamily = FontFamily.SansSerif
                    )
                }
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "AI Allocator Status Indicator",
                    tint = BrightBlue,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Tracking Status Gauge
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(SlateBackground.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Text("Routine Tracking", fontSize = 10.sp, color = TextGray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "$daysTrackedCount/7 Days",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (daysTrackedCount >= 7) NeonMint else WarmGold
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (daysTrackedCount >= 7) NeonMint else WarmGold)
                        )
                    }
                }

                // Hours Reverted to Growth
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(SlateBackground.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Text("Reclaimed Time Plans", fontSize = 10.sp, color = TextGray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = reclaimedHoursStr,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrightBlue
                        )
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Optimized successfully",
                            tint = NeonMint,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RoutineLogTab(
    selectedDay: String,
    onDaySelected: (String) -> Unit,
    calendarActivityMap: Map<String, Boolean>,
    currentActivities: List<DailyActivity>,
    onAddActivityClick: () -> Unit,
    onDeleteActivity: (DailyActivity) -> Unit,
    onLoadDemoClick: () -> Unit,
    onResetClick: () -> Unit
) {
    val weekDays = weekDaysList

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Week days pill list
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            weekDays.forEach { day ->
                val isSelected = day == selectedDay
                val hasLog = calendarActivityMap[day] == true
                val shortName = day.take(3)

                Box(
                    modifier = Modifier
                        .clickable { onDaySelected(day) }
                        .testTag("day_button_$shortName")
                        .background(
                            color = if (isSelected) NeonMint else CardDark,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) Color.Transparent else TextGray.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = shortName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) SlateBackground else TextLight
                        )
                        if (hasLog) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) SlateBackground else NeonMint)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$selectedDay's Profile",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextLight
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (currentActivities.isNotEmpty() || calendarActivityMap.values.any { it }) {
                    TextButton(
                        onClick = onResetClick,
                        modifier = Modifier.testTag("clear_data_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear all activities",
                            tint = NeonCoral,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear All", color = NeonCoral, fontSize = 12.sp)
                    }
                }
                FilledTonalButton(
                    onClick = onAddActivityClick,
                    modifier = Modifier.testTag("add_activity_fab"),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = NeonMint,
                        contentColor = SlateBackground
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add activity", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Log", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (currentActivities.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(CardDark, RoundedCornerShape(16.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Empty habits state",
                        tint = TextGray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No activities logged for $selectedDay",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextLight
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Add what you do today, or load a dummy 7-Day week log instantly to let the AI analyze and budget free times right away!",
                        fontSize = 12.sp,
                        color = TextGray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onLoadDemoClick,
                        modifier = Modifier.testTag("demo_load_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = BrightBlue, contentColor = SlateBackground)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Simulate tracker")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Load 7-Day Demo Routine", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // List of items
                items(currentActivities, key = { it.id }) { activity ->
                    ActivityItemRow(
                        activity = activity,
                        onDelete = { onDeleteActivity(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun ActivityItemRow(
    activity: DailyActivity,
    onDelete: (DailyActivity) -> Unit
) {
    val categoryColor = when (activity.category) {
        "Sleep" -> BlueIndicator
        "Work/Study" -> CoralIndicator
        "Leisure" -> MintIndicator
        "Chores" -> GoldIndicator
        else -> OrangeIndicator
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(categoryColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.name,
                    color = TextLight,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${activity.startTime} - ${activity.endTime}",
                        color = TextGray,
                        fontSize = 12.sp
                    )
                    Box(
                        modifier = Modifier
                            .background(categoryColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = activity.category,
                            color = categoryColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            IconButton(
                onClick = { onDelete(activity) },
                modifier = Modifier.testTag("delete_btn_${activity.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete entry",
                    tint = TextGray.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// Indicator Colors
val BlueIndicator = Color(0xFF60A5FA)
val CoralIndicator = Color(0xFFF87171)
val MintIndicator = Color(0xFF34D399)
val GoldIndicator = Color(0xFFFBBF24)
val OrangeIndicator = Color(0xFFFB923C)

@Composable
fun AIAllocationTab(
    viewModel: AllocationsViewModel,
    aiState: AIState,
    allAllocations: List<Allocation>,
    latestInsight: TimeInsight?,
    onToggleComplete: (Int, Boolean) -> Unit,
    hasLogs: Boolean
) {
    val studyTopic by viewModel.studyTopic.collectAsStateWithLifecycle()
    val bookTopic by viewModel.bookTopic.collectAsStateWithLifecycle()

    var isEditingFocus by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        if (!hasLogs) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardDark, RoundedCornerShape(16.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Locked AI Scheduler",
                        tint = WarmGold,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "AI Allocator is Locked",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextLight
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Please log weekly activities in the Daily Logs tab first, or load the 7-day demo routine to immediately test the planning flow!",
                        fontSize = 12.sp,
                        color = TextGray
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            return
        }

        // Custom AI Targets Block
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, TextGray.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Target settings", tint = BrightBlue)
                        Text(
                            text = "My Personal Learning Focus",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextLight
                        )
                    }

                    TextButton(onClick = { isEditingFocus = !isEditingFocus }) {
                        Text(if (isEditingFocus) "Done" else "Configure", color = NeonMint, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (isEditingFocus) {
                    OutlinedTextField(
                        value = studyTopic,
                        onValueChange = { viewModel.studyTopic.value = it },
                        label = { Text("Studying/Skills Topic", color = TextGray) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("study_topic_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonMint,
                            unfocusedBorderColor = TextGray,
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = bookTopic,
                        onValueChange = { viewModel.bookTopic.value = it },
                        label = { Text("Reading/Books Genre", color = TextGray) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("book_topic_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonMint,
                            unfocusedBorderColor = TextGray,
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight
                        )
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("STUDY STUDY TOPIC", fontSize = 9.sp, color = NeonCoral, fontWeight = FontWeight.Bold)
                            Text(
                                studyTopic,
                                color = TextLight,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("BOOK READING GOAL", fontSize = 9.sp, color = NeonMint, fontWeight = FontWeight.Bold)
                            Text(
                                bookTopic,
                                color = TextLight,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        
        // AI Device Sync and Alert Tools
        AIDeviceSyncAndAlertCenter(viewModel = viewModel)

        Spacer(modifier = Modifier.height(12.dp))

        // AI Allocation trigger button
        Button(
            onClick = { viewModel.generateAllocations() },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("optimize_schedule_btn"),
            colors = ButtonDefaults.buttonColors(containerColor = NeonMint, contentColor = SlateBackground),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Star, contentDescription = "Generate optimization")
                Text("RECLAIM & OPTIMIZE FREE TIME", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // State Machine Renderer
        when (aiState) {
            is AIState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardDark, RoundedCornerShape(12.dp))
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = NeonMint, strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Chronos AI is learning your routine...",
                            color = TextLight,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Analyzing 7 days of logs to locate idle slots & allocate study blocks",
                            color = TextGray,
                            fontSize = 10.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            is AIState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NeonCoral.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .border(1.dp, NeonCoral, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text("AI Scheduling Alert", color = NeonCoral, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text((aiState as AIState.Error).message, color = TextLight, fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            else -> {}
        }

        // Show result / Latest Insight
        if (latestInsight != null) {
            // Elegant insight bubble
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardDark.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BrightBlue.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(BrightBlue.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Info, contentDescription = "Brain icon", tint = BrightBlue, modifier = Modifier.size(18.dp))
                        }
                        Text(
                            text = "AI WEEKLY ANALYSIS & LEARNING",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrightBlue,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = latestInsight.analysisText,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = TextLight,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .background(NeonMint.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                            .fillMaxWidth()
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Check, "Checked icon", tint = NeonMint, modifier = Modifier.size(16.dp))
                            Text(
                                "Result: Reclaimed ${latestInsight.reclaimedWeeklyHours} of leisure and dedicated it to studying '$studyTopic' and reading '$bookTopic'.",
                                fontSize = 11.sp,
                                color = NeonMint,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        if (allAllocations.isNotEmpty()) {
            Text(
                text = "Your AI Allocated Schedule",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextLight
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Timetable rendering using clean, composable-safe standard loops
            for (day in weekDaysList) {
                val dayAllocations = allAllocations.filter { it.dayOfWeek == day }
                if (dayAllocations.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = day,
                            color = NeonMint,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(all = 4.dp)
                        )

                        for (model in dayAllocations) {
                            AllocationItem(allocation = model, onToggle = { onToggleComplete(model.id, it) })
                        }
                    }
                }
            }
        } else if (latestInsight == null && aiState !is AIState.Loading) {
            // Friendly onboarding empty state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Click optimization above to analyze logs and build your schedule!",
                    color = TextGray,
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun AllocationItem(
    allocation: Allocation,
    onToggle: (Boolean) -> Unit
) {
    val isStudy = allocation.type == "STUDY"
    val colorAccent = if (isStudy) NeonCoral else NeonMint

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(colorAccent.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isStudy) Icons.Default.Info else Icons.Default.Star,
                    contentDescription = "Allocation Indicator icon",
                    tint = colorAccent,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = allocation.activityName,
                    color = if (allocation.isCompleted) TextGray else TextLight,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (allocation.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "${allocation.startTime} - ${allocation.endTime}",
                        color = TextGray,
                        fontSize = 12.sp,
                        textDecoration = if (allocation.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                    Box(
                        modifier = Modifier
                            .background(colorAccent.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = allocation.type,
                            color = colorAccent,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Checkbox(
                checked = allocation.isCompleted,
                onCheckedChange = { onToggle(it) },
                modifier = Modifier.testTag("check_allocation_${allocation.id}"),
                colors = CheckboxDefaults.colors(
                    checkedColor = NeonMint,
                    uncheckedColor = TextGray,
                    checkmarkColor = SlateBackground
                )
            )
        }
    }
}

@Composable
fun AddActivityDialog(
    currentDay: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("19:00") }
    var endTime by remember { mutableStateOf("21:00") }
    var category by remember { mutableStateOf("Leisure") } // "Sleep", "Work/Study", "Leisure", "Chores", "Other"

    val categories = listOf("Sleep", "Work/Study", "Leisure", "Chores", "Other")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Add Log for $currentDay",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextLight
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("What did you do?", color = TextGray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonMint,
                        unfocusedBorderColor = TextGray,
                        focusedTextColor = TextLight,
                        unfocusedTextColor = TextLight
                    )
                )

                // Presets chip suggestions for speed! No static mock slop, just clean utility in line with M3 spacing.
                Column {
                    Text("Or select quick preset:", fontSize = 10.sp, color = TextGray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Triple("Sleep", "Sleep", "23:00 to 07:00"),
                            Triple("Job", "Work/Study", "09:00 to 17:00"),
                            Triple("Instagram", "Leisure", "19:00 to 21:00"),
                            Triple("Gaming", "Leisure", "18:00 to 20:00"),
                            Triple("Chores", "Chores", "17:30 to 18:30")
                        ).forEach { (presName, presCat, presDur) ->
                            Box(
                                modifier = Modifier
                                    .background(BrightBlue.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .border(1.dp, BrightBlue.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .clickable {
                                        name = presName
                                        category = presCat
                                        val parts = presDur.split(" to ")
                                        startTime = parts[0]
                                        endTime = parts[1]
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(presName, fontSize = 11.sp, color = BrightBlue, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start (HH:mm)", color = TextGray) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_start_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonMint,
                            unfocusedBorderColor = TextGray,
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight
                        )
                    )

                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End (HH:mm)", color = TextGray) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_end_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonMint,
                            unfocusedBorderColor = TextGray,
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight
                        )
                    )
                }

                // Category chooser
                Column {
                    Text("Category", fontSize = 12.sp, color = TextGray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            val isSelected = cat == category
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isSelected) NeonMint else SlateBackground,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { category = cat }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    cat,
                                    color = if (isSelected) SlateBackground else TextLight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextGray)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = { if (name.isNotBlank()) onSave(name, startTime, endTime, category) },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonMint, contentColor = SlateBackground),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Add Entry", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AIDeviceSyncAndAlertCenter(
    viewModel: AllocationsViewModel
) {
    val context = LocalContext.current
    val studyTopic by viewModel.studyTopic.collectAsStateWithLifecycle()
    val bookTopic by viewModel.bookTopic.collectAsStateWithLifecycle()
    
    var continuousSyncEnabled by remember { mutableStateOf(false) }
    var alertLogs by remember { mutableStateOf(listOf<String>()) }
    var permissionGranted by remember { mutableStateOf(NotificationHelper.hasNotificationPermission(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionGranted = isGranted
        if (isGranted && continuousSyncEnabled) {
            NotificationHelper.showContinuousSyncNotification(context, studyTopic, bookTopic)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, BrightBlue.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notification and alerts icon",
                        tint = WarmGold
                    )
                    Text(
                        text = "AI System Link & Alert Center",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextLight
                    )
                }
                
                Box(
                    modifier = Modifier
                        .background(if (continuousSyncEnabled && permissionGranted) NeonMint.copy(alpha = 0.15f) else TextGray.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (continuousSyncEnabled && permissionGranted) "TELEMETRY: BOUND" else "TELEMETRY: IDLE",
                        color = if (continuousSyncEnabled && permissionGranted) NeonMint else TextGray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Three Telemetry Gauges / Diagnostic Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pipe 1
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(SlateBackground, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Column {
                        Text("CONTEXT PIPELINE", fontSize = 8.sp, color = TextGray, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(6.dp).background(NeonMint, CircleShape))
                            Text("OPTIMIZED", fontSize = 11.sp, color = NeonMint, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Pipe 2
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(SlateBackground, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Column {
                        Text("PERSISTENT TRACKER", fontSize = 8.sp, color = TextGray, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(6.dp).background(if (continuousSyncEnabled && permissionGranted) NeonMint else TextGray, CircleShape))
                            Text(if (continuousSyncEnabled && permissionGranted) "RUNNING" else "PAUSED", fontSize = 11.sp, color = if (continuousSyncEnabled && permissionGranted) NeonMint else TextGray, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Pipe 3
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(SlateBackground, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Column {
                        Text("ALERT CHANNEL", fontSize = 8.sp, color = TextGray, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(6.dp).background(if (permissionGranted) NeonMint else NeonCoral, CircleShape))
                            Text(if (permissionGranted) "ENABLED" else "MUTED", fontSize = 11.sp, color = if (permissionGranted) NeonMint else NeonCoral, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Explanation Section: Why Continuous Notification is critical
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BrightBlue.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                    .border(BorderStroke(1.dp, BrightBlue.copy(alpha = 0.2f)), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "AI Context alignment warning",
                        tint = BrightBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Column {
                        Text(
                            text = "Device & AI Reliability Mandate",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextLight
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "To ensure the device works effectively with AI, continuous notifications are of absolute importance. Background study routines get aggressively killed by power conservation services unless anchored by an active notification stream. Keeping syncing enabled guarantees prompt context alignment.",
                            fontSize = 10.5.sp,
                            color = TextGray,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Continuous Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SlateBackground, RoundedCornerShape(10.dp))
                    .clickable {
                        val before = continuousSyncEnabled
                        if (!before) {
                            if (!permissionGranted && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch("android.permission.POST_NOTIFICATIONS")
                            } else {
                                continuousSyncEnabled = true
                                NotificationHelper.showContinuousSyncNotification(context, studyTopic, bookTopic)
                            }
                        } else {
                            continuousSyncEnabled = false
                            NotificationHelper.cancelContinuousSyncNotification(context)
                        }
                    }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Enable Continuous AI Sync Tracker",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextLight
                    )
                    Text(
                        text = "Pins live status tracker to status bar for continuous pipeline feed",
                        fontSize = 9.5.sp,
                        color = TextGray
                    )
                }
                
                Switch(
                    checked = continuousSyncEnabled && permissionGranted,
                    onCheckedChange = { isChecked ->
                        if (isChecked) {
                            if (!permissionGranted && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch("android.permission.POST_NOTIFICATIONS")
                            } else {
                                continuousSyncEnabled = true
                                NotificationHelper.showContinuousSyncNotification(context, studyTopic, bookTopic)
                            }
                        } else {
                            continuousSyncEnabled = false
                            NotificationHelper.cancelContinuousSyncNotification(context)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SlateBackground,
                        checkedTrackColor = NeonMint,
                        uncheckedThumbColor = TextGray,
                        uncheckedTrackColor = SlateBackground
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Alert tool section
            Divider(color = TextGray.copy(alpha = 0.15f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Chronos AI Study Alert Tool",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextLight
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Test or trigger instant alarms for studies to anchor your habit cues.",
                fontSize = 10.sp,
                color = TextGray
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Button 1: Test general alert
                Button(
                    onClick = {
                        if (!permissionGranted && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch("android.permission.POST_NOTIFICATIONS")
                        } else {
                            NotificationHelper.triggerInstantAlert(
                                context = context,
                                slotName = "Study Session: $studyTopic",
                                timeRange = "19:30 - 21:00",
                                category = "STUDY"
                            )
                            alertLogs = listOf("Triggered: Study Alert at ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}") + alertLogs.take(3)
                        }
                    },
                    modifier = Modifier.weight(1f).testTag("trigger_study_alert_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCoral),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Test alert", modifier = Modifier.size(14.dp), tint = SlateBackground)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ALERT: STUDY", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = SlateBackground)
                }

                // Button 2: Test reading alert
                Button(
                    onClick = {
                        if (!permissionGranted && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch("android.permission.POST_NOTIFICATIONS")
                        } else {
                            NotificationHelper.triggerInstantAlert(
                                context = context,
                                slotName = "Reading Session: $bookTopic",
                                timeRange = "20:00 - 21:15",
                                category = "READING"
                            )
                            alertLogs = listOf("Triggered: Reading Alert at ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}") + alertLogs.take(3)
                        }
                    },
                    modifier = Modifier.weight(1f).testTag("trigger_reading_alert_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = BrightBlue),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Test reading alert", modifier = Modifier.size(14.dp), tint = SlateBackground)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ALERT: READING", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = SlateBackground)
                }
            }

            if (alertLogs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SlateBackground.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Text("RECENT ALERT TRIGGER HISTORY", fontSize = 8.sp, color = TextGray, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    alertLogs.forEach { log ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(4.dp).background(NeonMint, CircleShape))
                            Text(log, fontSize = 9.5.sp, color = TextLight)
                        }
                    }
                }
            }
        }
    }
}
