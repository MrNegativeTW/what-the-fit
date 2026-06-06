package com.txwstudio.app.whatthefit.data.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * An [OotdRecord] with each recorded slot resolved to the live [Category] and [ClothingItem].
 * Because the links cascade-delete, [OotdSlot.category] and [OotdSlot.item] are effectively always
 * present, but are typed nullable so a stale row never crashes the read.
 */
data class OotdWithItems(
    @Embedded val record: OotdRecord,
    @Relation(
        entity = OotdItemCrossRef::class,
        parentColumn = "id",
        entityColumn = "ootdId",
    )
    val slots: List<OotdSlot>,
)

/** One slot of an OOTD: the stored link plus the live category (part) and item it points at. */
data class OotdSlot(
    @Embedded val ref: OotdItemCrossRef,
    @Relation(parentColumn = "categoryId", entityColumn = "id")
    val category: Category?,
    @Relation(parentColumn = "itemId", entityColumn = "id")
    val item: ClothingItem?,
)
