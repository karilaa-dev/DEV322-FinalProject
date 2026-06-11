package com.bananaginger.noisedetector

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test file that runs on an Android device or emulator.
 * Verifies app context and basic instrumentation setup.
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Get the app-under-test context and verify its packageName.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.bananaginger.noisedetector", appContext.packageName)
    }
}