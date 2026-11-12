package com.andorid.crackmaster.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.andorid.crackmaster.ui.theme.CardBackground

@Composable
fun APatchCard(
    modifier: Modifier = Modifier,
    border: BorderStroke? = null,
    containerColor: Color = CardBackground,
    cornerRadius: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.elevatedCardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = border,
        content = content
    )
}
