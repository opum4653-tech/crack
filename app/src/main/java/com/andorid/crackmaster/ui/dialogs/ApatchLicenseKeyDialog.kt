package com.andorid.crackmaster.ui.dialogs

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.andorid.crackmaster.ui.theme.ActivePillText
import com.andorid.crackmaster.ui.theme.ErrorRed
import com.andorid.crackmaster.ui.theme.NeonGreen
import com.andorid.crackmaster.ui.theme.TextMuted
import com.andorid.crackmaster.ui.theme.TextPrimary
import com.andorid.crackmaster.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApatchLicenseKeyDialog(
    initialKey: String,
    onDismiss: () -> Unit,
    onSubmitKey: (String) -> Unit,
    isSubmitting: Boolean = false,
    errorMessage: String? = null,
    isVerified: Boolean = false,
    terminalLogs: List<String> = emptyList()
) {
    val context = LocalContext.current
    var keyText by remember(initialKey) { mutableStateOf(initialKey) }
    var isKeyVisible by remember { mutableStateOf(false) }

    // Auto-scroll terminal to bottom when logs update
    val listState = rememberLazyListState()
    LaunchedEffect(terminalLogs.size) {
        if (terminalLogs.isNotEmpty()) {
            listState.animateScrollToItem(terminalLogs.size - 1)
        }
    }

    BasicAlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !isSubmitting,
            dismissOnClickOutside = !isSubmitting
        )
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF14171C),
            border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.35f)),
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth(0.94f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Status icon ──────────────────────────────────────────────
                Surface(
                    shape = CircleShape,
                    color = if (isVerified) Color(0xFF1B3828) else Color(0xFF16251C),
                    border = BorderStroke(1.dp, if (isVerified) NeonGreen else NeonGreen.copy(alpha = 0.5f)),
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isVerified) Icons.Filled.CheckCircle else Icons.Filled.Key,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (isVerified) "License Authorized" else "License Required",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = when {
                        isVerified   -> "Authorized. Loading binary into memory..."
                        isSubmitting -> "Verifying with server..."
                        else         -> "Enter your CrackMaster license key to run binary into memory."
                    },
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                // ── Terminal log view ───────────────────────────────────────
                AnimatedVisibility(visible = terminalLogs.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 80.dp, max = 160.dp)
                                .background(
                                    Color(0xFF0D0F12),
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            LazyColumn(state = listState) {
                                items(terminalLogs) { line ->
                                    val lineColor = when {
                                        line.startsWith("[-]") -> ErrorRed
                                        line.startsWith("[+]") -> NeonGreen
                                        line.startsWith("[?]") -> Color(0xFFFFD54F)
                                        else                   -> TextSecondary
                                    }
                                    Text(
                                        text = line,
                                        fontSize = 10.sp,
                                        color = lineColor,
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Key input (hidden once verified) ───────────────────────
                if (!isVerified) {
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = keyText,
                        onValueChange = { keyText = it },
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
                        enabled = !isSubmitting,
                        visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = clipboard.primaryClip
                                        if (clip != null && clip.itemCount > 0) {
                                            val text = clip.getItemAt(0).text?.toString().orEmpty()
                                            if (text.isNotBlank()) keyText = text.trim()
                                        }
                                    }
                                ) {
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
                        shape = RoundedCornerShape(12.dp),
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
                                if (!isSubmitting && keyText.trim().isNotEmpty()) {
                                    onSubmitKey(keyText.trim())
                                }
                            }
                        )
                    )

                    if (!errorMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage,
                            fontSize = 12.sp,
                            color = ErrorRed,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── Buttons ─────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isSubmitting && !isVerified,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF2E3440))
                    ) {
                        Text(text = "Cancel", fontSize = 13.sp, color = TextSecondary)
                    }

                    Button(
                        onClick = {
                            if (!isVerified && keyText.trim().isNotEmpty()) {
                                onSubmitKey(keyText.trim())
                            }
                        },
                        enabled = !isSubmitting && !isVerified && keyText.trim().isNotEmpty(),
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor          = NeonGreen,
                            contentColor            = ActivePillText,
                            disabledContainerColor  = NeonGreen.copy(alpha = 0.3f),
                            disabledContentColor    = ActivePillText.copy(alpha = 0.4f)
                        )
                    ) {
                        if (isSubmitting || isVerified) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(18.dp),
                                color       = ActivePillText,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(text = "START", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
