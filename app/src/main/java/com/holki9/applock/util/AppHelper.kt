package com.holki9.applock.util

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.holki9.applock.data.AppItem
import com.holki9.applock.data.PreferencesManager

object AppHelper {

    @Volatile
    private var labelToPackageCache: Map<String, String> = emptyMap()

    @Volatile
    private var packageToLabelCache: Map<String, String> = emptyMap()

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
            // Skip own package
            if (pkg == context.packageName) continue
            if (seenPackages.contains(pkg)) continue
            seenPackages.add(pkg)

            val name = resolveInfo.loadLabel(pm).toString()
            val isSystem = (resolveInfo.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val icon = resolveInfo.loadIcon(pm)
            val isLocked = lockedPackages.contains(pkg)

            appList.add(AppItem(packageName = pkg, name = name, isLocked = isLocked, isSystem = isSystem, icon = icon))
            labelToPkg[name.lowercase()] = pkg
            pkgToLabel[pkg] = name
        }

        labelToPackageCache = labelToPkg
        packageToLabelCache = pkgToLabel

        return appList.sortedWith(compareBy({ !it.isLocked }, { it.name.lowercase() }))
    }

    fun findPackageByLabel(label: String): String? {
        val key = label.trim().lowercase()
        return labelToPackageCache[key]
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
        } catch (e: Exception) {
            packageName
        }
    }
}
