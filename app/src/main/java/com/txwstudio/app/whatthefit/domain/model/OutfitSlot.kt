package com.txwstudio.app.whatthefit.domain.model

import com.txwstudio.app.whatthefit.data.entity.Category
import com.txwstudio.app.whatthefit.data.entity.ClothingItem

/** One row of a generated outfit: a category and the item picked for it ([item] == null → 無可用衣物). */
data class OutfitSlot(
    val category: Category,
    val item: ClothingItem?,
)
