package app.tacit.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartupBenchmark {

    @get:Rule
    val rule = MacrobenchmarkRule()

    private fun startup(mode: StartupMode, compilation: CompilationMode) = rule.measureRepeated(
        packageName = Targets.TACIT,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = compilation,
        iterations = 10,
        startupMode = mode
    ) {
        pressHome()
        startActivityAndWait()
        waitForSearchField()
    }

    @Test
    fun coldNoProfile() = startup(StartupMode.COLD, CompilationMode.None())

    @Test
    fun coldWithProfile() = startup(StartupMode.COLD, CompilationMode.Partial())

    @Test
    fun warm() = startup(StartupMode.WARM, CompilationMode.Partial())

    @Test
    fun hot() = startup(StartupMode.HOT, CompilationMode.Partial())
}
