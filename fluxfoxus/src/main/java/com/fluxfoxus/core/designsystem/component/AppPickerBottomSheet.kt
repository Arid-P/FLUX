package com.fluxfoxus.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fluxfoxus.core.designsystem.theme.HolstColors
import com.fluxfoxus.core.repository.AppInfo

/**
 * Reusable App Picker Bottom Sheet / Dialog.
 * Displays real launchable app icons, human-readable labels, and instant search filter.
 * Eliminates manual package/app name typing across Limits and Presets.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerBottomSheet(
    apps: List<AppInfo>,
    selectedPackage: String? = null,
    selectedPackages: Set<String> = emptySet(),
    isMultiSelect: Boolean = false,
    title: String = if (isMultiSelect) "Select Apps to Block" else "Choose Application",
    onAppSelected: (AppInfo) -> Unit = {},
    onTogglePackage: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showSystemApps by remember { mutableStateOf(false) }

    val filteredApps = remember(apps, searchQuery, showSystemApps) {
        apps.filter { app ->
            (showSystemApps || !app.isSystemApp) &&
                    (searchQuery.isBlank() ||
                            app.appName.contains(searchQuery.trim(), ignoreCase = true) ||
                            app.packageName.contains(searchQuery.trim(), ignoreCase = true))
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = HolstColors.Surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = HolstColors.Border) },
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        modifier = Modifier.fillMaxHeight(0.88f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = HolstColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )

                if (isMultiSelect) {
                    Text(
                        text = "${selectedPackages.size} Selected",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = HolstColors.PrimarySand,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search installed apps...", color = HolstColors.MutedBlue) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = HolstColors.MutedBlue
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = HolstColors.MutedBlue
                            )
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = HolstColors.TextPrimary,
                    unfocusedTextColor = HolstColors.TextPrimary,
                    focusedBorderColor = HolstColors.PrimarySand,
                    unfocusedBorderColor = HolstColors.Border
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("app_picker_search_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // System Apps Filter Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${filteredApps.size} apps found",
                    style = MaterialTheme.typography.bodySmall.copy(color = HolstColors.MutedBlue)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { showSystemApps = !showSystemApps }
                ) {
                    Checkbox(
                        checked = showSystemApps,
                        onCheckedChange = { showSystemApps = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = HolstColors.PrimarySand,
                            checkmarkColor = HolstColors.Background,
                            uncheckedColor = HolstColors.Border
                        )
                    )
                    Text(
                        text = "Include system apps",
                        style = MaterialTheme.typography.bodySmall.copy(color = HolstColors.MutedBlue)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // App List
            if (filteredApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No applications match your search.",
                        color = HolstColors.MutedBlue,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        val isSelected = if (isMultiSelect) {
                            selectedPackages.contains(app.packageName)
                        } else {
                            selectedPackage == app.packageName
                        }

                        AppListItemRow(
                            app = app,
                            isSelected = isSelected,
                            isMultiSelect = isMultiSelect,
                            onClick = {
                                if (isMultiSelect) {
                                    onTogglePackage(app.packageName)
                                } else {
                                    onAppSelected(app)
                                    onDismiss()
                                }
                            }
                        )
                    }
                }
            }

            if (isMultiSelect) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = HolstColors.PrimarySand),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("app_picker_done_button")
                ) {
                    Text(
                        text = "Done (${selectedPackages.size} Selected)",
                        color = HolstColors.Background,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun AppListItemRow(
    app: AppInfo,
    isSelected: Boolean,
    isMultiSelect: Boolean,
    onClick: () -> Unit
) {
    val imageBitmap = remember(app) { app.toImageBitmap() }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) HolstColors.Border.copy(alpha = 0.6f) else HolstColors.Surface,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, HolstColors.PrimarySand) else androidx.compose.foundation.BorderStroke(0.5.dp, HolstColors.Border),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("app_picker_item_${app.packageName}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(HolstColors.Border.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = app.appName,
                        modifier = Modifier.size(36.dp)
                    )
                } else {
                    Text(
                        text = app.appName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = HolstColors.PrimarySand,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = HolstColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.labelSmall.copy(color = HolstColors.MutedBlue),
                    maxLines = 1
                )
            }

            // Selection indicator
            if (isMultiSelect) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) HolstColors.AlertCrimson else Color.Transparent)
                        .border(
                            1.5.dp,
                            if (isSelected) HolstColors.AlertCrimson else HolstColors.Border,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = HolstColors.PrimarySand,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
