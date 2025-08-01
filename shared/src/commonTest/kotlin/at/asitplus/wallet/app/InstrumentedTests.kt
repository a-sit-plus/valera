package at.asitplus.wallet.app

import io.kotest.common.KotestInternal
import io.kotest.engine.TestEngineLauncher
import io.kotest.engine.listener.CollectingTestEngineListener
import io.kotest.engine.test.TestResult
import kotlin.test.Test
import kotlin.test.assertFalse

class InstrumentedTests {

    @OptIn(KotestInternal::class)
    @Test
    fun runInstrumentedTests() {

        val listener = CollectingTestEngineListener()
        TestEngineLauncher(listener).withClasses(InstrumentedTestsSuite::class).launch()
        listener.tests.map { entry ->
            {
                val testCase = entry.key
                val descriptor = testCase.descriptor.ids().joinToString(" > ") {
                    it.value
                }
                val cause = when (val value = entry.value) {
                    is TestResult.Error -> value.cause
                    is TestResult.Failure -> value.cause
                    else -> null
                }
                assertFalse(
                    entry.value.isErrorOrFailure, """$descriptor
                        |${cause?.stackTraceToString()}""".trimMargin()
                )
            }
        }.let {
            assertAll(it)
        }
        println("Total ${listener.tests.size}, Failure ${listener.tests.count { it.value.isErrorOrFailure }}")

    }

    private fun assertAll(assertions: List<() -> Unit>) {
        val failures = mutableListOf<Throwable>()

        for (assertion in assertions) {
            try {
                assertion()
            } catch (e: Throwable) {
                failures.add(e)
            }
        }

        if (failures.isNotEmpty()) {
            throw AssertionError("Multiple assertions failed:\n" + failures.joinToString("\n") {
                it.message ?: "Unknown error"
            })
        }
    }
}



