package com.txwstudio.app.whatthefit.data.repository

import androidx.paging.PagingSource
import com.txwstudio.app.whatthefit.data.entity.Category
import com.txwstudio.app.whatthefit.data.entity.CategoryWithCount
import com.txwstudio.app.whatthefit.data.entity.ClothingItem
import com.txwstudio.app.whatthefit.data.entity.ItemWithDetails
import com.txwstudio.app.whatthefit.data.entity.Tag
import com.txwstudio.app.whatthefit.data.entity.TagWithCount
import com.txwstudio.app.whatthefit.domain.model.TagKind
import kotlinx.coroutines.flow.Flow

/** Single entry point for all wardrobe data. Implemented by [WardrobeRepositoryImpl]. */
interface WardrobeRepository {
    // Categories (parts)
    fun observeCategories(): Flow<List<Category>>
    fun observeCategoriesWithCounts(): Flow<List<CategoryWithCount>>
    suspend fun getCategory(id: Long): Category?
    suspend fun addCategory(name: String): Long
    suspend fun renameCategory(id: Long, name: String)
    suspend fun deleteCategory(category: Category)
    suspend fun reorderCategories(orderedIds: List<Long>)

    // Tags (brand / color / occasion)
    fun observeTags(kind: TagKind): Flow<List<Tag>>
    fun observeTagsWithCounts(kind: TagKind): Flow<List<TagWithCount>>
    suspend fun addTag(kind: TagKind, name: String, swatchArgb: Long? = null): Long
    suspend fun updateTag(tag: Tag)
    suspend fun deleteTag(tag: Tag)
    suspend fun reorderTags(orderedIds: List<Long>)

    // Items
    fun searchItems(
        query: String,
        categoryIds: List<Long>,
        brandIds: List<Long>,
        colorIds: List<Long>,
        occasionIds: List<Long>,
    ): PagingSource<Int, ItemWithDetails>

    fun observeItemCount(): Flow<Int>
    suspend fun getAvailableItems(categoryId: Long): List<ClothingItem>
    suspend fun getItemWithDetails(id: Long): ItemWithDetails?
    suspend fun saveItem(item: ClothingItem, categoryIds: List<Long>, tagIds: List<Long>): Long
    suspend fun deleteItem(item: ClothingItem)
    suspend fun setAvailability(id: Long, available: Boolean)
}
