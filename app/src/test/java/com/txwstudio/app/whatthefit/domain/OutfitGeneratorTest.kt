package com.txwstudio.app.whatthefit.domain

import androidx.paging.PagingSource
import com.txwstudio.app.whatthefit.data.entity.Category
import com.txwstudio.app.whatthefit.data.entity.ClothingItem
import com.txwstudio.app.whatthefit.data.entity.ItemWithDetails
import com.txwstudio.app.whatthefit.data.repository.WardrobeRepository
import com.txwstudio.app.whatthefit.domain.model.TagKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.random.Random

class OutfitGeneratorTest {

    /** Fake repository exposing only the two methods the generator uses. */
    private class FakeRepo(
        private val categories: Map<Long, Category>,
        private val pools: Map<Long, List<ClothingItem>>,
    ) : WardrobeRepository {
        override suspend fun getCategory(id: Long): Category? = categories[id]
        override suspend fun getAvailableItems(categoryId: Long): List<ClothingItem> =
            pools[categoryId] ?: emptyList()

        override fun observeCategories(): Flow<List<Category>> = error("unused")
        override fun observeCategoriesWithCounts() = error("unused")
        override suspend fun addCategory(name: String): Long = error("unused")
        override suspend fun renameCategory(id: Long, name: String) = error("unused")
        override suspend fun deleteCategory(category: Category) = error("unused")
        override suspend fun reorderCategories(orderedIds: List<Long>) = error("unused")
        override fun observeTags(kind: TagKind) = error("unused")
        override fun observeTagsWithCounts(kind: TagKind) = error("unused")
        override suspend fun addTag(kind: TagKind, name: String, swatchArgb: Long?): Long = error("unused")
        override suspend fun updateTag(tag: com.txwstudio.app.whatthefit.data.entity.Tag) = error("unused")
        override suspend fun deleteTag(tag: com.txwstudio.app.whatthefit.data.entity.Tag) = error("unused")
        override suspend fun reorderTags(orderedIds: List<Long>) = error("unused")
        override fun searchItems(
            query: String,
            categoryIds: List<Long>,
            brandIds: List<Long>,
            colorIds: List<Long>,
            occasionIds: List<Long>,
            fitIds: List<Long>,
        ): PagingSource<Int, ItemWithDetails> = error("unused")
        override fun observeItemCount(): Flow<Int> = error("unused")
        override suspend fun getItemWithDetails(id: Long): ItemWithDetails? = error("unused")
        override suspend fun saveItem(item: ClothingItem, categoryIds: List<Long>, tagIds: List<Long>): Long =
            error("unused")
        override suspend fun deleteItem(item: ClothingItem) = error("unused")
        override suspend fun setAvailability(id: Long, available: Boolean) = error("unused")
    }

    private fun cat(id: Long) = Category(id = id, name = "C$id", sortOrder = id.toInt())
    private fun item(id: Long) = ClothingItem(id = id, name = "I$id")

    @Test
    fun generate_oneSlotPerCategoryInOrder_emptyPoolGivesNull() = runTest {
        val repo = FakeRepo(
            categories = mapOf(1L to cat(1), 2L to cat(2)),
            pools = mapOf(1L to listOf(item(10), item(11)), 2L to emptyList()),
        )
        val slots = OutfitGenerator(repo, Random(0)).generate(listOf(1L, 2L))

        assertEquals(listOf(1L, 2L), slots.map { it.category.id })
        assertNotNull(slots[0].item)
        assertNull(slots[1].item) // empty pool -> 無可用衣物
    }

    @Test
    fun generate_skipsDeletedCategory() = runTest {
        val repo = FakeRepo(
            categories = mapOf(1L to cat(1)), // id 2 deleted
            pools = mapOf(1L to listOf(item(10))),
        )
        val slots = OutfitGenerator(repo, Random(0)).generate(listOf(1L, 2L))
        assertEquals(listOf(1L), slots.map { it.category.id })
    }

    @Test
    fun rerollSingle_returnsDifferentItem_whenMultipleAvailable() = runTest {
        val pool = (0L until 5L).map { item(it) }
        val repo = FakeRepo(mapOf(1L to cat(1)), mapOf(1L to pool))
        val gen = OutfitGenerator(repo, Random(42))
        repeat(20) {
            val current = pool.random().id
            val result = gen.rerollSingle(1L, current)
            assertNotNull(result)
            assertNotEquals(current, result!!.id)
        }
    }

    @Test
    fun rerollSingle_returnsSameItem_whenOnlyOneAvailable() = runTest {
        val repo = FakeRepo(mapOf(1L to cat(1)), mapOf(1L to listOf(item(10))))
        val result = OutfitGenerator(repo, Random(1)).rerollSingle(1L, 10L)
        assertEquals(10L, result?.id)
    }

    @Test
    fun rerollSingle_returnsNull_whenPoolEmpty() = runTest {
        val repo = FakeRepo(mapOf(1L to cat(1)), mapOf(1L to emptyList()))
        assertNull(OutfitGenerator(repo, Random(1)).rerollSingle(1L, null))
    }

    @Test
    fun generate_allowsSameItemAcrossCategories_noDedup() = runTest {
        val shared = item(99)
        val repo = FakeRepo(
            categories = mapOf(1L to cat(1), 2L to cat(2)),
            pools = mapOf(1L to listOf(shared), 2L to listOf(shared)),
        )
        val slots = OutfitGenerator(repo, Random(0)).generate(listOf(1L, 2L))
        assertEquals(99L, slots[0].item?.id)
        assertEquals(99L, slots[1].item?.id) // same item allowed in both slots
    }
}
