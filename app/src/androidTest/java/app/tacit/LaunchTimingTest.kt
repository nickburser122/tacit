package app.tacit

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.isInternal
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleCallback
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import app.tacit.fixtures.Fixtures
import app.tacit.ui.SearchActivity
import org.hamcrest.Matchers.not
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LaunchTimingTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    private fun log(message: String) {
        Log.i("LaunchTiming", message)
    }

    private fun dump(label: String) {
        Looper.getMainLooper().thread.stackTrace.take(10).forEach { log("$label main $it") }
        Thread.getAllStackTraces().entries
            .filter { it.key.name.startsWith("Instr") }
            .forEach { entry -> entry.value.take(14).forEach { log("$label ${entry.key.name} $it") } }
    }

    private fun run(label: String, stub: Int, releaseBeforeClose: Boolean) {
        Graph.prefs.store.edit().clear().commit()
        Graph.prefs.barAtBottom = false
        Graph.prefs.closeAfterAction = false
        Graph.prefs.contactsEnabled = false
        Graph.prefs.clipboardEnabled = false
        Graph.prefs.resultCount = 8
        Graph.applyPrefs()
        Graph.useItemsForTesting(Fixtures.all())
        Intents.init()
        val ok = Instrumentation.ActivityResult(Activity.RESULT_OK, null)
        if (stub == 1) intending(not(isInternal())).respondWith(ok)
        if (stub == 2) intending(hasAction(Intent.ACTION_VIEW)).respondWith(ok)
        val t0 = SystemClock.elapsedRealtime()
        val callback = ActivityLifecycleCallback { activity, stage ->
            log("$label +${SystemClock.elapsedRealtime() - t0}ms ${activity.javaClass.simpleName} $stage")
        }
        ActivityLifecycleMonitorRegistry.getInstance().addLifecycleCallback(callback)
        val scenario = ActivityScenario.launch(SearchActivity::class.java)
        instrumentation.waitForIdleSync()
        val watcher = Thread {
            try {
                Thread.sleep(8000)
                dump("$label@close+8s")
            } catch (e: InterruptedException) {
                return@Thread
            }
        }
        watcher.isDaemon = true
        val c0 = SystemClock.elapsedRealtime()
        log("$label closing at +${c0 - t0}ms")
        watcher.start()
        if (releaseBeforeClose) Intents.release()
        scenario.close()
        val closeMs = SystemClock.elapsedRealtime() - c0
        watcher.interrupt()
        if (!releaseBeforeClose) Intents.release()
        ActivityLifecycleMonitorRegistry.getInstance().removeLifecycleCallback(callback)
        Graph.useItemsForTesting(null)
        log("$label close=${closeMs}ms")
    }

    @Test
    fun control() = run("control", stub = 0, releaseBeforeClose = false)

    @Test
    fun stubExternal() = run("stubExternal", stub = 1, releaseBeforeClose = false)

    @Test
    fun stubExternalReleaseFirst() = run("stubExternalReleaseFirst", stub = 1, releaseBeforeClose = true)

    @Test
    fun stubViewOnly() = run("stubViewOnly", stub = 2, releaseBeforeClose = false)
}
