package com.txwstudio.app.whatthefit.data.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
import com.txwstudio.app.whatthefit.data.entity.Category
import com.txwstudio.app.whatthefit.data.entity.ClothingItem
import com.txwstudio.app.whatthefit.data.entity.ItemCategoryCrossRef
import com.txwstudio.app.whatthefit.data.entity.ItemTagCrossRef
import com.txwstudio.app.whatthefit.data.entity.ItemWithDetails
import com.txwstudio.app.whatthefit.data.entity.Tag
import kotlinx.coroutines.flow.Flow

@Dao
interface ClothingItemDao {
    /** Real-time, paginated search over name + notes. An empty [query] matches all. */
    @Transaction
    @Query(
        "SELECT * FROM ClothingItem WHERE " +
                "name LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%' " +
                "ORDER BY name ASC, id ASC",
    )
    fun pagingSearch(query: String): PagingSource<Int, ItemWithDetails>

    /**
     * Faceted search: name/notes text plus AND'd `EXISTS` clauses over the cross-ref tables.
     * The SQL is built dynamically in the repository (one clause per active dimension), so this
     * takes a raw query. [observedEntities] lists every table the SQL can touch — required for
     * Paging to invalidate when items, their links, or tag/category names change.
     */
    @Transaction
    @RawQuery(
        observedEntities = [
            ClothingItem::class,
            Category::class,
            Tag::class,
            ItemCategoryCrossRef::class,
            ItemTagCrossRef::class,
        ],
    )
    fun pagingSearchFiltered(query: SupportSQLiteQuery): PagingSource<Int, ItemWithDetails>

    /** Available (not in laundry) items linked to [categoryId] — the core generation pool. */
    @Query(
        "SELECT ci.* FROM ClothingItem ci " +
                "INNER JOIN ItemCategoryCrossRef ref ON ci.id = ref.itemId " +
                "WHERE ref.categoryId = :categoryId AND ci.isAvailable = 1",
    )
    suspend fun getAvailableItemsByCategory(categoryId: Long): List<ClothingItem>

    @Transaction
    @Query("SELECT * FROM ClothingItem WHERE id = :id")
    suspend fun getItemWithDetails(id: Long): ItemWithDetails?

    @Query("SELECT * FROM ClothingItem WHERE id = :id")
    suspend fun getById(id: Long): ClothingItem?

    @Insert
    suspend fun insert(item: ClothingItem): Long

    @Update
    suspend fun update(item: ClothingItem)

    @Delete
    suspend fun delete(item: ClothingItem)

    @Query("UPDATE ClothingItem SET isAvailable = :available WHERE id = :id")
    suspend fun setAvailable(id: Long, available: Boolean)

    @Query("SELECT COUNT(*) FROM ClothingItem")
    fun observeCount(): Flow<Int>
}
