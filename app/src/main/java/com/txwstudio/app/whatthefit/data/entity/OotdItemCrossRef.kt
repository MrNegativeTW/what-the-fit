package com.txwstudio.app.whatthefit.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Links an [OotdRecord] to the [ClothingItem] worn for a given [Category] part. One item per part
 * per record (the primary key is `ootdId` + `categoryId`). CASCADE on every side: deleting the
 * record, the category, or the item removes the link, so an OOTD reflects the live wardrobe.
 */
@Entity(
    primaryKeys = ["ootdId", "categoryId"],
    foreignKeys = [
        ForeignKey(
            entity = OotdRecord::class,
            parentColumns = ["id"],
            childColumns = ["ootdId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ClothingItem::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("categoryId"), Index("itemId")],
)
data class OotdItemCrossRef(
    val ootdId: Long,
    val categoryId: Long,
    val itemId: Long,
)
