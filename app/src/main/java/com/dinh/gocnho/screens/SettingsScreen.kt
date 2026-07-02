package com.dinh.gocnho.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dinh.gocnho.ui.theme.AppThemeMode

@Composable
fun SettingsScreen(
    currentThemeMode: AppThemeMode,
    onThemeChange: (AppThemeMode) -> Unit
) {
    var showThemeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text(
            text = "Cài đặt chung",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Theme Selection Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showThemeDialog = true }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Giao diện",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = when (currentThemeMode) {
                        AppThemeMode.SYSTEM -> "Theo hệ thống"
                        AppThemeMode.LIGHT -> "Sáng"
                        AppThemeMode.DARK -> "Tối"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }
    }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentMode = currentThemeMode,
            onDismiss = { showThemeDialog = false },
            onThemeSelected = { newMode ->
                onThemeChange(newMode)
                showThemeDialog = false
            }
        )
    }
}

@Composable
fun ThemeSelectionDialog(
    currentMode: AppThemeMode,
    onDismiss: () -> Unit,
    onThemeSelected: (AppThemeMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Chọn giao diện", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column {
                ThemeOption(
                    text = "Theo hệ thống (Mặc định)",
                    selected = currentMode == AppThemeMode.SYSTEM,
                    onClick = { onThemeSelected(AppThemeMode.SYSTEM) }
                )
                ThemeOption(
                    text = "Sáng",
                    selected = currentMode == AppThemeMode.LIGHT,
                    onClick = { onThemeSelected(AppThemeMode.LIGHT) }
                )
                ThemeOption(
                    text = "Tối",
                    selected = currentMode == AppThemeMode.DARK,
                    onClick = { onThemeSelected(AppThemeMode.DARK) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        titleContentColor = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
fun ThemeOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary,
                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 8.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
