package com.fluxfoxus.features.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.fluxfoxus.MainActivity
import com.fluxfoxus.core.database.FluxFoxusDatabase
import com.fluxfoxus.core.designsystem.theme.HolstColors
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

class FluxFoxusGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = FluxFoxusDatabase.getDatabase(context)
        val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Fetch today's usages and focus sessions
        val usages = try {
            db.dailyUsageDao().getDailyUsageForDate(todayIso).first()
        } catch (e: Exception) {
            emptyList()
        }

        val sessions = try {
            db.focusSessionDao().getAllSessions().first()
        } catch (e: Exception) {
            emptyList()
        }

        val totalScreenMs = usages.sumOf { it.totalTimeInForegroundMs }
        val totalScreenMins = (totalScreenMs / 60000L).toInt()
        val totalHours = totalScreenMins / 60
        val totalRemMins = totalScreenMins % 60

        val todaySessions = sessions.filter {
            val sDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.startTime))
            sDate == todayIso && it.status == "COMPLETED"
        }
        val focusMins = todaySessions.sumOf { (it.actualDurationSeconds / 60).coerceAtLeast(it.targetDurationMinutes) }
        val casualMins = (totalScreenMins - focusMins).coerceAtLeast(0)

        val ratio = if (casualMins <= 0) {
            if (focusMins > 0) 10.0f else 0.0f
        } else {
            focusMins.toFloat() / casualMins.toFloat()
        }

        provideContent {
            GlanceWidgetContent(
                totalHours = totalHours,
                totalMinutes = totalRemMins,
                focusRatio = ratio
            )
        }
    }
}

@Composable
fun GlanceWidgetContent(
    totalHours: Int,
    totalMinutes: Int,
    focusRatio: Float
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(0xFF0D1B2A)) // Abyssal Navy
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "FLUXFOXUS",
                style = TextStyle(
                    color = ColorProvider(Color(0xFF8E9CAE)), // Muted Blue
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = String.format(Locale.US, "⚡ %.1fx", focusRatio),
                style = TextStyle(
                    color = ColorProvider(Color(0xFFD4C4A8)), // Warm Sand
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            )
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        // Center: 4-category segment bar indicator
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(6.dp)
        ) {
            Box(modifier = GlanceModifier.defaultWeight().fillMaxHeight().background(Color(0xFF778D7A))) {} // Sage (Productive)
            Spacer(modifier = GlanceModifier.width(2.dp))
            Box(modifier = GlanceModifier.defaultWeight().fillMaxHeight().background(Color(0xFFD4C4A8))) {} // Sand (Semi-Prod)
            Spacer(modifier = GlanceModifier.width(2.dp))
            Box(modifier = GlanceModifier.defaultWeight().fillMaxHeight().background(Color(0xFFE07A5F))) {} // Coral (Distracting)
            Spacer(modifier = GlanceModifier.width(2.dp))
            Box(modifier = GlanceModifier.defaultWeight().fillMaxHeight().background(Color(0xFF415A77))) {} // Steel Blue (Others)
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        // Screen Time Hero Metric
        Text(
            text = "${totalHours}h ${totalMinutes}m",
            style = TextStyle(
                color = ColorProvider(Color(0xFFF4F1DE)), // Eggshell Cream
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        )

        Text(
            text = "Total Screen Time Today",
            style = TextStyle(
                color = ColorProvider(Color(0xFF8E9CAE)),
                fontSize = 10.sp
            )
        )

        Spacer(modifier = GlanceModifier.height(8.dp))

        // Quick Focus Trigger Button
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(Color(0xFF1B263B))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "▶ Start Focus Session",
                style = TextStyle(
                    color = ColorProvider(Color(0xFFD4C4A8)), // Warm Sand
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            )
        }
    }
}

class FluxFoxusGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FluxFoxusGlanceWidget()
}
