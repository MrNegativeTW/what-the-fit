package com.txwstudio.app.whatthefit.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/** Junction table for the many-to-many relationship between [ClothingItem] and [Tag]. */
@Entity(
    primaryKeys = ["itemId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = ClothingItem::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Tag::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("tagId")],
)
data class ItemTagCrossRef(
    val itemId: Long,
    val tagId: Long,
)
