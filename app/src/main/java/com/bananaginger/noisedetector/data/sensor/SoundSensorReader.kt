package com.bananaginger.noisedetector.data.sensor

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlin.math.log10
import kotlin.math.sqrt

data class SoundReading(
    val estimatedSoundLevelDb: Double
)

interface SoundSensorReader {
    val isAvailable: Boolean
    fun readings(): Flow<SoundReading>
}

class AndroidSoundSensorReader(
    context: Context
) : SoundSensorReader {
    private val applicationContext = context.applicationContext

    override val isAvailable: Boolean
        get() = applicationContext.packageManager.hasSystemFeature(
            PackageManager.FEATURE_MICROPHONE
        )

    @SuppressLint("MissingPermission")
    override fun readings(): Flow<SoundReading> = flow {
        val permissionGranted = ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!permissionGranted) {
            throw SecurityException("Microphone permission is required.")
        }

        if (!isAvailable) {
            throw IllegalStateException("Microphone is unavailable.")
        }

        val minimumBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE_HZ,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        if (minimumBufferSize <= 0) {
            throw IllegalStateException("Unable to create the microphone buffer.")
        }

        val audioRecord = AudioRecord.Builder()
            .setAudioSource(MediaRecorder.AudioSource.MIC)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE_HZ)
                    .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()
            )
            .setBufferSizeInBytes(minimumBufferSize)
            .build()

        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            audioRecord.release()
            throw IllegalStateException("Unable to initialize microphone monitoring.")
        }

        val buffer = ShortArray(minimumBufferSize / BYTES_PER_SAMPLE)

        try {
            audioRecord.startRecording()

            while (currentCoroutineContext().isActive) {
                val samplesRead = audioRecord.read(buffer, 0, buffer.size)

                if (samplesRead > 0) {
                    emit(
                        SoundReading(
                            estimatedSoundLevelDb = calculateEstimatedDb(
                                samples = buffer,
                                sampleCount = samplesRead
                            )
                        )
                    )
                } else {
                    throw IllegalStateException("Unable to read microphone data.")
                }
            }
        } finally {
            runCatching {
                if (audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    audioRecord.stop()
                }
            }
            audioRecord.release()
        }
    }.flowOn(Dispatchers.IO)

    private fun calculateEstimatedDb(
        samples: ShortArray,
        sampleCount: Int
    ): Double {
        var sumOfSquares = 0.0

        for (index in 0 until sampleCount) {
            val sample = samples[index].toDouble()
            sumOfSquares += sample * sample
        }

        val rootMeanSquare = sqrt(sumOfSquares / sampleCount)
        val normalizedAmplitude = rootMeanSquare / Short.MAX_VALUE.toDouble()

        if (normalizedAmplitude <= 0.0) {
            return 0.0
        }

        return (MAX_ESTIMATED_DB + 20.0 * log10(normalizedAmplitude))
            .coerceIn(0.0, MAX_ESTIMATED_DB)
    }

    companion object {
        private const val SAMPLE_RATE_HZ = 44_100
        private const val BYTES_PER_SAMPLE = 2
        private const val MAX_ESTIMATED_DB = 90.0
    }
}
