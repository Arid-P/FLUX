package com.fluxfoxus.features.onboarding

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.fluxfoxus.core.datastore.UserPreferencesRepository
import com.fluxfoxus.core.designsystem.theme.HolstColors
import com.fluxfoxus.core.permission.PermissionChecker
import com.fluxfoxus.core.permission.ShieldStatus
import kotlinx.coroutines.launch

@Composable
fun OnboardingWizardScreen(
    userPrefs: UserPreferencesRepository,
    onFinishOnboarding: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var step by remember { mutableIntStateOf(1) }
    var shieldStatus by remember { mutableStateOf(PermissionChecker.getShieldStatus(context)) }

    // Auto-detect permissions when returning to app from system settings
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        shieldStatus = PermissionChecker.getShieldStatus(context)
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        shieldStatus = PermissionChecker.getShieldStatus(context)
    }

    fun completeOnboarding() {
        coroutineScope.launch {
            userPrefs.setOnboardingCompleted(true)
            onFinishOnboarding()
        }
    }

    Scaffold(
        containerColor = HolstColors.Background
    ) { padding ->
        when (step) {
            1 -> {
                // Step 1: Welcome & Mission Screen
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Aperture Emblem
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(HolstColors.Surface)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "FluxFoxus Shield",
                            tint = HolstColors.PrimarySand,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Text(
                        text = "Forge Unbreakable Focus",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = HolstColors.TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "FluxFoxus enforces hard digital boundaries for your Olympiad and JEE study blocks. 100% offline, private, and local.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = HolstColors.MutedBlue
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(48.dp))

                    Button(
                        onClick = { step = 2 },
                        colors = ButtonDefaults.buttonColors(containerColor = HolstColors.PrimarySand),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("configure_shields_button")
                    ) {
                        Text(
                            text = "Configure Focus Shields ➔",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = HolstColors.Background,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            2 -> {
                // Step 2: The Interactive Shields Checklist
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "FLUXFOXUS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = HolstColors.MutedBlue,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = "Step 2/3",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = HolstColors.PrimarySand,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "🛡️ Activate Focus Shields",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = HolstColors.TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Text(
                        text = "Enable the required Android shields to enforce deep focus blocks. All data stays local on your device.",
                        style = MaterialTheme.typography.bodySmall.copy(color = HolstColors.MutedBlue)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Shield 1: Notifications
                        item {
                            ShieldCard(
                                shieldIndex = 1,
                                title = "🔔 Focus Timers & Break Alerts",
                                description = "Ensures timer chimes and break warnings alert in background.",
                                isGranted = shieldStatus.hasNotifications,
                                isOptional = false,
                                onEnable = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                            )
                        }

                        // Shield 2: Usage Stats
                        item {
                            ShieldCard(
                                shieldIndex = 2,
                                title = "📊 24h Momentum & App Limits",
                                description = "Allows FF to calculate daily study analytics and monitor limits.",
                                isGranted = shieldStatus.hasUsageStats,
                                isOptional = false,
                                onEnable = {
                                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                }
                            )
                        }

                        // Shield 3: Display Overlay
                        item {
                            ShieldCard(
                                shieldIndex = 3,
                                title = "🪟 Distraction Intervention Window",
                                description = "Allows FF to present the 5/10/20/40m timer and habit friction.",
                                isGranted = shieldStatus.hasOverlay,
                                isOptional = false,
                                onEnable = {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    ).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                }
                            )
                        }

                        // Shield 4: Accessibility Service
                        item {
                            ShieldCard(
                                shieldIndex = 4,
                                title = "🔒 Core Focus Interceptor",
                                description = "Instantly closes blacklisted apps and checks YouTube whitelist.",
                                isGranted = shieldStatus.hasAccessibility,
                                isOptional = false,
                                onEnable = {
                                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                }
                            )
                        }

                        // Shield 5: Battery Optimization (Optional)
                        item {
                            ShieldCard(
                                shieldIndex = 5,
                                title = "⚡ Prevent Background Timer Kills",
                                description = "Keeps multi-hour sessions immune to OS sleep and OEM cleaners.",
                                isGranted = shieldStatus.hasBatteryOptimizationIgnored,
                                isOptional = true,
                                onEnable = {
                                    val intent = Intent(
                                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                        Uri.parse("package:${context.packageName}")
                                    ).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Continue CTA strictly gated until all mandatory shields are ready
                    Button(
                        onClick = { step = 3 },
                        enabled = shieldStatus.areMandatoryReady,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HolstColors.PrimarySand,
                            disabledContainerColor = HolstColors.Border.copy(alpha = 0.4f),
                            contentColor = HolstColors.Background,
                            disabledContentColor = HolstColors.TextMuted
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("continue_to_dashboard_button")
                    ) {
                        Text(
                            text = if (shieldStatus.areMandatoryReady) {
                                "Continue to Device Protection ➔"
                            } else {
                                "Grant All 4 Mandatory Shields to Continue"
                            },
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            3 -> {
                // Step 3: OEM Device Killer Survival Guide
                val manufacturer = Build.MANUFACTURER.lowercase()
                val brandTitle = when {
                    manufacturer.contains("samsung") -> "Samsung OneUI"
                    manufacturer.contains("xiaomi") || manufacturer.contains("poco") || manufacturer.contains("redmi") -> "Xiaomi MIUI / HyperOS"
                    manufacturer.contains("oneplus") || manufacturer.contains("oppo") || manufacturer.contains("realme") -> "OnePlus OxygenOS / ColorOS"
                    else -> "Stock Android / Pixel"
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "FLUXFOXUS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = HolstColors.MutedBlue,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = "Step 3/3",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = HolstColors.PrimarySand,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "⚡ OEM Background Protection",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = HolstColors.TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Text(
                        text = "Detected: $brandTitle",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = HolstColors.PrimarySand,
                            fontWeight = FontWeight.SemiBold
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Android manufacturers aggressively terminate background timers. Follow these recommendations to ensure 100% session reliability:",
                        style = MaterialTheme.typography.bodySmall.copy(color = HolstColors.MutedBlue)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    when {
                        manufacturer.contains("samsung") -> {
                            OemGuideCard(
                                stepNumber = 1,
                                instruction = "Lock FluxFoxus in Recent Apps",
                                detail = "Open your app switcher, tap the FluxFoxus icon at the top, and select 'Lock this app' (🔒)."
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OemGuideCard(
                                stepNumber = 2,
                                instruction = "Add to 'Never Auto-Sleeping Apps'",
                                detail = "Settings ➔ Battery ➔ Background usage limits ➔ Never auto-sleeping apps ➔ Add FluxFoxus."
                            )
                        }
                        manufacturer.contains("xiaomi") || manufacturer.contains("poco") || manufacturer.contains("redmi") -> {
                            OemGuideCard(
                                stepNumber = 1,
                                instruction = "Enable Autostart in App Info",
                                detail = "Long press FluxFoxus icon ➔ App Info ➔ Toggle 'Autostart' ON."
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OemGuideCard(
                                stepNumber = 2,
                                instruction = "Battery Saver: No Restrictions",
                                detail = "App Info ➔ Battery saver ➔ Select 'No restrictions'."
                            )
                        }
                        manufacturer.contains("oneplus") || manufacturer.contains("oppo") || manufacturer.contains("realme") -> {
                            OemGuideCard(
                                stepNumber = 1,
                                instruction = "Disable Auto-Optimize",
                                detail = "Settings ➔ Battery ➔ More settings ➔ Optimize battery use ➔ FluxFoxus ➔ Don't optimize."
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OemGuideCard(
                                stepNumber = 2,
                                instruction = "Allow Background Activity",
                                detail = "App Info ➔ Battery usage ➔ Toggle 'Allow background activity' ON."
                            )
                        }
                        else -> {
                            OemGuideCard(
                                stepNumber = 1,
                                instruction = "Set Battery Usage to 'Unrestricted'",
                                detail = "Settings ➔ Apps ➔ FluxFoxus ➔ App battery usage ➔ Choose 'Unrestricted'."
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = { completeOnboarding() },
                        colors = ButtonDefaults.buttonColors(containerColor = HolstColors.PrimarySand),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("enter_fluxfoxus_button")
                    ) {
                        Text(
                            text = "All Shields Active — Enter FluxFoxus",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = HolstColors.Background,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OemGuideCard(
    stepNumber: Int,
    instruction: String,
    detail: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = HolstColors.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, HolstColors.Border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "$stepNumber. $instruction",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = HolstColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall.copy(color = HolstColors.MutedBlue)
            )
        }
    }
}

@Composable
fun ShieldCard(
    shieldIndex: Int,
    title: String,
    description: String,
    isGranted: Boolean,
    isOptional: Boolean,
    onEnable: () -> Unit
) {
    val borderColor = if (isGranted) HolstColors.AccentSage else HolstColors.Border

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = HolstColors.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$shieldIndex. $title",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = HolstColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.weight(1f)
                )

                if (isGranted) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = HolstColors.AccentSage
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Granted",
                                tint = HolstColors.Background,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Granted 🟢",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = HolstColors.Background,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = HolstColors.Surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, HolstColors.Border),
                        modifier = Modifier.clickable(onClick = onEnable)
                    ) {
                        Text(
                            text = if (isOptional) "Enable (Opt) ➔" else "Enable ➔",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = HolstColors.PrimarySand,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(color = HolstColors.MutedBlue)
            )
        }
    }
}
