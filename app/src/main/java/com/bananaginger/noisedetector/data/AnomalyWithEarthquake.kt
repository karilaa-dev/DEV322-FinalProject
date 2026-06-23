package com.bananaginger.noisedetector.data

import androidx.room.Embedded
import androidx.room.Relation

data class AnomalyWithEarthquake(
    @Embedded val anomaly: AnomalyEntity,
    @Relation(
        parentColumn = "closestEarthquakeId",
        entityColumn = "id"
    )
    val earthquake: EarthquakeEntity?
)
