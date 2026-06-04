package com.txwstudio.app.whatthefit.data.db

import androidx.room.TypeConverter
import com.txwstudio.app.whatthefit.domain.model.TagKind

class TagKindConverter {
    @TypeConverter
    fun fromKind(kind: TagKind): String = kind.name

    @TypeConverter
    fun toKind(value: String): TagKind = TagKind.valueOf(value)
}
