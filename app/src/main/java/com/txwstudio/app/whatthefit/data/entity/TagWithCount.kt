package com.txwstudio.app.whatthefit.data.entity

import androidx.room.Embedded

/** A tag together with the number of clothing items currently linked to it. */
data class TagWithCount(
    @Embedded val tag: Tag,
    val itemCount: Int,
)
