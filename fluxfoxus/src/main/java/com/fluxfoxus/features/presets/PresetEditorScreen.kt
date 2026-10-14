package com.fluxfoxus.features.presets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.window.Dialog
import com.fluxfoxus.core.designsystem.theme.*

import com.fluxfoxus.core.designsystem.component.AppPickerBottomSheet
import com.fluxfoxus.core.designsystem.component.RusticMedleyHeader
import com.fluxfoxus.core.designsystem.component.AmberAutumnAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetEditorScreen(
    viewModel: PresetEditorViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = HolstColors.Background,
        topBar = {
            RusticMedleyHeader(
                title = if (state.isEditing) "Edit Preset" else "New Preset",
                actions = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = HolstColors.TextPrimary
                        )
                    }
                    TextButton(
                        onClick = { viewModel.savePreset(onSaved) },
                        enabled = state.name.isNotBlank(),
                        modifier = Modifier.testTag("save_preset_top_button")
                    ) {
                        Text(
                            text = "Save",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (state.name.isNotBlank()) AmberAutumnAccent else HolstColors.Border
                            )
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = HolstColors.Background,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = { viewModel.savePreset(onSaved) },
                    enabled = state.name.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HolstColors.PrimarySand,
                        disabledContainerColor = HolstColors.Border
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_preset_bottom_button")
                ) {
                    Text(
                        text = "Save Preset",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = if (state.name.isNotBlank()) HolstColors.Background else HolstColors.TextMuted,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Preset Identity Section
            item {
                SectionHeader("PRESET IDENTITY")
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = HolstColors.Surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, HolstColors.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Emoji Button
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = HolstColors.Border,
                            modifier = Modifier
                                .size(48.dp)
                                .clickable { viewModel.setEmojiSheetVisible(true) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = state.emoji, fontSize = 24.sp)
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        OutlinedTextField(
                            value = state.name,
                            onValueChange = { viewModel.setName(it) },
                            placeholder = { Text("Preset name (e.g. Olympiad Math)", color = HolstColors.MutedBlue) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = HolstColors.TextPrimary,
                                unfocusedTextColor = HolstColors.TextPrimary,
                                focusedBorderColor = HolstColors.PrimarySand,
                                unfocusedBorderColor = HolstColors.Border
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("preset_name_input")
                        )
                    }
                }
            }

            // Break Configuration Section
            item {
                SectionHeader("BREAK CONFIGURATION")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Number of breaks
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = HolstColors.Surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, HolstColors.Border),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "NUMBER OF BREAKS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = HolstColors.MutedBlue,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { viewModel.setBreakCount(state.breakCount - 1) }) {
                                    Text("-", style = MaterialTheme.typography.titleLarge.copy(color = HolstColors.PrimarySand))
                                }
                                Text(
                                    text = "${state.breakCount}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = HolstColors.TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                TextButton(onClick = { viewModel.setBreakCount(state.breakCount + 1) }) {
                                    Text("+", style = MaterialTheme.typography.titleLarge.copy(color = HolstColors.PrimarySand))
                                }
                            }
                            Text(
                                text = "breaks",
                                style = MaterialTheme.typography.labelSmall.copy(color = HolstColors.MutedBlue)
                            )
                        }
                    }

                    // Break Duration
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = HolstColors.Surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, HolstColors.Border),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "BREAK DURATION",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = HolstColors.MutedBlue,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (state.breakCount > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(onClick = { viewModel.setBreakDuration(state.breakDurationMinutes - 1) }) {
                                        Text("-", style = MaterialTheme.typography.titleLarge.copy(color = HolstColors.PrimarySand))
                                    }
                                    Text(
                                        text = "${state.breakDurationMinutes}m",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = HolstColors.TextPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    TextButton(onClick = { viewModel.setBreakDuration(state.breakDurationMinutes + 1) }) {
                                        Text("+", style = MaterialTheme.typography.titleLarge.copy(color = HolstColors.PrimarySand))
                                    }
                                }
                                Text(
                                    text = "minutes",
                                    style = MaterialTheme.typography.labelSmall.copy(color = HolstColors.MutedBlue)
                                )
                            } else {
                                Box(
                                    modifier = Modifier.height(48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Breaks disabled",
                                        style = MaterialTheme.typography.bodySmall.copy(color = HolstColors.MutedBlue)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Content-Filtered Apps Section
            item {
                SectionHeader("CONTENT-FILTERED APPS")
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = HolstColors.Surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, HolstColors.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // YouTube
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("▶️ YouTube", style = MaterialTheme.typography.bodyMedium.copy(color = HolstColors.TextPrimary, fontWeight = FontWeight.Bold))
                            }
                            ThreeWayFilterToggle(
                                selectedMode = state.youtubeMode,
                                onSelectMode = { viewModel.setYoutubeMode(it) }
                            )
                            Text(
                                text = "Status: ${state.youtubeWhitelists.size} Study Channels Whitelisted",
                                style = MaterialTheme.typography.labelSmall.copy(color = HolstColors.MutedBlue)
                            )
                        }

                        HorizontalDivider(color = HolstColors.Border)

                        // Web Browsers
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🌐 Web Browsers (Chrome, Firefox, Brave)", style = MaterialTheme.typography.bodyMedium.copy(color = HolstColors.TextPrimary, fontWeight = FontWeight.Bold))
                            }
                            ThreeWayFilterToggle(
                                selectedMode = state.browserMode,
                                onSelectMode = { viewModel.setBrowserMode(it) }
                            )
                            Text(
                                text = "Status: ${state.browserWhitelists.size} Study Websites Whitelisted",
                                style = MaterialTheme.typography.labelSmall.copy(color = HolstColors.MutedBlue)
                            )
                        }

                        HorizontalDivider(color = HolstColors.Border)

                        // Manage Whitelists Button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.openWhitelistManager("YOUTUBE") }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚙️ Manage Global Content Whitelists",
                                style = MaterialTheme.typography.bodyMedium.copy(color = HolstColors.PrimarySand, fontWeight = FontWeight.SemiBold)
                            )
                            Icon(Icons.Default.ChevronRight, contentDescription = "Manage", tint = HolstColors.PrimarySand)
                        }
                    }
                }
            }

            // App Restrictions Section
            item {
                SectionHeader("APP RESTRICTIONS")
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = HolstColors.Surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, HolstColors.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Blocked Apps: ${state.blockedPackages.size} Selected",
                            style = MaterialTheme.typography.bodyMedium.copy(color = HolstColors.TextPrimary)
                        )

                        Button(
                            onClick = { viewModel.setAppSelectorVisible(true) },
                            colors = ButtonDefaults.buttonColors(containerColor = HolstColors.Border),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Configure Blocked Apps (${state.blockedPackages.size}) →",
                                style = MaterialTheme.typography.bodyMedium.copy(color = HolstColors.TextPrimary)
                            )
                        }
                    }
                }
            }

            // Description / Syllabus Notes Section
            item {
                SectionHeader("DESCRIPTION / SYLLABUS NOTES (OPTIONAL)")
                OutlinedTextField(
                    value = state.description,
                    onValueChange = { viewModel.setDescription(it) },
                    placeholder = { Text("e.g. Mock exam practice only. No solutions until 6 PM.", color = HolstColors.MutedBlue) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = HolstColors.TextPrimary,
                        unfocusedTextColor = HolstColors.TextPrimary,
                        focusedBorderColor = HolstColors.PrimarySand,
                        unfocusedBorderColor = HolstColors.Border
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                )
            }

            // Delete Button (if editing)
            if (state.isEditing) {
                item {
                    TextButton(
                        onClick = { viewModel.deletePreset(onSaved) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = HolstColors.AlertCrimson)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Delete Preset",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = HolstColors.AlertCrimson,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Emoji Picker Bottom Sheet / Dialog
    if (state.isEmojiSheetVisible) {
        val academicFavorites = listOf("📐", "🔬", "📚", "💻", "🎯", "⚡", "🧠", "⏳", "✍️", "♟️")
        val generalEmojis = listOf("🚀", "🔥", "🛡️", "⭐", "📖", "💡", "☕", "🪐", "🧩", "🏆", "🎨", "🌿")

        Dialog(onDismissRequest = { viewModel.setEmojiSheetVisible(false) }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = HolstColors.Surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, HolstColors.Border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Choose Focus Emoji",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = HolstColors.TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Text("Academic Favorites", style = MaterialTheme.typography.labelSmall.copy(color = HolstColors.PrimarySand))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(academicFavorites) { emoji ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = HolstColors.Border,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clickable { viewModel.setEmoji(emoji) }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = emoji, fontSize = 22.sp)
                                }
                            }
                        }
                    }

                    Text("General Icons", style = MaterialTheme.typography.labelSmall.copy(color = HolstColors.MutedBlue))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(generalEmojis) { emoji ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = HolstColors.Border,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clickable { viewModel.setEmoji(emoji) }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = emoji, fontSize = 22.sp)
                                }
                            }
                        }
                    }

                    TextButton(
                        onClick = { viewModel.setEmojiSheetVisible(false) },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Cancel", color = HolstColors.MutedBlue)
                    }
                }
            }
        }
    }

    // App Selector Dialog
    // App Selector Bottom Sheet
    if (state.isAppSelectorVisible) {
        AppPickerBottomSheet(
            apps = state.installedApps,
            selectedPackages = state.blockedPackages,
            isMultiSelect = true,
            title = "Select Apps to Block",
            onTogglePackage = { pkg -> viewModel.toggleAppBlocked(pkg) },
            onDismiss = { viewModel.setAppSelectorVisible(false) }
        )
    }

    // Whitelist Manager Dialog with Typing Verification
    if (state.isWhitelistManagerVisible) {
        WhitelistManagerDialog(
            type = state.whitelistManagerType,
            items = if (state.whitelistManagerType == "YOUTUBE") state.youtubeWhitelists else state.browserWhitelists,
            onClose = { viewModel.closeWhitelistManager() },
            onSwitchType = { viewModel.openWhitelistManager(it) },
            onAddItem = { t, id, label -> viewModel.addWhitelistItem(t, id, label) },
            onDeleteItem = { id -> viewModel.deleteWhitelistItem(id) }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
            color = HolstColors.MutedBlue,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        ),
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun ThreeWayFilterToggle(
    selectedMode: String,
    onSelectMode: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        listOf("BLOCK" to "Block", "ALLOW" to "Allow", "STUDY_MODE" to "Study Mode 🛡️").forEach { (mode, label) ->
            val isSelected = selectedMode == mode
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) {
                    if (mode == "STUDY_MODE") HolstColors.AccentSage else HolstColors.PrimarySand
                } else HolstColors.Border,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelectMode(mode) }
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isSelected) HolstColors.Background else HolstColors.TextPrimary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    ),
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .wrapContentWidth(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
private fun WhitelistManagerDialog(
    type: String,
    items: List<com.fluxfoxus.core.database.entity.GlobalContentWhitelistEntity>,
    onClose: () -> Unit,
    onSwitchType: (String) -> Unit,
    onAddItem: (type: String, identifier: String, label: String) -> Unit,
    onDeleteItem: (id: String) -> Unit
) {
    var isAdding by remember { mutableStateOf(false) }
    var inputIdentifier by remember { mutableStateOf("") }
    var inputLabel by remember { mutableStateOf("") }
    var quoteInput by remember { mutableStateOf("") }

    val verificationQuote = "Focus is the art of knowing what to ignore"
    val isQuoteVerified = quoteInput.trim().equals(verificationQuote, ignoreCase = true)

    Dialog(onDismissRequest = onClose) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = HolstColors.Surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, HolstColors.Border),
            modifier = Modifier
                .fillMaxWidth()
                .height(520.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize()
            ) {
                Text(
                    text = "Global Content Whitelists",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = HolstColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Tabs: YouTube vs Web Browsers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (type == "YOUTUBE") HolstColors.PrimarySand else HolstColors.Border,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSwitchType("YOUTUBE") }
                    ) {
                        Text(
                            text = "YouTube Channels",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (type == "YOUTUBE") HolstColors.Background else HolstColors.TextPrimary,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .wrapContentWidth(Alignment.CenterHorizontally)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (type == "BROWSER") HolstColors.PrimarySand else HolstColors.Border,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSwitchType("BROWSER") }
                    ) {
                        Text(
                            text = "Web Domains",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (type == "BROWSER") HolstColors.Background else HolstColors.TextPrimary,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .wrapContentWidth(Alignment.CenterHorizontally)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (!isAdding) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (items.isEmpty()) {
                            item {
                                Text(
                                    text = "No whitelisted items yet.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = HolstColors.MutedBlue),
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        } else {
                            items(items) { item ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = HolstColors.Border,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.displayLabel, style = MaterialTheme.typography.bodyMedium.copy(color = HolstColors.TextPrimary, fontWeight = FontWeight.Bold))
                                            Text(item.targetIdentifier, style = MaterialTheme.typography.labelSmall.copy(color = HolstColors.MutedBlue))
                                        }
                                        IconButton(onClick = { onDeleteItem(item.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = HolstColors.AlertCrimson)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { isAdding = true },
                            colors = ButtonDefaults.buttonColors(containerColor = HolstColors.PrimarySand),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+ Add Whitelist", color = HolstColors.Background, fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = onClose) {
                            Text("Close", color = HolstColors.MutedBlue)
                        }
                    }
                } else {
                    // Typing Verification Friction Engine to Add Whitelist Item
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = inputLabel,
                            onValueChange = { inputLabel = it },
                            label = { Text("Display Label (e.g. 3Blue1Brown)", color = HolstColors.TextMuted) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = inputIdentifier,
                            onValueChange = { inputIdentifier = it },
                            label = { Text(if (type == "YOUTUBE") "Channel Handle (@3blue1brown)" else "Domain (desmos.com)", color = HolstColors.TextMuted) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "Discipline Verification:",
                            style = MaterialTheme.typography.labelSmall.copy(color = HolstColors.PrimarySand, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "\"$verificationQuote\"",
                            style = MaterialTheme.typography.bodySmall.copy(color = HolstColors.TextPrimary, fontWeight = FontWeight.Medium)
                        )
                        OutlinedTextField(
                            value = quoteInput,
                            onValueChange = { quoteInput = it },
                            placeholder = { Text("Type the quote exactly to verify...", color = HolstColors.MutedBlue) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (isQuoteVerified && inputIdentifier.isNotBlank() && inputLabel.isNotBlank()) {
                                    onAddItem(type, inputIdentifier.trim(), inputLabel.trim())
                                    isAdding = false
                                    inputIdentifier = ""
                                    inputLabel = ""
                                    quoteInput = ""
                                }
                            },
                            enabled = isQuoteVerified && inputIdentifier.isNotBlank() && inputLabel.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = HolstColors.PrimarySand),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Verify & Add", color = HolstColors.Background, fontWeight = FontWeight.Bold)
                        }

                        TextButton(onClick = { isAdding = false }) {
                            Text("Cancel", color = HolstColors.MutedBlue)
                        }
                    }
                }
            }
        }
    }
}
