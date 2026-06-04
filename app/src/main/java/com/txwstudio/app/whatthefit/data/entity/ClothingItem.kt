package com.txwstudio.app.whatthefit.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A single piece of clothing. [seasons] is a bit flag (label/display only — doesn't affect
 * generation). Brand / color / occasion are configurable [Tag]s linked via [ItemTagCrossRef].
 * [notes] is free text. [name] is indexed for fast search across large wardrobes (>200 items).
 */
@Entity(indices = [Index(value = ["name"])])
data class ClothingItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isAvailable: Boolean = true,
    val seasons: Int = 0,
    val notes: String = "",
)
