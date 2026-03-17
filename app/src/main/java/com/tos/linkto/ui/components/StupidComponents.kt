package com.tos.linkto.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import com.tos.linkto.MainActivity

/**
 * 带有智能无障碍点击逻辑的通用处理器
 */
fun handleAccessibleClick(
    isAccessibilityMode: Boolean,
    lastClickTime: Long,
    label: String,
    onAction: () -> Unit,
    updateLastClickTime: (Long) -> Unit
) {
    if (isAccessibilityMode) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime < 500) {
            onAction()
        } else {
            MainActivity.instance.speak(label)
        }
        updateLastClickTime(currentTime)
    } else {
        onAction()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StupidCard(
    isAccessibilityMode: Boolean,
    label: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    colors: CardColors = CardDefaults.cardColors(),
    content: @Composable ColumnScope.() -> Unit
) {
    var lastClickTime by remember { mutableLongStateOf(0L) }
    Card(
        onClick = { handleAccessibleClick(isAccessibilityMode, lastClickTime, label, onAction) { lastClickTime = it } },
        modifier = modifier,
        colors = colors,
        content = content
    )
}

@Composable
fun StupidButton(
    isAccessibilityMode: Boolean,
    label: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    shape: Shape = ButtonDefaults.shape,
    content: @Composable RowScope.() -> Unit
) {
    var lastClickTime by remember { mutableLongStateOf(0L) }
    Button(
        onClick = { handleAccessibleClick(isAccessibilityMode, lastClickTime, label, onAction) { lastClickTime = it } },
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        shape = shape,
        content = content
    )
}

@Composable
fun StupidIconButton(
    isAccessibilityMode: Boolean,
    label: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var lastClickTime by remember { mutableLongStateOf(0L) }
    IconButton(
        onClick = { handleAccessibleClick(isAccessibilityMode, lastClickTime, label, onAction) { lastClickTime = it } },
        modifier = modifier,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StupidSurface(
    isAccessibilityMode: Boolean,
    label: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surface,
    shape: Shape = MaterialTheme.shapes.medium,
    content: @Composable () -> Unit
) {
    var lastClickTime by remember { mutableLongStateOf(0L) }
    Surface(
        onClick = { handleAccessibleClick(isAccessibilityMode, lastClickTime, label, onAction) { lastClickTime = it } },
        modifier = modifier,
        color = color,
        shape = shape,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StupidFilterChip(
    isAccessibilityMode: Boolean,
    label: String,
    selected: Boolean,
    onAction: () -> Unit,
    chipLabel: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    var lastClickTime by remember { mutableLongStateOf(0L) }
    FilterChip(
        selected = selected,
        onClick = { handleAccessibleClick(isAccessibilityMode, lastClickTime, label, onAction) { lastClickTime = it } },
        label = chipLabel,
        modifier = modifier
    )
}