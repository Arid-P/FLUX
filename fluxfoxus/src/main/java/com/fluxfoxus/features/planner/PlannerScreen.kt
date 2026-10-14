package com.fluxfoxus.features.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fluxfoxus.core.database.entity.FocusSessionEntity
import com.fluxfoxus.core.database.entity.PresetEntity
import com.fluxfoxus.core.designsystem.component.RusticMedleyHeader
import com.fluxfoxus.core.designsystem.theme.FluxTheme
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    viewModel: PlannerViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        containerColor = FluxTheme.colors.background,
        topBar = {
            RusticMedleyHeader(
                title = "PLANNER"
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openScheduleSheet() },
                containerColor = FluxTheme.colors.primary,
                contentColor = FluxTheme.colors.background,
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Schedule Session")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = monthYearFormat.format(Date(uiState.selectedDateEpochMs)),
                    style = FluxTheme.typography.headlineMedium,
                    color = FluxTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )

                TextButton(onClick = { viewModel.selectDate(System.currentTimeMillis()) }) {
                    Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, tint = FluxTheme.colors.accent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Today", color = FluxTheme.colors.accent, fontWeight = FontWeight.SemiBold)
                }
            }

            // 7-Day Horizontal Strip
            WeekStrip(
                selectedDateEpochMs = uiState.selectedDateEpochMs,
                onDateSelected = { viewModel.selectDate(it) }
            )

            // Scheduled Sessions List
            if (uiState.scheduledSessions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No focus blocks scheduled for this day.\nTap '+' to schedule a session.",
                        color = FluxTheme.colors.textMuted,
                        style = FluxTheme.typography.bodyMedium,
                        lineHeight = 22.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.scheduledSessions, key = { it.id }) { session ->
                        ScheduledSessionCard(session)
                    }
                }
            }
        }

        // Schedule Session Bottom Sheet with Start & End Time Configuration
        if (uiState.isScheduleSheetOpen) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.closeScheduleSheet() },
                sheetState = sheetState,
                containerColor = FluxTheme.colors.surface
            ) {
                ScheduleBottomSheetContent(
                    selectedDateEpochMs = uiState.selectedDateEpochMs,
                    presets = uiState.availablePresets,
                    onSchedule = { presetId, title, startMs, endMs, duration ->
                        viewModel.scheduleSession(presetId, title, startMs, endMs, duration)
                    }
                )
            }
        }
    }
}

@Composable
private fun WeekStrip(
    selectedDateEpochMs: Long,
    onDateSelected: (Long) -> Unit
) {
    val days = remember {
        val list = mutableListOf<Long>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -3)
        for (i in 0 until 7) {
            list.add(cal.timeInMillis)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
    val numFormat = SimpleDateFormat("d", Locale.getDefault())

    val selectedCal = Calendar.getInstance().apply { timeInMillis = selectedDateEpochMs }
    val selectedDayOfYear = selectedCal.get(Calendar.DAY_OF_YEAR)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        days.forEach { timeMs ->
            val cal = Calendar.getInstance().apply { timeInMillis = timeMs }
            val isSelected = cal.get(Calendar.DAY_OF_YEAR) == selectedDayOfYear

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) FluxTheme.colors.primary else FluxTheme.colors.surface)
                    .border(1.dp, if (isSelected) FluxTheme.colors.primary else FluxTheme.colors.border, RoundedCornerShape(12.dp))
                    .clickable { onDateSelected(timeMs) }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = dayFormat.format(Date(timeMs)),
                    fontSize = 11.sp,
                    color = if (isSelected) FluxTheme.colors.background else FluxTheme.colors.textMuted
                )
                Text(
                    text = numFormat.format(Date(timeMs)),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) FluxTheme.colors.background else FluxTheme.colors.textPrimary
                )
            }
        }
    }
}

