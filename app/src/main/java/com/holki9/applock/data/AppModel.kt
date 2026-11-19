package com.holki9.applock.data

import android.graphics.drawable.Drawable

data class AppItem(
    val packageName: String,
    val name: String,
    val isLocked: Boolean,
    val isSystem: Boolean,
    val icon: Drawable? = null
)

enum class RelockPolicy(val id: Int, val title: String, val description: String) {
    IMMEDIATELY(0, "Сразу при выходе", "Блокировать при каждом закрытии или переключении приложения"),
    SCREEN_OFF(1, "При выключении экрана", "Остается разблокированным, пока экран включен"),
    TIMEOUT_1_MIN(2, "Через 1 минуту", "Повторная блокировка через 1 минуту после выхода"),
    TIMEOUT_5_MIN(3, "Через 5 минут", "Повторная блокировка через 5 минут после выхода");

    companion object {
        fun fromId(id: Int): RelockPolicy {
            return entries.find { it.id == id } ?: SCREEN_OFF
        }
    }
}
