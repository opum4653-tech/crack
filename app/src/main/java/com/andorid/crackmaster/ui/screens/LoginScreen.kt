package com.andorid.crackmaster.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andorid.crackmaster.ui.components.APatchCard
import com.andorid.crackmaster.ui.components.APatchPillButton
import com.andorid.crackmaster.ui.theme.BackgroundDark
import com.andorid.crackmaster.ui.theme.CardBackground
import com.andorid.crackmaster.ui.theme.ErrorRed
import com.andorid.crackmaster.ui.theme.NeonGreen
import com.andorid.crackmaster.ui.theme.TextMuted
import com.andorid.crackmaster.ui.theme.TextPrimary
import com.andorid.crackmaster.ui.theme.TextSecondary
import com.andorid.crackmaster.ui.viewmodel.CrackMasterViewModel

@Composable
fun LoginScreen(
    viewModel: CrackMasterViewModel,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF07090C),
                        BackgroundDark,
                        Color(0xFF050709)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Centered Glowing Card (Exact mockup reproduction)
        APatchCard(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .padding(vertical = 24.dp),
            border = BorderStroke(1.5.dp, NeonGreen.copy(alpha = 0.85f)),
            containerColor = Color(0xFF101419).copy(alpha = 0.95f),
            cornerRadius = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Title: Crack Master
                Text(
                    text = "Crack",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Master",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Subtitle: - SECURE LOGIN -
                Text(
                    text = "— SECURE LOGIN —",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonGreen,
                    letterSpacing = 3.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Helper text
                Text(
                    text = "Enter your license key to start",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // OutlinedTextField for License Key with Paste trailing icon
                OutlinedTextField(
                    value = state.licenseKey,
                    onValueChange = { viewModel.onLicenseKeyChanged(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            text = "License Key",
                            fontSize = 12.sp,
                            color = NeonGreen
                        )
                    },
                    placeholder = {
                        Text(
                            text = "XXXX-XXXX-XXXX-XXXX",
                            fontSize = 13.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = clipboard.primaryClip
                                if (clip != null && clip.itemCount > 0) {
                                    val text = clip.getItemAt(0).text?.toString().orEmpty()
                                    if (text.isNotBlank()) {
                                        viewModel.onLicenseKeyChanged(text.trim())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentPaste,
                                contentDescription = "Paste from clipboard",
                                tint = NeonGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonGreen,
                        unfocusedBorderColor = NeonGreen.copy(alpha = 0.45f),
                        cursorColor = NeonGreen,
                        focusedContainerColor = Color(0xFF0B0E12),
                        unfocusedContainerColor = Color(0xFF0B0E12)
                    )
                )

                if (!state.authError.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = state.authError!!,
                        fontSize = 12.sp,
                        color = ErrorRed,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Action Button: AUTHENTICATE
                if (state.isAuthenticating) {
                    CircularProgressIndicator(
                        color = NeonGreen,
                        modifier = Modifier.size(36.dp)
                    )
                } else {
                    APatchPillButton(
                        text = "AUTHENTICATE",
                        onClick = { viewModel.authenticateInitial(onLoginSuccess) },
                        containerColor = NeonGreen
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Footer: V2.0 • Crack Master
                Text(
                    text = "V2.0  •  Crack Master",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
