package com.txwstudio.app.whatthefit.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One recorded outfit of the day. [epochDay] is `LocalDate.toEpochDay()` so records sort and group
 * by day without a time component. [photoPath] points at an internal file under `filesDir/ootd`, or
 * null when no photo was attached. The worn items are linked through [OotdItemCrossRef].
 */
@Entity
data class OotdRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val notes: String = "",
    val photoPath: String? = null,
)
