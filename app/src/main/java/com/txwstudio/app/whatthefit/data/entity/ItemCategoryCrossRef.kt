package com.txwstudio.app.whatthefit.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Junction table backing the many-to-many relationship between [ClothingItem] and [Category].
 * CASCADE delete on both sides keeps the table clean when either side is removed; deleting a
 * category therefore only unlinks its items, never deletes the items themselves.
 */
@Entity(
    primaryKeys = ["itemId", "categoryId"],
    foreignKeys = [
        ForeignKey(
            entity = ClothingItem::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("categoryId")],
)
data class ItemCategoryCrossRef(
    val itemId: Long,
    val categoryId: Long,
)
