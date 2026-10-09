package app.tacit.sources

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import app.tacit.Prefs
import app.tacit.core.ItemKind
import app.tacit.core.SearchItem

class SettingSource(private val context: Context, private val prefs: Prefs) {

    @Volatile
    private var cached: List<SearchItem> = emptyList()

    fun items(): List<SearchItem> = cached

    fun reload(apps: List<SearchItem>) {
        val pm = context.packageManager
        val seenActions = HashSet<String>()
        val seenComponents = HashSet<String>()
        val result = ArrayList<SearchItem>(1024)

        for (entry in SettingsCatalog.entries) {
            if (!resolves(Intent(entry.action))) continue
            seenActions.add(entry.action)
            result.add(SearchItem("setting:${entry.action}", ItemKind.SETTING, entry.label, "Settings",
                entry.synonyms, "action|${entry.action}"))
        }

        for (field in Settings::class.java.fields) {
            if (!field.name.startsWith("ACTION_") || field.type != String::class.java) continue
            val action = runCatching { field.get(null) as String }.getOrNull() ?: continue
            if (action in seenActions || action in SettingsCatalog.skipped) continue
            if (!resolves(Intent(action))) continue
            seenActions.add(action)
            val label = humanize(field.name.removePrefix("ACTION_").removeSuffix("_SETTINGS"))
            result.add(SearchItem("setting:$action", ItemKind.SETTING, label, "Settings", emptyList(), "action|$action"))
        }

        for (packageName in SETTINGS_PACKAGES) {
            val info = runCatching { pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES) }.getOrNull() ?: continue
            for (activity in info.activities.orEmpty()) {
                if (!activity.exported || !activity.enabled || activity.permission != null) continue
                val label = activity.loadLabel(pm).toString().trim()
                if (label.isEmpty() || label.equals("settings", true) || label.length > 60) continue
                val component = "${activity.packageName}/${activity.name}"
                if (!seenComponents.add(component)) continue
                if (result.any { it.label.equals(label, true) }) continue
                result.add(SearchItem("setting:c:$component", ItemKind.SETTING, label, "Settings",
                    listOf(humanize(activity.name.substringAfterLast('.').removeSuffix("Activity").removeSuffix("Settings"))),
                    "component|$component"))
            }
        }

        val shortcutIntent = Intent(Intent.ACTION_MAIN).addCategory("com.android.settings.SHORTCUT")
        for (resolve in pm.queryIntentActivities(shortcutIntent, 0)) {
            val activity = resolve.activityInfo ?: continue
            val component = "${activity.packageName}/${activity.name}"
            if (!seenComponents.add(component)) continue
            val label = resolve.loadLabel(pm).toString().trim()
            if (label.isEmpty() || result.any { it.label.equals(label, true) }) continue
            result.add(SearchItem("setting:c:$component", ItemKind.SETTING, label, "Settings", emptyList(), "component|$component"))
        }

        for (panel in SettingsCatalog.panels) {
            if (!resolves(Intent(panel.action))) continue
            result.add(SearchItem("setting:panel:${panel.action}", ItemKind.SETTING, panel.label, "Panel",
                panel.synonyms, "action|${panel.action}"))
        }

        result.add(SearchItem("toggle:flashlight", ItemKind.SETTING, "Flashlight", "Toggle",
            listOf("torch", "lamp", "light", "كشاف"), "toggle|flashlight"))

        if (prefs.settingsSearchFallback) {
            val search = Intent(SYSTEM_SETTINGS_SEARCH)
            if (resolves(search)) {
                result.add(SearchItem("setting:search", ItemKind.SETTING, "Search system settings", "Settings",
                    listOf("settings search", "find setting"), "action|$SYSTEM_SETTINGS_SEARCH"))
            }
        }

        for (app in apps) {
            if (app.kind != ItemKind.APP || app.subtitle.isNotEmpty()) continue
            val pkg = app.payload.substringBefore('/')
            result.add(SearchItem("appinfo:$pkg", ItemKind.SETTING, "${app.label} info", "App",
                listOf("${app.label} details", "${app.label} permissions", "${app.label} storage", "${app.label} battery"),
                "appinfo|$pkg"))
            result.add(SearchItem("appnotif:$pkg", ItemKind.SETTING, "${app.label} notifications", "App",
                emptyList(), "appnotif|$pkg"))
            val prefsIntent = Intent(Intent.ACTION_APPLICATION_PREFERENCES).setPackage(pkg)
            val resolved = pm.queryIntentActivities(prefsIntent, 0).firstOrNull()?.activityInfo
            if (resolved != null && resolved.exported) {
                result.add(SearchItem("appprefs:$pkg", ItemKind.SETTING, "${app.label} settings", "App",
                    listOf("${app.label} preferences", "${app.label} options"), "component|${resolved.packageName}/${resolved.name}"))
            }
        }
        cached = result
    }

    private fun resolves(intent: Intent): Boolean =
        context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null

    private fun humanize(name: String): String {
        val spaced = name.replace('_', ' ').replace(Regex("(?<=[a-z])(?=[A-Z])"), " ").lowercase().trim()
        return spaced.replaceFirstChar { it.uppercase() }
    }

    fun intentFor(payload: String): Intent? {
        val parts = payload.split('|', limit = 2)
        if (parts.size < 2) return null
        return when (parts[0]) {
            "action" -> Intent(parts[1])
            "component" -> ComponentName.unflattenFromString(parts[1])?.let { Intent().setComponent(it) }
            "appinfo" -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", parts[1], null))
            "appnotif" -> Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, parts[1])
            else -> null
        }?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    companion object {
        val SETTINGS_PACKAGES = listOf("com.android.settings")
        const val SYSTEM_SETTINGS_SEARCH = "com.android.settings.action.SETTINGS_SEARCH"
    }
}
