package com.bananaginger.noisedetector.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.sqrt

data class MotionReading(
    val accelerationMagnitude: Float
)

interface MotionSensorReader {
    val isAvailable: Boolean
    fun readings(): Flow<MotionReading>
}

class AndroidMotionSensorReader(
    context: Context
) : MotionSensorReader {
    private val sensorManager =
        context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val accelerometer =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    override val isAvailable: Boolean
        get() = accelerometer != null

    override fun readings(): Flow<MotionReading> = callbackFlow {
        val sensor = accelerometer
        if (sensor == null) {
            close(IllegalStateException("Accelerometer is unavailable."))
            return@callbackFlow
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val magnitude = sqrt(x * x + y * y + z * z)
                trySend(MotionReading(accelerationMagnitude = magnitude))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        val registered = sensorManager.registerListener(
            listener,
            sensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )

        if (!registered) {
            close(IllegalStateException("Unable to start accelerometer monitoring."))
            return@callbackFlow
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}
