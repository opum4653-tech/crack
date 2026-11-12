package com.andorid.crackmaster.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andorid.crackmaster.ui.theme.ActivePillText
import com.andorid.crackmaster.ui.theme.BackgroundDark
import com.andorid.crackmaster.ui.theme.ErrorRed
import com.andorid.crackmaster.ui.theme.NeonGreen
import com.andorid.crackmaster.ui.theme.TextMuted
import com.andorid.crackmaster.ui.theme.TextPrimary
import com.andorid.crackmaster.ui.theme.TextSecondary
import com.andorid.crackmaster.ui.viewmodel.CrackMasterViewModel

@Composable
fun LoginScreen(
    viewModel: CrackMasterViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var isKeyVisible by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Logo / title ─────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0F2018)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Key,
                    contentDescription = null,
                    tint = NeonGreen,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "• Crack Master",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = NeonGreen
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Enter your license key to continue",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ── License key field ────────────────────────────────────────────
            OutlinedTextField(
                value = state.loginKey,
                onValueChange = { viewModel.onLoginKeyChanged(it) },
                label = { Text("License Key", fontSize = 12.sp, color = NeonGreen) },
                placeholder = {
                    Text(
                        "CM-XXXX-XXXX-XXXX",
                        fontSize = 12.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                },
                singleLine = true,
                enabled = !state.isLoginAuthenticating,
                visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            val text = clipboard.getText()?.text.orEmpty().trim()
                            if (text.isNotBlank()) viewModel.onLoginKeyChanged(text)
                        }) {
                            Icon(
                                imageVector = Icons.Filled.ContentPaste,
                                contentDescription = "Paste",
                                tint = NeonGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                            Icon(
                                imageVector = if (isKeyVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor      = TextPrimary,
                    unfocusedTextColor    = TextPrimary,
                    focusedBorderColor    = NeonGreen,
                    unfocusedBorderColor  = NeonGreen.copy(alpha = 0.4f),
                    cursorColor           = NeonGreen,
                    focusedContainerColor   = Color(0xFF0C0F14),
                    unfocusedContainerColor = Color(0xFF0C0F14),
                    disabledTextColor     = TextMuted,
                    disabledBorderColor   = NeonGreen.copy(alpha = 0.2f),
                    disabledContainerColor  = Color(0xFF0C0F14)
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (!state.isLoginAuthenticating) viewModel.authenticateInitial()
                    }
                )
            )

            if (!state.loginError.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.loginError!!,
                    fontSize = 12.sp,
                    color = ErrorRed,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Authenticate button ──────────────────────────────────────────
            Button(
                onClick = { viewModel.authenticateInitial() },
                enabled = !state.isLoginAuthenticating && state.loginKey.trim().isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor         = NeonGreen,
                    contentColor           = ActivePillText,
                    disabledContainerColor = NeonGreen.copy(alpha = 0.35f),
                    disabledContentColor   = ActivePillText.copy(alpha = 0.4f)
                )
            ) {
                if (state.isLoginAuthenticating) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(22.dp),
                        color       = ActivePillText,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Verifying...", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text("AUTHENTICATE", fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
        }
    }
}
