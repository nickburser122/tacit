package app.tacit

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.tacit.clip.ClipboardStore
import app.tacit.data.AliasStore
import app.tacit.sources.SettingSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlatformContractTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val pm = context.packageManager

    @Test
    fun noInternetPermissionInInstalledApp() {
        val info = pm.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        val requested = info.requestedPermissions.orEmpty().toList()
        assertFalse(requested.contains(Manifest.permission.INTERNET))
        assertFalse(requested.contains(Manifest.permission.ACCESS_NETWORK_STATE))
    }

    @Test
    fun homeIntentResolvesToTacit() {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setPackage(context.packageName)
        assertNotNull(pm.resolveActivity(intent, 0))
    }

    @Test
    fun assistIntentResolvesToTacit() {
        val intent = Intent(Intent.ACTION_ASSIST).setPackage(context.packageName)
        assertNotNull(pm.resolveActivity(intent, 0))
    }

    @Test
    fun tileServiceDeclared() {
        val info = pm.getServiceInfo(ComponentName(context, "app.tacit.system.TacitTile"), 0)
        assertEquals("android.permission.BIND_QUICK_SETTINGS_TILE", info.permission)
    }

    @Test
    fun widgetDeclared() {
        val widgets = android.appwidget.AppWidgetManager.getInstance(context).installedProviders
        assertTrue(widgets.any { it.provider.packageName == context.packageName })
    }

    @Test
    fun voiceInteractionServiceDeclared() {
        val intent = Intent("android.service.voice.VoiceInteractionService").setPackage(context.packageName)
        assertTrue(pm.queryIntentServices(intent, PackageManager.GET_META_DATA).isNotEmpty())
    }

    @Test
    fun noAutomaticInitializerProviders() {
        val info = pm.getPackageInfo(context.packageName, PackageManager.GET_PROVIDERS)
        val names = info.providers.orEmpty().map { it.name }
        assertFalse(names.any { it.contains("InitializationProvider") || it.contains("WorkManagerInitializer") || it.contains("EmojiCompat") })
    }

    @Test
    fun backupDisabled() {
        val flags = pm.getApplicationInfo(context.packageName, 0).flags
        assertTrue(flags and android.content.pm.ApplicationInfo.FLAG_ALLOW_BACKUP == 0)
    }

    @Test
    fun settingsDiscoveryFindsCommonScreens() {
        val source = SettingSource(context, Graph.prefs)
        source.reload(emptyList())
        val labels = source.items().map { it.label }
        assertTrue(labels.contains("Wi-Fi"))
        assertTrue(labels.contains("Bluetooth"))
        assertTrue(labels.contains("Display"))
        assertTrue(source.items().map { it.key }.toSet().size == source.items().size)
        println("TACIT_SETTINGS_COVERAGE count=${labels.size}")
    }

    @Test
    fun settingsIntentsResolve() {
        val source = SettingSource(context, Graph.prefs)
        source.reload(emptyList())
        val unresolved = source.items().filter { it.payload.startsWith("action|") || it.payload.startsWith("component|") }
            .mapNotNull { item -> source.intentFor(item.payload)?.takeIf { pm.resolveActivity(it, 0) == null }?.let { item.label } }
        assertTrue("Unresolvable settings rows: $unresolved", unresolved.isEmpty())
    }

    @Test
    fun aliasStoreRoundTrip() {
        val store = AliasStore(context)
        store.set("test:item", listOf("one", "two"))
        assertEquals(listOf("one", "two"), AliasStore(context).get("test:item"))
        store.set("test:item", emptyList())
    }

    @Test
    fun clipboardEncryptedAtRest() {
        Graph.prefs.clipboardEnabled = true
        Graph.prefs.sensitivePolicy = "skip"
        val store = ClipboardStore(context, Graph.prefs)
        store.clear()
        assertTrue(store.add("plain secret marker 123"))
        val bytes = java.io.File(context.filesDir, "clips.bin").readBytes()
        assertFalse(String(bytes, Charsets.ISO_8859_1).contains("plain secret marker"))
        assertEquals("plain secret marker 123", ClipboardStore(context, Graph.prefs).all().first().text)
        assertFalse(store.add("4111 1111 1111 1111"))
        store.clear()
        Graph.prefs.clipboardEnabled = false
    }

    @Test
    fun settingsDefaultAppsResolves() {
        assertNotNull(pm.resolveActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS), 0))
    }
}
