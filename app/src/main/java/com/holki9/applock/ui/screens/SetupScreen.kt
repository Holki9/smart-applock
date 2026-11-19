package com.holki9.applock.ui.screens

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.holki9.applock.data.PreferencesManager
import com.holki9.applock.ui.components.PinDotsIndicator
import com.holki9.applock.ui.components.PinKeypad
import com.holki9.applock.ui.theme.AccentEmerald
import com.holki9.applock.util.PermissionHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    onSetupFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager.getInstance(context) }

    var step by remember { mutableIntStateOf(1) }
    var firstPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isPinError by remember { mutableStateOf(false) }

    // Recovery step states
    val standardQuestions = listOf(
        "Ваш любимый город?",
        "Кличка вашего первого питомца?",
        "Номер или название вашей школы?",
        "Ваша любимая книга или фильм?",
        "Марка вашего первого автомобиля?"
    )
    var selectedQuestion by remember { mutableStateOf(standardQuestions[0]) }
    var questionExpanded by remember { mutableStateOf(false) }
    var recoveryAnswer by remember { mutableStateOf("") }
    var recoveryError by remember { mutableStateOf(false) }

    // Permissions check
    val hasOverlay = PermissionHelper.hasOverlayPermission(context)
    val hasAccessibility = PermissionHelper.isAccessibilityServiceEnabled(context)
    val hasUsage = PermissionHelper.hasUsageStatsPermission(context)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Icon
        Spacer(modifier = Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedContent(targetState = step, label = "stepTransition") { currentStep ->
            when (currentStep) {
                1 -> {
                    // Create PIN
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Создайте мастер PIN-код",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Этот PIN-код будет использоваться для разблокировки приложений и доступа к настройкам",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(32.dp))
                        PinDotsIndicator(
                            pinLength = 4,
                            enteredLength = firstPin.length,
                            isError = false
                        )

                        Spacer(modifier = Modifier.height(40.dp))
                        PinKeypad(
                            onDigitClick = { digit ->
                                if (firstPin.length < 4) {
                                    firstPin += digit
                                    if (firstPin.length == 4) {
                                        step = 2
                                    }
                                }
                            },
                            onDeleteClick = {
                                if (firstPin.isNotEmpty()) {
                                    firstPin = firstPin.dropLast(1)
                                }
                            }
                        )
                    }
                }

                2 -> {
                    // Confirm PIN
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Повторите PIN-код",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isPinError) "PIN-коды не совпадают. Попробуйте снова." else "Для подтверждения введите PIN-код еще раз",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isPinError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(32.dp))
                        PinDotsIndicator(
                            pinLength = 4,
                            enteredLength = confirmPin.length,
                            isError = isPinError
                        )

                        Spacer(modifier = Modifier.height(40.dp))
                        PinKeypad(
                            onDigitClick = { digit ->
                                if (confirmPin.length < 4) {
                                    confirmPin += digit
                                    if (confirmPin.length == 4) {
                                        if (confirmPin == firstPin) {
                                            prefs.setPin(firstPin)
                                            step = 3
                                        } else {
                                            isPinError = true
                                            confirmPin = ""
                                        }
                                    }
                                }
                            },
                            onDeleteClick = {
                                isPinError = false
                                if (confirmPin.isNotEmpty()) {
                                    confirmPin = confirmPin.dropLast(1)
                                }
                            }
                        )
                    }
                }

                3 -> {
                    // Recovery Setup
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Защита от потери PIN",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Укажите секретный вопрос. Если вы забудете PIN, вы сможете мгновенно сбросить его или удалить приложение без блокировки смартфона.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { questionExpanded = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "Секретный вопрос",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        selectedQuestion,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Text("▼", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        if (questionExpanded) {
                            AlertDialog(
                                onDismissRequest = { questionExpanded = false },
                                title = { Text("Выберите вопрос") },
                                text = {
                                    Column {
                                        standardQuestions.forEach { question ->
                                            Text(
                                                text = question,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        selectedQuestion = question
                                                        questionExpanded = false
                                                    }
                                                    .padding(vertical = 12.dp),
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                        }
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = { questionExpanded = false }) {
                                        Text("Закрыть")
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = recoveryAnswer,
                            onValueChange = {
                                recoveryAnswer = it
                                recoveryError = false
                            },
                            label = { Text("Ваш секретный ответ") },
                            isError = recoveryError,
                            supportingText = {
                                if (recoveryError) {
                                    Text("Пожалуйста, введите ответ (минимум 2 символа)")
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = {
                                if (recoveryAnswer.trim().length >= 2) {
                                    prefs.setRecovery(selectedQuestion, recoveryAnswer)
                                    step = 4
                                } else {
                                    recoveryError = true
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Продолжить", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                4 -> {
                    // Permissions Step
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Необходимые разрешения",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Для работы блокировки и умного контекстного меню Android требует предоставить системные разрешения:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Permission item: Overlay
                        PermissionItem(
                            title = "Отображение поверх других окон",
                            desc = "Требуется для показа экрана ввода PIN-кода и всплывающей кнопки быстрой блокировки",
                            isGranted = hasOverlay,
                            onGrantClick = { PermissionHelper.requestOverlayPermission(context) }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Permission item: Accessibility
                        PermissionItem(
                            title = "Специальные возможности",
                            desc = "Ключевая служба для обнаружения открытия приложений и долгого нажатия на иконку в лаунчере",
                            isGranted = hasAccessibility,
                            onGrantClick = { PermissionHelper.openAccessibilitySettings(context) }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Permission item: Usage Access
                        PermissionItem(
                            title = "Доступ к данным об использовании",
                            desc = "Резервный механизм контроля активных приложений",
                            isGranted = hasUsage,
                            onGrantClick = { PermissionHelper.openUsageStatsSettings(context) }
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        Button(
                            onClick = {
                                prefs.completeSetup()
                                onSetupFinished()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Завершить настройку", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionItem(
    title: String,
    desc: String,
    isGranted: Boolean,
    onGrantClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isGranted) AccentEmerald.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = if (isGranted) AccentEmerald else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (!isGranted) {
                Button(
                    onClick = onGrantClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Вкл", fontSize = 13.sp)
                }
            }
        }
    }
}
