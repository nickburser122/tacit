package app.tacit

import android.content.Context
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EntryPointsUiTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = instrumentation.targetContext
    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        device = UiDevice.getInstance(instrumentation)
        device.pressHome()
    }

    private fun waitForSearch() =
        device.wait(Until.findObject(By.res(context.packageName, "search_input")), 5_000)

    @Test
    fun launcherIconOpensSearch() {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        assertNotNull(waitForSearch())
    }

    @Test
    fun assistIntentOpensSearch() {
        context.startActivity(Intent(Intent.ACTION_ASSIST).setPackage(context.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        assertNotNull(waitForSearch())
    }

    @Test
    fun keyboardIsShownOnOpen() {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        assertNotNull(waitForSearch())
        device.waitForIdle()
        val output = device.executeShellCommand("dumpsys input_method")
        assertTrue("IME not shown", output.contains("mInputShown=true") || output.contains("isInputViewShown=true"))
    }

    @Test
    fun typingThenHomeClearsQuery() {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        val field = waitForSearch()!!
        field.text = "abc"
        context.startActivity(intent)
        device.waitForIdle()
        val again = waitForSearch()!!
        assertTrue(again.text.isNullOrEmpty() || again.text == context.getString(R.string.search_hint))
    }
}
