package app.tacit.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KissComparisonBenchmark {

    @get:Rule
    val rule = MacrobenchmarkRule()

    private fun installed(pkg: String): Boolean = runCatching {
        InstrumentationRegistry.getInstrumentation().targetContext.packageManager.getPackageInfo(pkg, 0)
    }.isSuccess

    private fun coldStart(pkg: String) = rule.measureRepeated(
        packageName = pkg,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = CompilationMode.DEFAULT,
        iterations = 10,
        startupMode = StartupMode.COLD
    ) {
        pressHome()
        startActivityAndWait()
    }

    @Test
    fun tacitCold() = coldStart(Targets.TACIT)

    @Test
    fun kissCold() {
        assumeTrue("Install KISS Launcher (fr.neamar.kiss) to compare", installed(Targets.KISS))
        coldStart(Targets.KISS)
    }
}
