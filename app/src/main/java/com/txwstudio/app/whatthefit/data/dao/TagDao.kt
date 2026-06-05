package com.txwstudio.app.whatthefit.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.txwstudio.app.whatthefit.data.entity.Tag
import com.txwstudio.app.whatthefit.data.entity.TagWithCount
import com.txwstudio.app.whatthefit.domain.model.TagKind
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT * FROM Tag WHERE kind = :kind ORDER BY sortOrder ASC, id ASC")
    fun observeByKind(kind: TagKind): Flow<List<Tag>>

    @Query(
        "SELECT t.*, COUNT(ref.itemId) AS itemCount FROM Tag t " +
                "LEFT JOIN ItemTagCrossRef ref ON t.id = ref.tagId " +
                "WHERE t.kind = :kind GROUP BY t.id ORDER BY t.sortOrder ASC, t.id ASC",
    )
    fun observeByKindWithCounts(kind: TagKind): Flow<List<TagWithCount>>

    @Query("SELECT * FROM Tag WHERE id = :id")
    suspend fun getById(id: Long): Tag?

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM Tag WHERE kind = :kind")
    suspend fun getMaxSortOrder(kind: TagKind): Int

    @Insert
    suspend fun insert(tag: Tag): Long

    @Update
    suspend fun update(tag: Tag)

    @Delete
    suspend fun delete(tag: Tag)

    @Query("UPDATE Tag SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int)

    @Transaction
    suspend fun applyOrder(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id -> updateSortOrder(id, index) }
    }
}
