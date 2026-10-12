package app.tacit

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.tacit.fixtures.Fixtures
import app.tacit.ui.SearchActivity
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class LaunchTimingTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    private fun log(message: String) {
        Log.i("LaunchTiming", message)
    }

    private fun dumpMain(label: String) {
        val main = Looper.getMainLooper()
        main.thread.stackTrace.take(12).forEach { log("$label stack $it") }
        val lines = StringBuilder()
        main.dump({ lines.append(it).append('\n') }, "")
        lines.lines().take(14).forEach { log("$label queue ${it.take(200)}") }
    }

    private fun latencyMs(): Long {
        val latch = CountDownLatch(1)
        val start = SystemClock.elapsedRealtime()
        var done = 0L
        Handler(Looper.getMainLooper()).post {
            done = SystemClock.elapsedRealtime()
            latch.countDown()
        }
        latch.await(10, TimeUnit.SECONDS)
        return if (done == 0L) -1L else done - start
    }

    private fun measure(label: String, intents: Boolean, items: Boolean, type: Boolean) {
        Graph.prefs.store.edit().clear().commit()
        Graph.prefs.barAtBottom = false
        Graph.prefs.closeAfterAction = false
        Graph.prefs.contactsEnabled = false
        Graph.prefs.clipboardEnabled = false
        Graph.prefs.resultCount = 8
        Graph.applyPrefs()
        if (items) Graph.useItemsForTesting(Fixtures.all())
        if (intents) Intents.init()
        val watcher = Thread {
            try {
                Thread.sleep(3000)
                dumpMain("$label@3s")
                Thread.sleep(17000)
                dumpMain("$label@20s")
            } catch (e: InterruptedException) {
                return@Thread
            }
        }
        watcher.isDaemon = true
        watcher.start()
        val t0 = SystemClock.elapsedRealtime()
        val scenario = ActivityScenario.launch(SearchActivity::class.java)
        val t1 = SystemClock.elapsedRealtime()
        instrumentation.waitForIdleSync()
        val t2 = SystemClock.elapsedRealtime()
        if (type) {
            onView(withId(R.id.search_input)).perform(replaceText("2+2"))
            instrumentation.waitForIdleSync()
        }
        val t3 = SystemClock.elapsedRealtime()
        val latency = latencyMs()
        scenario.close()
        val t4 = SystemClock.elapsedRealtime()
        watcher.interrupt()
        if (intents) Intents.release()
        if (items) Graph.useItemsForTesting(null)
        log("$label launch=${t1 - t0}ms idle=${t2 - t1}ms type=${t3 - t2}ms mainLatency=${latency}ms close=${t4 - t3}ms")
    }

    @Test
    fun plain() = measure("plain", intents = false, items = false, type = false)

    @Test
    fun intentsOnly() = measure("intentsOnly", intents = true, items = false, type = false)

    @Test
    fun itemsOnly() = measure("itemsOnly", intents = false, items = true, type = false)

    @Test
    fun both() = measure("both", intents = true, items = true, type = false)

    @Test
    fun bothAndType() = measure("bothAndType", intents = true, items = true, type = true)
}
