package com.txwstudio.app.whatthefit.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.txwstudio.app.whatthefit.domain.model.TagKind

/**
 * A user-configurable label (brand / color / occasion). Like a [Category] but discriminated by
 * [kind]. [sortOrder] drives drag-reorder. [swatchArgb] is an optional color dot (seeded for the
 * default colors; null for user-added tags and non-color kinds).
 */
@Entity(indices = [Index("kind")])
data class Tag(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: TagKind,
    val name: String,
    val sortOrder: Int = 0,
    val swatchArgb: Long? = null,
)
