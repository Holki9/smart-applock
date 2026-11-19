package com.holki9.applock.data

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "smart_applock_prefs"

        private const val KEY_SETUP_COMPLETED = "setup_completed"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_SECURITY_QUESTION = "security_question"
        private const val KEY_SECURITY_ANSWER_HASH = "security_answer_hash"
        private const val KEY_SERVICE_ENABLED = "service_enabled"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_LAUNCHER_QUICK_LOCK = "launcher_quick_lock_enabled"
        private const val KEY_VIBRATION_ENABLED = "vibration_enabled"
        private const val KEY_PROTECT_SETTINGS = "protect_settings"
        private const val KEY_RELOCK_POLICY = "relock_policy"
        private const val KEY_LOCKED_PACKAGES = "locked_packages"

        @Volatile
        private var instance: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return instance ?: synchronized(this) {
                instance ?: PreferencesManager(context.applicationContext).also { instance = it }
            }
        }
    }

    fun isSetupCompleted(): Boolean = prefs.getBoolean(KEY_SETUP_COMPLETED, false)

    fun completeSetup() {
        prefs.edit().putBoolean(KEY_SETUP_COMPLETED, true).apply()
    }

    fun setPin(pin: String) {
        val hash = hashString(pin)
        prefs.edit().putString(KEY_PIN_HASH, hash).apply()
    }

    fun checkPin(pin: String): Boolean {
        val savedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        return savedHash == hashString(pin)
    }

    fun hasPin(): Boolean = prefs.contains(KEY_PIN_HASH)

    fun setRecovery(question: String, answer: String) {
        val normalizedAnswer = answer.trim().lowercase()
        prefs.edit()
            .putString(KEY_SECURITY_QUESTION, question)
            .putString(KEY_SECURITY_ANSWER_HASH, hashString(normalizedAnswer))
            .apply()
    }

    fun getSecurityQuestion(): String {
        return prefs.getString(KEY_SECURITY_QUESTION, "Ваш секретный вопрос") ?: "Ваш секретный вопрос"
    }

    fun checkRecoveryAnswer(answer: String): Boolean {
        val savedHash = prefs.getString(KEY_SECURITY_ANSWER_HASH, null) ?: return false
        val normalizedAnswer = answer.trim().lowercase()
        return savedHash == hashString(normalizedAnswer)
    }

    fun isServiceEnabled(): Boolean = prefs.getBoolean(KEY_SERVICE_ENABLED, true)

    fun setServiceEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SERVICE_ENABLED, enabled).apply()
    }

    fun isBiometricEnabled(): Boolean = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true)

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun isLauncherQuickLockEnabled(): Boolean = prefs.getBoolean(KEY_LAUNCHER_QUICK_LOCK, true)

    fun setLauncherQuickLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LAUNCHER_QUICK_LOCK, enabled).apply()
    }

    fun isVibrationEnabled(): Boolean = prefs.getBoolean(KEY_VIBRATION_ENABLED, true)

    fun setVibrationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIBRATION_ENABLED, enabled).apply()
    }

    fun shouldProtectSettings(): Boolean = prefs.getBoolean(KEY_PROTECT_SETTINGS, false)

    fun setProtectSettings(protect: Boolean) {
        prefs.edit().putBoolean(KEY_PROTECT_SETTINGS, protect).apply()
    }

    fun getRelockPolicy(): RelockPolicy {
        val id = prefs.getInt(KEY_RELOCK_POLICY, RelockPolicy.SCREEN_OFF.id)
        return RelockPolicy.fromId(id)
    }

    fun setRelockPolicy(policy: RelockPolicy) {
        prefs.edit().putInt(KEY_RELOCK_POLICY, policy.id).apply()
    }

    fun getLockedPackages(): Set<String> {
        return prefs.getStringSet(KEY_LOCKED_PACKAGES, emptySet()) ?: emptySet()
    }

    fun lockApp(packageName: String) {
        val set = getLockedPackages().toMutableSet()
        set.add(packageName)
        prefs.edit().putStringSet(KEY_LOCKED_PACKAGES, set).apply()
    }

    fun unlockApp(packageName: String) {
        val set = getLockedPackages().toMutableSet()
        set.remove(packageName)
        prefs.edit().putStringSet(KEY_LOCKED_PACKAGES, set).apply()
    }

    fun toggleLock(packageName: String): Boolean {
        val set = getLockedPackages().toMutableSet()
        val newState = if (set.contains(packageName)) {
            set.remove(packageName)
            false
        } else {
            set.add(packageName)
            true
        }
        prefs.edit().putStringSet(KEY_LOCKED_PACKAGES, set).apply()
        return newState
    }

    fun isAppLocked(packageName: String): Boolean {
        if (!isServiceEnabled()) return false
        // Avoid self-locking
        if (packageName == "com.holki9.applock") return false

        // Settings protection check
        if (packageName == "com.android.settings" || packageName == "com.google.android.packageinstaller") {
            if (!shouldProtectSettings()) return false
        }

        return getLockedPackages().contains(packageName)
    }

    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
