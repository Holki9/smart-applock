package com.holki9.applock.util

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import com.holki9.applock.data.AppItem
import com.holki9.applock.data.PreferencesManager

object AppHelper {

    @Volatile
    private var labelToPackageCache: Map<String, String> = emptyMap()

    @Volatile
    private var packageToLabelCache: Map<String, String> = emptyMap()

    @Volatile
    private var isCacheInitialized: Boolean = false

    /**
     * Ultra-fast, lightweight label <-> package mapping without decoding Bitmaps.
     * Takes ~10-20ms and negligible memory.
     */
    fun initPackageCache(context: Context) {
        try {
            val pm = context.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
            val labelToPkg = mutableMapOf<String, String>()
            val pkgToLabel = mutableMapOf<String, String>()

            for (resolveInfo in resolveInfos) {
                val pkg = resolveInfo.activityInfo.packageName
                if (pkg == context.packageName) continue

                val label = resolveInfo.loadLabel(pm).toString()
                labelToPkg[label.lowercase().trim()] = pkg
                pkgToLabel[pkg] = label
            }

            labelToPackageCache = labelToPkg
            packageToLabelCache = pkgToLabel
            isCacheInitialized = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getInstalledApps(context: Context): List<AppItem> {
        val pm = context.packageManager
        val prefs = PreferencesManager.getInstance(context)
        val lockedPackages = prefs.getLockedPackages()

        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
        val appList = mutableListOf<AppItem>()
        val labelToPkg = mutableMapOf<String, String>()
        val pkgToLabel = mutableMapOf<String, String>()

        val seenPackages = mutableSetOf<String>()

        for (resolveInfo in resolveInfos) {
            val pkg = resolveInfo.activityInfo.packageName
            if (pkg == context.packageName) continue
            if (seenPackages.contains(pkg)) continue
            seenPackages.add(pkg)

            val name = resolveInfo.loadLabel(pm).toString()
            val isSystem = (resolveInfo.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val icon = resolveInfo.loadIcon(pm)
            val isLocked = lockedPackages.contains(pkg)

            appList.add(AppItem(packageName = pkg, name = name, isLocked = isLocked, isSystem = isSystem, icon = icon))
            labelToPkg[name.lowercase().trim()] = pkg
            pkgToLabel[pkg] = name
        }

        labelToPackageCache = labelToPkg
        packageToLabelCache = pkgToLabel
        isCacheInitialized = true

        return appList.sortedWith(compareBy({ !it.isLocked }, { it.name.lowercase() }))
    }

    fun findPackageByLabel(rawLabel: String?): String? {
        if (rawLabel.isNullOrBlank()) return null
        val cleaned = rawLabel.trim().lowercase()

        // 1. Direct exact match
        labelToPackageCache[cleaned]?.let { return it }

        // 2. Clean Samsung notification badges or extra words like "Sound Assistant, 1 уведомление"
        val commaIndex = cleaned.indexOf(',')
        if (commaIndex > 0) {
            val beforeComma = cleaned.substring(0, commaIndex).trim()
            labelToPackageCache[beforeComma]?.let { return it }
        }

        // 3. Substring / startsWith match against cached labels
        for ((cachedLabel, pkg) in labelToPackageCache) {
            if (cachedLabel.length >= 3) {
                if (cleaned.startsWith(cachedLabel) || cleaned.contains(cachedLabel) || cachedLabel.contains(cleaned)) {
                    return pkg
                }
            }
        }

        return null
    }

    fun findLabelByPackage(context: Context, packageName: String): String {
        packageToLabelCache[packageName]?.let { return it }
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString().also {
                val updated = packageToLabelCache.toMutableMap()
                updated[packageName] = it
                packageToLabelCache = updated
            }
        } catch (_: Exception) {
            packageName
        }
    }
}
