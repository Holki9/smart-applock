package com.holki9.applock.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.holki9.applock.data.PreferencesManager
import com.holki9.applock.data.RelockPolicy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager.getInstance(context) }

    var isBiometricEnabled by remember { mutableStateOf(prefs.isBiometricEnabled()) }
    var isQuickLockEnabled by remember { mutableStateOf(prefs.isLauncherQuickLockEnabled()) }
    var isVibrationEnabled by remember { mutableStateOf(prefs.isVibrationEnabled()) }
    var isProtectSettings by remember { mutableStateOf(prefs.shouldProtectSettings()) }
    var currentPolicy by remember { mutableStateOf(prefs.getRelockPolicy()) }

    var showPolicyDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showRecoveryDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Section: Security
            Text(
                text = "Безопасность",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            SettingsActionItem(
                icon = Icons.Default.Pin,
                title = "Изменить PIN-код",
                subtitle = "Установить новый 4-значный пароль защиты",
                onClick = { showChangePinDialog = true }
            )

            SettingsActionItem(
                icon = Icons.Default.Security,
                title = "Секретный вопрос для сброса",
                subtitle = "Восстановление доступа при забытом пароле",
                onClick = { showRecoveryDialog = true }
            )

            SettingsToggleItem(
                icon = Icons.Default.Fingerprint,
                title = "Биометрическая разблокировка",
                subtitle = "Быстрый вход по отпечатку пальца или лицу",
                checked = isBiometricEnabled,
                onCheckedChange = {
                    isBiometricEnabled = it
                    prefs.setBiometricEnabled(it)
                }
            )

            // Section: Locking behavior
            Text(
                text = "Поведение блокировки",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            SettingsActionItem(
                icon = Icons.Default.Timer,
                title = "Политика повторной блокировки",
                subtitle = currentPolicy.title,
                onClick = { showPolicyDialog = true }
            )

            SettingsToggleItem(
                icon = Icons.Default.TouchApp,
                title = "Быстрая блокировка с иконки",
                subtitle = "Кнопка «Заблокировать» прямо при долгом нажатии на иконку в меню или на рабочем столе",
                checked = isQuickLockEnabled,
                onCheckedChange = {
                    isQuickLockEnabled = it
                    prefs.setLauncherQuickLockEnabled(it)
                }
            )

            SettingsToggleItem(
                icon = Icons.Default.Vibration,
                title = "Виброотклик",
                subtitle = "Тактильная отдача при вводе и нажатиях",
                checked = isVibrationEnabled,
                onCheckedChange = {
                    isVibrationEnabled = it
                    prefs.setVibrationEnabled(it)
                }
            )

            // Section: Safety & Recovery
            Text(
                text = "Безопасное удаление и защита",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            SettingsToggleItem(
                icon = Icons.Default.Lock,
                title = "Защита системных настроек",
                subtitle = "Если выключено, вы всегда сможете удалить приложение через настройки телефона, даже забыв пароль",
                checked = isProtectSettings,
                onCheckedChange = {
                    isProtectSettings = it
                    prefs.setProtectSettings(it)
                }
            )

            SettingsActionItem(
                icon = Icons.Default.HelpOutline,
                title = "Экстренное удаление и сброс",
                subtitle = "Инструкция: как удалить или сбросить приложение при забытом пароле",
                onClick = { showHelpDialog = true }
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Dialog: Relock Policy
    if (showPolicyDialog) {
        AlertDialog(
            onDismissRequest = { showPolicyDialog = false },
            title = { Text("Когда блокировать приложения?") },
            text = {
                Column {
                    RelockPolicy.entries.forEach { policy ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currentPolicy = policy
                                    prefs.setRelockPolicy(policy)
                                    showPolicyDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (policy == currentPolicy),
                                onClick = {
                                    currentPolicy = policy
                                    prefs.setRelockPolicy(policy)
                                    showPolicyDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(policy.title, fontWeight = FontWeight.SemiBold)
                                Text(
                                    policy.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPolicyDialog = false }) {
                    Text("Закрыть")
                }
            }
        )
    }

    // Dialog: Change PIN
    if (showChangePinDialog) {
        var newPin by remember { mutableStateOf("") }
        var pinError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            title = { Text("Новый PIN-код") },
            text = {
                Column {
                    Text("Введите новый 4-значный код:")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = {
                            if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                newPin = it
                                pinError = false
                            }
                        },
                        label = { Text("PIN-код (4 цифры)") },
                        isError = pinError,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newPin.length == 4) {
                        prefs.setPin(newPin)
                        showChangePinDialog = false
                    } else {
                        pinError = true
                    }
                }) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePinDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Dialog: Recovery Question
    if (showRecoveryDialog) {
        var newQuestion by remember { mutableStateOf(prefs.getSecurityQuestion()) }
        var newAnswer by remember { mutableStateOf("") }
        var ansError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showRecoveryDialog = false },
            title = { Text("Секретный вопрос") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newQuestion,
                        onValueChange = { newQuestion = it },
                        label = { Text("Вопрос") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newAnswer,
                        onValueChange = {
                            newAnswer = it
                            ansError = false
                        },
                        label = { Text("Ответ") },
                        isError = ansError,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newAnswer.trim().length >= 2) {
                        prefs.setRecovery(newQuestion, newAnswer)
                        showRecoveryDialog = false
                    } else {
                        ansError = true
                    }
                }) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecoveryDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Dialog: Help & Uninstall
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("Экстренный сброс и удаление") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "1. Сброс через секретный ответ:\n" +
                                "На любом экране ввода PIN нажмите «Забыли PIN?» и введите ваш секретный ответ. Вы сразу сможете задать новый пароль.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "2. Легкое удаление:\n" +
                                "По умолчанию защита «Настроек телефона» отключена. Если вы забыли PIN и секретный ответ, просто откройте:\n" +
                                "«Настройки телефона» → «Приложения» → «Smart AppLock» → «Удалить».",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("Понятно")
                }
            }
        )
    }
}

@Composable
fun SettingsActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}
