package com.andorid.crackmaster.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.andorid.crackmaster.R
import com.andorid.crackmaster.data.SystemDetector
import com.andorid.crackmaster.ui.components.APatchCard
import com.andorid.crackmaster.ui.components.APatchPillButton
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
    var modeDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(18.dp))

        // 1. Top Header Bar: • Crack Master + ACTIVE Badge
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

            // ACTIVE Badge Pill
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
                    color = ActivePillText,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Target Game Launcher Card (Interactive Game Launcher)
        APatchCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (state.gameInfo.isInstalled) {
                        val launched = SystemDetector.launchGame(context, state.gameInfo.packageName)
                        if (!launched) {
                            Toast.makeText(context, "Failed to launch game", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Game is not installed (${state.gameInfo.packageName})", Toast.LENGTH_SHORT).show()
                    }
                },
            border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.35f)),
            containerColor = CardBackground,
            cornerRadius = 20.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Real Installed Game App Icon via PackageManager / Coil
                    val iconPainter = if (state.gameInfo.icon != null) {
                        rememberAsyncImagePainter(model = state.gameInfo.icon)
                    } else {
                        painterResource(id = R.drawable.ic_game_ffmax)
                    }

                    Image(
                        painter = iconPainter,
                        contentDescription = state.gameInfo.title,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = state.gameInfo.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Version: ${state.gameInfo.versionName}",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        if (state.gameInfo.isInstalled) {
                            Text(
                                text = "Code: ${state.gameInfo.versionCode}",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Badges Row: [EXTERNAL] (Kernel removed as required by dj) & [Dynamic ABI] & • INSTALLED
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GameChipBadge(label = state.gameInfo.modeBadge)
                        GameChipBadge(label = state.gameInfo.architecture)
                    }

                    // Status: • INSTALLED / NOT INSTALLED
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "•",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = if (state.gameInfo.isInstalled) NeonGreen else TextMuted
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (state.gameInfo.isInstalled) "INSTALLED" else "NOT INSTALLED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (state.gameInfo.isInstalled) NeonGreen else TextMuted
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 3. SYSTEM INFO Section (Dynamic Hardware & OS Detection)
        Text(
            text = "SYSTEM INFO",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = NeonGreen,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        APatchCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = CardBackground,
            cornerRadius = 20.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
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

        // 4. SELECT MODE Section
        Text(
            text = "SELECT MODE",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = NeonGreen,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            APatchCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { modeDropdownExpanded = true },
                border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f)),
                containerColor = CardBackground,
                cornerRadius = 14.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = state.selectedMode,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NeonGreen
                    )
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = "Select Mode",
                        tint = NeonGreen
                    )
                }
            }

            DropdownMenu(
                expanded = modeDropdownExpanded,
                onDismissRequest = { modeDropdownExpanded = false },
                modifier = Modifier.background(Color(0xFF14171C))
            ) {
                state.availableModes.forEach { mode ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (mode == state.selectedMode) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = NeonGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(
                                    text = mode,
                                    color = if (mode == state.selectedMode) NeonGreen else TextPrimary,
                                    fontSize = 14.sp
                                )
                            }
                        },
                        onClick = {
                            viewModel.onSelectMode(mode)
                            modeDropdownExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 5. ACTIONS Section (START CHEAT / STOP CHEAT)
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
                    Spacer(modifier = Modifier.height(8.dp))
                } else {
                    CircularProgressIndicator(
                        color = NeonGreen,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    text = state.launchStatusMessage ?: "Running binary into memory...",
                    fontSize = 13.sp,
                    color = NeonGreen
                )
            }
        } else {
            APatchPillButton(
                text = if (state.isCheatRunning) "STOP CHEAT" else "START CHEAT",
                onClick = { viewModel.toggleCheatEngine() },
                containerColor = if (state.isCheatRunning) ErrorRed else NeonGreen,
                contentColor = if (state.isCheatRunning) TextPrimary else ActivePillText
            )
        }

        if (!state.launchStatusMessage.isNullOrBlank() && !state.isLaunching) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = state.launchStatusMessage!!,
                fontSize = 12.sp,
                color = if (state.isCheatRunning) NeonGreen else ErrorRed
            )
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