@Composable
private fun ScheduledSessionCard(session: FocusSessionEntity) {
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val startStr = timeFormat.format(Date(session.startTime))
    val endStr = if (session.endTime != null) timeFormat.format(Date(session.endTime)) else ""

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = FluxTheme.colors.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, FluxTheme.colors.border, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = session.sessionTitle,
                        style = FluxTheme.typography.titleMedium,
                        color = FluxTheme.colors.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Origin Badge: [FF] or [FD]
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (session.sessionOrigin == "TASK_SYNC") FluxTheme.colors.accent.copy(alpha = 0.2f) else FluxTheme.colors.primary.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (session.sessionOrigin == "TASK_SYNC") "FD" else "FF",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (session.sessionOrigin == "TASK_SYNC") FluxTheme.colors.accent else FluxTheme.colors.primary
                        )
                    }
                }

                Text(
                    text = "$startStr - $endStr (${session.targetDurationMinutes}m)",
                    color = FluxTheme.colors.textMuted,
                    fontSize = 12.sp
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(FluxTheme.colors.background)
                    .border(1.dp, FluxTheme.colors.border, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = session.status,
                    color = if (session.status == "COMPLETED") FluxTheme.colors.accent else FluxTheme.colors.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ScheduleBottomSheetContent(
    selectedDateEpochMs: Long,
    presets: List<PresetEntity>,
    onSchedule: (presetId: String, title: String, startTimeEpochMs: Long, endTimeEpochMs: Long, durationMinutes: Int) -> Unit
) {
    var selectedPresetId by remember { mutableStateOf(presets.firstOrNull()?.id ?: "default") }
    val selectedPreset = presets.find { it.id == selectedPresetId }
    var title by remember(selectedPreset) { mutableStateOf(selectedPreset?.name ?: "Focus Session") }
    var durationMinutes by remember { mutableIntStateOf(45) }

    // Start time configuration: calculate next clean 15-minute slot
    val initialCal = remember(selectedDateEpochMs) {
        Calendar.getInstance().apply {
            val nowCal = Calendar.getInstance()
            timeInMillis = selectedDateEpochMs
            set(Calendar.HOUR_OF_DAY, nowCal.get(Calendar.HOUR_OF_DAY))
            val currentMin = nowCal.get(Calendar.MINUTE)
            val roundedMin = ((currentMin / 15) + 1) * 15
            if (roundedMin >= 60) {
                add(Calendar.HOUR_OF_DAY, 1)
                set(Calendar.MINUTE, 0)
            } else {
                set(Calendar.MINUTE, roundedMin)
            }
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    var startHour24 by remember { mutableIntStateOf(initialCal.get(Calendar.HOUR_OF_DAY)) }
    var startMinute by remember { mutableIntStateOf(initialCal.get(Calendar.MINUTE)) }

    // Compute exact start and finish epoch milliseconds
    val computedStartEpochMs = remember(selectedDateEpochMs, startHour24, startMinute) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = selectedDateEpochMs
            set(Calendar.HOUR_OF_DAY, startHour24)
            set(Calendar.MINUTE, startMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        cal.timeInMillis
    }

    val computedEndEpochMs = remember(computedStartEpochMs, durationMinutes) {
        computedStartEpochMs + durationMinutes * 60 * 1000L
    }

    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val startStr = timeFormat.format(Date(computedStartEpochMs))
    val endStr = timeFormat.format(Date(computedEndEpochMs))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Schedule Focus Block",
            style = FluxTheme.typography.titleMedium,
            color = FluxTheme.colors.textPrimary,
            fontWeight = FontWeight.Bold
        )

        // Preset Selector Chips
        Text(text = "Choose Preset:", color = FluxTheme.colors.textMuted, fontSize = 12.sp)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(presets, key = { it.id }) { preset ->
                val isSelected = preset.id == selectedPresetId
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) FluxTheme.colors.accent else FluxTheme.colors.surface)
                        .border(1.dp, if (isSelected) FluxTheme.colors.accent else FluxTheme.colors.border, RoundedCornerShape(8.dp))
                        .clickable {
                            selectedPresetId = preset.id
                            title = preset.name
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "${preset.emoji} ${preset.name}",
                        color = if (isSelected) FluxTheme.colors.background else FluxTheme.colors.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Title Field
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Session Title") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = FluxTheme.colors.primary,
                unfocusedBorderColor = FluxTheme.colors.border,
                focusedTextColor = FluxTheme.colors.textPrimary,
                unfocusedTextColor = FluxTheme.colors.textPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Start Time Selector
        Text(text = "Start Time:", color = FluxTheme.colors.textMuted, fontSize = 12.sp)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = FluxTheme.colors.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, FluxTheme.colors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hour Adjuster
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = {
                        startHour24 = (startHour24 - 1 + 24) % 24
                    }) {
                        Text("-", style = FluxTheme.typography.titleMedium, color = FluxTheme.colors.primary)
                    }
                    val displayHour12 = when (val h = startHour24 % 12) {
                        0 -> 12
                        else -> h
                    }
                    Text(
                        text = displayHour12.toString().padStart(2, '0'),
                        style = FluxTheme.typography.titleMedium,
                        color = FluxTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = {
                        startHour24 = (startHour24 + 1) % 24
                    }) {
                        Text("+", style = FluxTheme.typography.titleMedium, color = FluxTheme.colors.primary)
                    }
                }

                Text(
                    text = ":",
                    style = FluxTheme.typography.titleMedium,
                    color = FluxTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )

                // Minute Adjuster (15 min steps)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = {
                        startMinute = (startMinute - 15 + 60) % 60
                    }) {
                        Text("-", style = FluxTheme.typography.titleMedium, color = FluxTheme.colors.primary)
                    }
                    Text(
                        text = startMinute.toString().padStart(2, '0'),
                        style = FluxTheme.typography.titleMedium,
                        color = FluxTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = {
                        startMinute = (startMinute + 15) % 60
                    }) {
                        Text("+", style = FluxTheme.typography.titleMedium, color = FluxTheme.colors.primary)
                    }
                }

                // AM / PM Toggle
                val isPm = startHour24 >= 12
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = FluxTheme.colors.primary.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, FluxTheme.colors.primary),
                    modifier = Modifier.clickable {
                        startHour24 = if (isPm) startHour24 - 12 else startHour24 + 12
                    }
                ) {
                    Text(
                        text = if (isPm) "PM" else "AM",
                        color = FluxTheme.colors.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Duration Chips
        Text(text = "Target Duration:", color = FluxTheme.colors.textMuted, fontSize = 12.sp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(25, 45, 60, 90).forEach { mins ->
                val isSelected = durationMinutes == mins
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) FluxTheme.colors.primary else FluxTheme.colors.surface)
                        .border(1.dp, if (isSelected) FluxTheme.colors.primary else FluxTheme.colors.border, RoundedCornerShape(8.dp))
                        .clickable { durationMinutes = mins },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${mins}m",
                        color = if (isSelected) FluxTheme.colors.background else FluxTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Dynamically Computed Finish Time Readout Banner
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = FluxTheme.colors.primary.copy(alpha = 0.12f),
            border = androidx.compose.foundation.BorderStroke(1.dp, FluxTheme.colors.primary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Starts at $startStr • Finishes at $endStr",
                    style = FluxTheme.typography.bodyMedium.copy(
                        color = FluxTheme.colors.primary,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Total Focus Target: $durationMinutes minutes",
                    style = FluxTheme.typography.bodySmall.copy(color = FluxTheme.colors.textMuted)
                )
            }
        }

        // Confirm Schedule Button
        Button(
            onClick = {
                onSchedule(selectedPresetId, title, computedStartEpochMs, computedEndEpochMs, durationMinutes)
            },
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = FluxTheme.colors.primary,
                contentColor = FluxTheme.colors.background
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(text = "Confirm Schedule", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
