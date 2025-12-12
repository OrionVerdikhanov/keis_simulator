package com.giftfest.game.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.giftfest.game.ui.theme.*

data class GameSettings(
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val hapticEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val autoSaveEnabled: Boolean = true,
    val showTutorialHints: Boolean = true,
    val language: String = "ru"
)

@Composable
fun SettingsScreen(
    settings: GameSettings,
    onSettingsChange: (GameSettings) -> Unit,
    onResetTutorial: () -> Unit,
    onResetProgress: () -> Unit,
    onBack: () -> Unit
) {
    var showResetDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(BackgroundDark, Color(0xFF051208))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        SettingsHeader(onBack = onBack)

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SettingsSectionTitle(icon = "🔊", title = "Звук и вибрация")
            }

            item {
                SettingsToggleItem(
                    icon = Icons.Default.VolumeUp,
                    title = "Звуковые эффекты",
                    description = "Звуки соединений и действий",
                    isEnabled = settings.soundEnabled,
                    onToggle = { onSettingsChange(settings.copy(soundEnabled = it)) }
                )
            }

            item {
                SettingsToggleItem(
                    icon = Icons.Default.MusicNote,
                    title = "Музыка",
                    description = "Фоновая музыка в игре",
                    isEnabled = settings.musicEnabled,
                    onToggle = { onSettingsChange(settings.copy(musicEnabled = it)) }
                )
            }

            item {
                SettingsToggleItem(
                    icon = Icons.Default.Vibration,
                    title = "Вибрация",
                    description = "Тактильная отдача при действиях",
                    isEnabled = settings.hapticEnabled,
                    onToggle = { onSettingsChange(settings.copy(hapticEnabled = it)) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSectionTitle(icon = "⚙️", title = "Игровые настройки")
            }

            item {
                SettingsToggleItem(
                    icon = Icons.Default.Notifications,
                    title = "Уведомления",
                    description = "Напоминания об энергии и наградах",
                    isEnabled = settings.notificationsEnabled,
                    onToggle = { onSettingsChange(settings.copy(notificationsEnabled = it)) }
                )
            }

            item {
                SettingsToggleItem(
                    icon = Icons.Default.Save,
                    title = "Автосохранение",
                    description = "Автоматическое сохранение прогресса",
                    isEnabled = settings.autoSaveEnabled,
                    onToggle = { onSettingsChange(settings.copy(autoSaveEnabled = it)) }
                )
            }

            item {
                SettingsToggleItem(
                    icon = Icons.Default.Help,
                    title = "Подсказки",
                    description = "Показывать контекстные подсказки",
                    isEnabled = settings.showTutorialHints,
                    onToggle = { onSettingsChange(settings.copy(showTutorialHints = it)) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSectionTitle(icon = "📱", title = "Данные")
            }

            item {
                SettingsActionItem(
                    icon = Icons.Default.Refresh,
                    title = "Сбросить обучение",
                    description = "Показать обучение заново",
                    onClick = onResetTutorial
                )
            }

            item {
                SettingsActionItem(
                    icon = Icons.Default.DeleteForever,
                    title = "Сбросить прогресс",
                    description = "Удалить весь прогресс и начать заново",
                    onClick = { showResetDialog = true },
                    isDanger = true
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                AppInfoCard()
            }
        }
    }

    if (showResetDialog) {
        ResetProgressDialog(
            onConfirm = {
                showResetDialog = false
                onResetProgress()
            },
            onDismiss = { showResetDialog = false }
        )
    }
}

@Composable
private fun SettingsHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(40.dp)
                .background(BackgroundCard, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Назад",
                tint = TextPrimary
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = "⚙️ Настройки",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
    }
}

@Composable
private fun SettingsSectionTitle(icon: String, title: String) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, fontSize = 20.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
    }
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    description: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle(!isEnabled) }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isEnabled) AccentGold else TextMuted,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AccentGold,
                    checkedTrackColor = AccentGold.copy(alpha = 0.5f),
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = BackgroundCell
                )
            )
        }
    }
}

@Composable
private fun SettingsActionItem(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    isDanger: Boolean = false
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDanger) AccentRed else AccentBlue,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDanger) AccentRed else TextPrimary
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun AppInfoCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = BackgroundCard.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🎁",
                fontSize = 36.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Gift Fest",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = AccentGold
            )
            Text(
                text = "Версия 1.0.0",
                fontSize = 12.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Соединяйте подарки, повышайте уровень\nи собирайте коллекцию!",
                fontSize = 12.sp,
                color = TextMuted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun ResetProgressDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BackgroundCard,
        title = {
            Text(
                text = "⚠️ Сбросить прогресс?",
                fontWeight = FontWeight.Bold,
                color = AccentRed
            )
        },
        text = {
            Column {
                Text(
                    text = "Это действие нельзя отменить!",
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("• Уровень сбросится до 1", color = AccentRed)
                Text("• Все монеты и кристаллы удалятся", color = AccentRed)
                Text("• Престиж и бонусы сбросятся", color = AccentRed)
                Text("• Статистика обнулится", color = AccentRed)
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
            ) {
                Text("Сбросить всё")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = TextSecondary)
            }
        }
    )
}
