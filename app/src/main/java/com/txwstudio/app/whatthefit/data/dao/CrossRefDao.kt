package com.txwstudio.app.whatthefit.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.txwstudio.app.whatthefit.data.entity.ItemCategoryCrossRef

@Dao
interface CrossRefDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(ref: ItemCategoryCrossRef)

    @Query("DELETE FROM ItemCategoryCrossRef WHERE itemId = :itemId")
    suspend fun clearItemRefs(itemId: Long)

    /** Replace an item's category links with exactly [categoryIds]. */
    @Transaction
    suspend fun setItemCategories(itemId: Long, categoryIds: List<Long>) {
        clearItemRefs(itemId)
        categoryIds.forEach { categoryId ->
            insert(ItemCategoryCrossRef(itemId = itemId, categoryId = categoryId))
        }
    }
}
