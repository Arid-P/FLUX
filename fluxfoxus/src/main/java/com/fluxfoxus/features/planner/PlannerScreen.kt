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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fluxfoxus.core.database.entity.FocusSessionEntity
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

        // Schedule Session Bottom Sheet
        if (uiState.isScheduleSheetOpen) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.closeScheduleSheet() },
                sheetState = sheetState,
                containerColor = FluxTheme.colors.surface
            ) {
                ScheduleBottomSheetContent(
                    presets = uiState.availablePresets,
                    onSchedule = { presetId, title, duration ->
                        val startMs = System.currentTimeMillis() + 10 * 60 * 1000L
                        val endMs = startMs + duration * 60 * 1000L
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
    presets: List<com.fluxfoxus.core.database.entity.PresetEntity>,
    onSchedule: (presetId: String, title: String, durationMinutes: Int) -> Unit
) {
    var selectedPresetId by remember { mutableStateOf(presets.firstOrNull()?.id ?: "default") }
    val selectedPreset = presets.find { it.id == selectedPresetId }
    var title by remember(selectedPreset) { mutableStateOf(selectedPreset?.name ?: "Focus Session") }
    var durationMinutes by remember { mutableIntStateOf(45) }

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

        // Save Button
        Button(
            onClick = { onSchedule(selectedPresetId, title, durationMinutes) },
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
