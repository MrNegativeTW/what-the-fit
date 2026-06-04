package com.txwstudio.app.whatthefit.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.txwstudio.app.whatthefit.data.entity.ItemTagCrossRef

@Dao
interface TagCrossRefDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(ref: ItemTagCrossRef)

    @Query("DELETE FROM ItemTagCrossRef WHERE itemId = :itemId")
    suspend fun clearItemTags(itemId: Long)

    /** Replace an item's tag links with exactly [tagIds]. */
    @Transaction
    suspend fun setItemTags(itemId: Long, tagIds: List<Long>) {
        clearItemTags(itemId)
        tagIds.forEach { tagId -> insert(ItemTagCrossRef(itemId = itemId, tagId = tagId)) }
    }
}
