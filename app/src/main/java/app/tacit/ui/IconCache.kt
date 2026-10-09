package app.tacit.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.LruCache
import app.tacit.Graph
import app.tacit.Prefs

class IconCache(private val context: Context, private val prefs: Prefs) {

    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val size = Ui.dp(context, 32)
    private val memory = object : LruCache<String, Bitmap>(8 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    fun cached(key: String): Bitmap? = memory.get(key + mode())

    fun load(key: String, payload: String, foreground: Int, done: (Bitmap?) -> Unit) {
        if (prefs.iconMode == "none") return done(null)
        val cacheKey = key + mode()
        memory.get(cacheKey)?.let { return done(it) }
        Graph.worker.post {
            val bitmap = runCatching { render(payload, foreground) }.getOrNull()
            if (bitmap != null) memory.put(cacheKey, bitmap)
            Graph.main.post { done(bitmap) }
        }
    }

    fun clear() = memory.evictAll()

    private fun mode(): String = prefs.iconMode + Ui.isDark(context)

    private fun render(payload: String, foreground: Int): Bitmap? {
        val parts = payload.split('|')
        val component = ComponentName.unflattenFromString(parts[0]) ?: return null
        val user = Graph.apps.userFor(parts.getOrNull(1)?.toLongOrNull())
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(component)
        val info = launcherApps.resolveActivity(intent, user) ?: return null
        var drawable: Drawable = info.getBadgedIcon(0)
        val mono = prefs.iconMode == "mono"
        if (mono && Build.VERSION.SDK_INT >= 33) {
            val raw = info.getIcon(0)
            val monochrome = (raw as? AdaptiveIconDrawable)?.monochrome
            if (monochrome != null) {
                val mutated = monochrome.mutate()
                mutated.setTint(foreground)
                drawable = mutated
                return draw(drawable, scale = 1.5f)
            }
        }
        if (mono) {
            drawable = drawable.mutate()
            drawable.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
        }
        return draw(drawable, 1f)
    }

    private fun draw(drawable: Drawable, scale: Float): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val inset = ((size * scale - size) / 2).toInt()
        drawable.setBounds(-inset, -inset, size + inset, size + inset)
        drawable.draw(canvas)
        return bitmap
    }
}
