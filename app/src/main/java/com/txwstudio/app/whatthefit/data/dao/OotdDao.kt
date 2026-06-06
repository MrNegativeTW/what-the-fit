package com.txwstudio.app.whatthefit.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.txwstudio.app.whatthefit.data.entity.OotdItemCrossRef
import com.txwstudio.app.whatthefit.data.entity.OotdRecord
import com.txwstudio.app.whatthefit.data.entity.OotdWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface OotdDao {
    /** All records with their resolved slots, newest day first. */
    @Transaction
    @Query("SELECT * FROM OotdRecord ORDER BY epochDay DESC, id DESC")
    fun observeAll(): Flow<List<OotdWithItems>>

    @Transaction
    @Query("SELECT * FROM OotdRecord WHERE id = :id")
    suspend fun getById(id: Long): OotdWithItems?

    @Insert
    suspend fun insert(record: OotdRecord): Long

    @Update
    suspend fun update(record: OotdRecord)

    @Delete
    suspend fun delete(record: OotdRecord)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRef(ref: OotdItemCrossRef)

    @Query("DELETE FROM OotdItemCrossRef WHERE ootdId = :ootdId")
    suspend fun clearRefs(ootdId: Long)

    /** Replace a record's slots with exactly [slots] (each pair is categoryId to itemId). */
    @Transaction
    suspend fun setSlots(ootdId: Long, slots: List<Pair<Long, Long>>) {
        clearRefs(ootdId)
        slots.forEach { (categoryId, itemId) ->
            insertRef(OotdItemCrossRef(ootdId = ootdId, categoryId = categoryId, itemId = itemId))
        }
    }
}
