package com.txwstudio.app.whatthefit.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.txwstudio.app.whatthefit.data.entity.Category
import com.txwstudio.app.whatthefit.data.entity.CategoryWithCount
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM Category ORDER BY sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<Category>>

    @Query(
        "SELECT c.*, COUNT(ref.itemId) AS itemCount FROM Category c " +
                "LEFT JOIN ItemCategoryCrossRef ref ON c.id = ref.categoryId " +
                "GROUP BY c.id ORDER BY c.sortOrder ASC, c.id ASC",
    )
    fun observeAllWithCounts(): Flow<List<CategoryWithCount>>

    @Query("SELECT * FROM Category ORDER BY sortOrder ASC, id ASC")
    suspend fun getAll(): List<Category>

    @Query("SELECT * FROM Category WHERE id = :id")
    suspend fun getById(id: Long): Category?

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM Category")
    suspend fun getMaxSortOrder(): Int

    @Insert
    suspend fun insert(category: Category): Long

    @Update
    suspend fun update(category: Category)

    @Delete
    suspend fun delete(category: Category)

    @Query("UPDATE Category SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int)

    /** Persist a drag-reorder: assign sortOrder by position in [orderedIds]. */
    @Transaction
    suspend fun applyOrder(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id -> updateSortOrder(id, index) }
    }
}
