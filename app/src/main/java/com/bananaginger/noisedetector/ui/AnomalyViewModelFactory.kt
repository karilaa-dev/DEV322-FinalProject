package com.bananaginger.noisedetector.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bananaginger.noisedetector.data.location.LocationProvider
import com.bananaginger.noisedetector.data.location.LocationSelectionStore
import com.bananaginger.noisedetector.data.repository.AnomalyRepository
import com.bananaginger.noisedetector.data.settings.DetectionSettingsStore
import com.bananaginger.noisedetector.data.sensor.MotionSensorReader
import com.bananaginger.noisedetector.data.sensor.SoundSensorReader

// BananaGinger/Kyryl: Factory injects repository without adding a dependency injection framework.
class AnomalyViewModelFactory(
    private val repository: AnomalyRepository,
    private val motionSensorReader: MotionSensorReader,
    private val soundSensorReader: SoundSensorReader,
    private val locationProvider: LocationProvider,
    private val locationSelectionStore: LocationSelectionStore,
    private val detectionSettingsStore: DetectionSettingsStore
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AnomalyViewModel::class.java)) {
            return AnomalyViewModel(
                repository = repository,
                motionSensorReader = motionSensorReader,
                soundSensorReader = soundSensorReader,
                locationProvider = locationProvider,
                locationSelectionStore = locationSelectionStore,
                detectionSettingsStore = detectionSettingsStore
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
