package com.txwstudio.app.whatthefit.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A body-part category (e.g. 帽子, 上衣). [sortOrder] drives drag-reorder display order. */
@Entity
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sortOrder: Int = 0,
)
