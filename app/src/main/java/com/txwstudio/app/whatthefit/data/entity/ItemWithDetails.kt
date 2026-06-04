package com.txwstudio.app.whatthefit.data.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

/** A clothing item with its assigned categories (parts) and tags (brand/color/occasion). */
data class ItemWithDetails(
    @Embedded val item: ClothingItem,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = ItemCategoryCrossRef::class,
            parentColumn = "itemId",
            entityColumn = "categoryId",
        ),
    )
    val categories: List<Category>,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = ItemTagCrossRef::class,
            parentColumn = "itemId",
            entityColumn = "tagId",
        ),
    )
    val tags: List<Tag>,
)
