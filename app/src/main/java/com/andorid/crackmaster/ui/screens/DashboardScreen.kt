package com.andorid.crackmaster.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.andorid.crackmaster.R
import com.andorid.crackmaster.data.SystemDetector
import com.andorid.crackmaster.ui.components.APatchCard
import com.andorid.crackmaster.ui.theme.ActivePillBg
import com.andorid.crackmaster.ui.theme.ActivePillText
import com.andorid.crackmaster.ui.theme.BackgroundDark
import com.andorid.crackmaster.ui.theme.CardBackground
import com.andorid.crackmaster.ui.theme.ErrorRed
import com.andorid.crackmaster.ui.theme.NeonGreen
import com.andorid.crackmaster.ui.theme.TextMuted
import com.andorid.crackmaster.ui.theme.TextPrimary
import com.andorid.crackmaster.ui.theme.TextSecondary
import com.andorid.crackmaster.ui.viewmodel.CrackMasterViewModel

@Composable
fun DashboardScreen(
    viewModel: CrackMasterViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(18.dp))

        // Header: • Crack Master + ACTIVE Badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "•",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = NeonGreen
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Crack Master",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonGreen
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(ActivePillBg)
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ACTIVE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = ActivePillText
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Game Card — taps open the game
        APatchCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val launchIntent = context.packageManager
                        .getLaunchIntentForPackage(state.gameInfo.packageName)
                        ?: context.packageManager
                            .getLaunchIntentForPackage(SystemDetector.PKG_FF_MAX)
                    if (launchIntent != null) {
                        context.startActivity(launchIntent)
                    } else {
                        Toast.makeText(context, "Game not installed", Toast.LENGTH_SHORT).show()
                    }
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Game icon or fallback logo
                val gameDrawable = try {
                    context.packageManager.getApplicationIcon(state.gameInfo.packageName)
                } catch (_: Exception) {
                    try {
                        context.packageManager.getApplicationIcon(SystemDetector.PKG_FF_MAX)
                    } catch (_: Exception) { null }
                }

                if (gameDrawable != null) {
                    Image(
                        bitmap = gameDrawable.toBitmap(width = 48, height = 48).asImageBitmap(),
                        contentDescription = "Game Icon",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.logo_crackmaster),
                        contentDescription = "App Logo",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Free Fire",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = if (state.gameInfo.isInstalled)
                            "Version: ${state.gameInfo.versionName}"
                        else "Version: -",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        GameChipBadge(label = state.gameInfo.modeBadge.ifBlank { "EXTERNAL" })
                        GameChipBadge(label = state.gameInfo.architecture.ifBlank { "ARM64-V8A" })
                        Text(
                            text = if (state.gameInfo.isInstalled) "• INSTALLED" else "• NOT INSTALLED",
                            fontSize = 10.sp,
                            color = if (state.gameInfo.isInstalled) NeonGreen else TextMuted,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // SYSTEM INFO
        Text(
            text = "SYSTEM INFO",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = NeonGreen,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        APatchCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SystemInfoRow(
                    label = "Root Access",
                    value = state.systemInfo.rootStatusText,
                    valueColor = if (state.systemInfo.isRootGranted) NeonGreen else ErrorRed
                )
                SystemInfoRow(
                    label = "Android",
                    value = state.systemInfo.androidVersion,
                    valueColor = NeonGreen
                )
                SystemInfoRow(
                    label = "Device Name",
                    value = state.systemInfo.deviceName,
                    valueColor = NeonGreen
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ACTIONS — side-by-side START / STOP
        Text(
            text = "ACTIONS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = NeonGreen,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (state.isLaunching) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (state.downloadProgress in 1..99) {
                    LinearProgressIndicator(
                        progress = { state.downloadProgress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = NeonGreen,
                        trackColor = Color(0xFF262C36)
                    )
                } else {
                    CircularProgressIndicator(
                        color = NeonGreen,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // START card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clickable(enabled = !state.isEngineRunning) {
                            viewModel.toggleEngine()
                        },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (!state.isEngineRunning) NeonGreen else Color(0xFF1A2B1A)
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "START",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (!state.isEngineRunning) Color(0xFF000000) else NeonGreen.copy(alpha = 0.4f),
                            textAlign = TextAlign.Center,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // STOP card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clickable(enabled = state.isEngineRunning) {
                            viewModel.toggleEngine()
                        },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (state.isEngineRunning) ErrorRed else Color(0xFF2B1A1A)
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "STOP",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (state.isEngineRunning) TextPrimary else ErrorRed.copy(alpha = 0.4f),
                            textAlign = TextAlign.Center,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
private fun GameChipBadge(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF0F2018))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = NeonGreen,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun SystemInfoRow(
    label: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = TextSecondary
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}
