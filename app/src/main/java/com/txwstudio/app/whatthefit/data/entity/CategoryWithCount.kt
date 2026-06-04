package com.txwstudio.app.whatthefit.data.entity

import androidx.room.Embedded

/** A category together with the number of clothing items currently linked to it. */
data class CategoryWithCount(
    @Embedded val category: Category,
    val itemCount: Int,
)
