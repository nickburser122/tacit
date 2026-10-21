package app.tacit

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.ScrollView
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tacit.ui.SettingsActivity
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DiagScrollTest {

    private fun views(root: View, out: MutableList<View> = ArrayList()): List<View> {
        out.add(root)
        if (root is ViewGroup) for (i in 0 until root.childCount) views(root.getChildAt(i), out)
        return out
    }

    private fun state(target: View): String {
        val visible = Rect()
        val has = target.getGlobalVisibleRect(visible)
        val loc = IntArray(2)
        target.getLocationOnScreen(loc)
        val total = target.width * target.height
        val shown = if (has) visible.width() * visible.height() else 0
        return "has=$has vis=$visible loc=${loc[0]},${loc[1]} size=${target.width}x${target.height} pct=${if (total == 0) -1 else shown * 100 / total} shown=${target.isShown}"
    }

    @Test
    fun describeTheImportRow() {
        var report = "none"
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            Idle.await()
            scenario.onActivity { activity ->
                val all = views(activity.window.decorView)
                val scroll = all.filterIsInstance<ScrollView>().first()
                val target = all.filterIsInstance<TextView>().first { it.text.toString() == activity.getString(R.string.import_all) }
                val insets = activity.window.decorView.rootWindowInsets?.getInsets(WindowInsets.Type.systemBars())
                val metrics = activity.resources.displayMetrics
                val sloc = IntArray(2)
                scroll.getLocationOnScreen(sloc)
                val head = "sdk=${android.os.Build.VERSION.SDK_INT} insets=$insets display=${metrics.widthPixels}x${metrics.heightPixels} " +
                    "scroll=${scroll.width}x${scroll.height}@${sloc[0]},${sloc[1]} y=${scroll.scrollY} child=${scroll.getChildAt(0).height} " +
                    "pad=${scroll.paddingTop}/${scroll.paddingBottom} clip=${scroll.clipToPadding} fits=${scroll.fitsSystemWindows}"
                val before = state(target)
                val asked = target.requestRectangleOnScreen(Rect(0, 0, target.width, target.height), true)
                val afterRequest = state(target)
                val afterY = scroll.scrollY
                scroll.fullScroll(View.FOCUS_DOWN)
                val afterFull = state(target)
                report = "$head | before $before | request=$asked y=$afterY $afterRequest | full y=${scroll.scrollY} $afterFull"
            }
        }
        fail(report)
    }
}
