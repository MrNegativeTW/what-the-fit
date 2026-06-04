package com.txwstudio.app.whatthefit.data.repository

import androidx.paging.PagingSource
import androidx.room.withTransaction
import androidx.sqlite.db.SimpleSQLiteQuery
import com.txwstudio.app.whatthefit.data.dao.CategoryDao
import com.txwstudio.app.whatthefit.data.dao.ClothingItemDao
import com.txwstudio.app.whatthefit.data.dao.CrossRefDao
import com.txwstudio.app.whatthefit.data.dao.TagCrossRefDao
import com.txwstudio.app.whatthefit.data.dao.TagDao
import com.txwstudio.app.whatthefit.data.db.WtfDatabase
import com.txwstudio.app.whatthefit.data.entity.Category
import com.txwstudio.app.whatthefit.data.entity.CategoryWithCount
import com.txwstudio.app.whatthefit.data.entity.ClothingItem
import com.txwstudio.app.whatthefit.data.entity.ItemWithDetails
import com.txwstudio.app.whatthefit.data.entity.Tag
import com.txwstudio.app.whatthefit.data.entity.TagWithCount
import com.txwstudio.app.whatthefit.domain.model.TagKind
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WardrobeRepositoryImpl @Inject constructor(
    private val db: WtfDatabase,
    private val categoryDao: CategoryDao,
    private val itemDao: ClothingItemDao,
    private val crossRefDao: CrossRefDao,
    private val tagDao: TagDao,
    private val tagCrossRefDao: TagCrossRefDao,
) : WardrobeRepository {
    // --- Categories ---

    override fun observeCategories(): Flow<List<Category>> = categoryDao.observeAll()

    override fun observeCategoriesWithCounts(): Flow<List<CategoryWithCount>> =
        categoryDao.observeAllWithCounts()

    override suspend fun getCategory(id: Long): Category? = categoryDao.getById(id)

    override suspend fun addCategory(name: String): Long {
        val nextOrder = categoryDao.getMaxSortOrder() + 1
        return categoryDao.insert(Category(name = name, sortOrder = nextOrder))
    }

    override suspend fun renameCategory(id: Long, name: String) {
        val existing = categoryDao.getById(id) ?: return
        categoryDao.update(existing.copy(name = name))
    }

    override suspend fun deleteCategory(category: Category) = categoryDao.delete(category)

    override suspend fun reorderCategories(orderedIds: List<Long>) = categoryDao.applyOrder(orderedIds)

    // --- Tags ---

    override fun observeTags(kind: TagKind): Flow<List<Tag>> = tagDao.observeByKind(kind)

    override fun observeTagsWithCounts(kind: TagKind): Flow<List<TagWithCount>> =
        tagDao.observeByKindWithCounts(kind)

    override suspend fun addTag(kind: TagKind, name: String, swatchArgb: Long?): Long {
        val nextOrder = tagDao.getMaxSortOrder(kind) + 1
        return tagDao.insert(Tag(kind = kind, name = name, sortOrder = nextOrder, swatchArgb = swatchArgb))
    }

    override suspend fun updateTag(tag: Tag) = tagDao.update(tag)

    override suspend fun deleteTag(tag: Tag) = tagDao.delete(tag)

    override suspend fun reorderTags(orderedIds: List<Long>) = tagDao.applyOrder(orderedIds)

    // --- Items ---

    /**
     * Text search (name/notes) combined with a faceted label filter: AND across dimensions, OR
     * within one. With no filters active we use the typed [ClothingItemDao.pagingSearch]; otherwise
     * we build a raw query that appends one `EXISTS` clause per non-empty dimension. Empty lists are
     * skipped (never emitted as `IN ()`), and `?` binds are appended in clause order.
     */
    override fun searchItems(
        query: String,
        categoryIds: List<Long>,
        brandIds: List<Long>,
        colorIds: List<Long>,
        occasionIds: List<Long>,
    ): PagingSource<Int, ItemWithDetails> {
        if (categoryIds.isEmpty() && brandIds.isEmpty() && colorIds.isEmpty() && occasionIds.isEmpty()) {
            return itemDao.pagingSearch(query)
        }

        val sql = StringBuilder(
            "SELECT * FROM ClothingItem WHERE (name LIKE '%' || ? || '%' OR notes LIKE '%' || ? || '%')",
        )
        val args = mutableListOf<Any>(query, query)

        fun appendExists(table: String, column: String, ids: List<Long>) {
            if (ids.isEmpty()) return
            val placeholders = ids.joinToString(",") { "?" }
            sql.append(
                " AND EXISTS (SELECT 1 FROM $table r " +
                    "WHERE r.itemId = ClothingItem.id AND r.$column IN ($placeholders))",
            )
            args.addAll(ids)
        }

        appendExists("ItemCategoryCrossRef", "categoryId", categoryIds) // Parts
        appendExists("ItemTagCrossRef", "tagId", brandIds) // Brands
        appendExists("ItemTagCrossRef", "tagId", colorIds) // Colors
        appendExists("ItemTagCrossRef", "tagId", occasionIds) // Occasions
        sql.append(" ORDER BY name ASC, id ASC")

        return itemDao.pagingSearchFiltered(SimpleSQLiteQuery(sql.toString(), args.toTypedArray()))
    }

    override fun observeItemCount(): Flow<Int> = itemDao.observeCount()

    override suspend fun getAvailableItems(categoryId: Long): List<ClothingItem> =
        itemDao.getAvailableItemsByCategory(categoryId)

    override suspend fun getItemWithDetails(id: Long): ItemWithDetails? =
        itemDao.getItemWithDetails(id)

    /**
     * Insert (id == 0) or update an item and rebuild its category + tag links atomically.
     * Returns the item's id.
     */
    override suspend fun saveItem(
        item: ClothingItem,
        categoryIds: List<Long>,
        tagIds: List<Long>,
    ): Long = db.withTransaction {
        val id = if (item.id == 0L) {
            itemDao.insert(item)
        } else {
            itemDao.update(item)
            item.id
        }
        crossRefDao.setItemCategories(id, categoryIds)
        tagCrossRefDao.setItemTags(id, tagIds)
        id
    }

    override suspend fun deleteItem(item: ClothingItem) = itemDao.delete(item)

    override suspend fun setAvailability(id: Long, available: Boolean) =
        itemDao.setAvailable(id, available)
}
