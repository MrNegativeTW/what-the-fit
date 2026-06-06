package com.txwstudio.app.whatthefit.data

import android.content.Context
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.testing.asSnapshot
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.txwstudio.app.whatthefit.R
import com.txwstudio.app.whatthefit.data.dao.CategoryDao
import com.txwstudio.app.whatthefit.data.dao.ClothingItemDao
import com.txwstudio.app.whatthefit.data.dao.CrossRefDao
import com.txwstudio.app.whatthefit.data.dao.TagCrossRefDao
import com.txwstudio.app.whatthefit.data.dao.TagDao
import com.txwstudio.app.whatthefit.data.db.WtfDatabase
import com.txwstudio.app.whatthefit.data.entity.Category
import com.txwstudio.app.whatthefit.data.entity.ClothingItem
import com.txwstudio.app.whatthefit.data.entity.ItemCategoryCrossRef
import com.txwstudio.app.whatthefit.data.entity.Tag
import com.txwstudio.app.whatthefit.data.repository.WardrobeRepositoryImpl
import com.txwstudio.app.whatthefit.domain.model.TagKind
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WtfDatabaseTest {
    private lateinit var db: WtfDatabase
    private lateinit var categoryDao: CategoryDao
    private lateinit var itemDao: ClothingItemDao
    private lateinit var crossRefDao: CrossRefDao
    private lateinit var tagDao: TagDao
    private lateinit var tagCrossRefDao: TagCrossRefDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // No seed callback here — start clean for deterministic assertions.
        db = Room.inMemoryDatabaseBuilder(context, WtfDatabase::class.java)
            .build()
        categoryDao = db.categoryDao()
        itemDao = db.clothingItemDao()
        crossRefDao = db.crossRefDao()
        tagDao = db.tagDao()
        tagCrossRefDao = db.tagCrossRefDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun itemWithCategories_returnsAllLinkedCategories() = runTest {
        val hatId = categoryDao.insert(Category(name = "帽子"))
        val jacketId = categoryDao.insert(Category(name = "外套"))
        val itemId = itemDao.insert(ClothingItem(name = "連帽外套"))
        crossRefDao.setItemCategories(itemId, listOf(hatId, jacketId))

        val result = itemDao.getItemWithDetails(itemId)
        assertNotNull(result)
        assertEquals(setOf(hatId, jacketId), result!!.categories.map { it.id }.toSet())
    }

    @Test
    fun deletingCategory_cascadesCrossRefsButKeepsItems() = runTest {
        val catId = categoryDao.insert(Category(name = "上衣"))
        val itemId = itemDao.insert(ClothingItem(name = "藍色上衣"))
        crossRefDao.setItemCategories(itemId, listOf(catId))

        categoryDao.delete(Category(id = catId, name = "上衣"))

        // Item survives, just unlinked (orphan).
        val item = itemDao.getItemWithDetails(itemId)
        assertNotNull(item)
        assertTrue(item!!.categories.isEmpty())
        // No items remain available for the deleted category.
        assertTrue(itemDao.getAvailableItemsByCategory(catId).isEmpty())
    }

    @Test
    fun deletingItem_cascadesCrossRefs() = runTest {
        val catId = categoryDao.insert(Category(name = "鞋子"))
        val itemId = itemDao.insert(ClothingItem(name = "白布鞋"))
        crossRefDao.setItemCategories(itemId, listOf(catId))

        itemDao.delete(ClothingItem(id = itemId, name = "白布鞋"))

        assertTrue(itemDao.getAvailableItemsByCategory(catId).isEmpty())
    }

    @Test
    fun setItemCategories_replacesLinks() = runTest {
        val a = categoryDao.insert(Category(name = "A"))
        val b = categoryDao.insert(Category(name = "B"))
        val c = categoryDao.insert(Category(name = "C"))
        val itemId = itemDao.insert(ClothingItem(name = "multi"))

        crossRefDao.setItemCategories(itemId, listOf(a, b))
        crossRefDao.setItemCategories(itemId, listOf(b, c))

        val categories = itemDao.getItemWithDetails(itemId)!!.categories.map { it.id }.toSet()
        assertEquals(setOf(b, c), categories)
    }

    @Test
    fun getAvailableItems_respectsAvailabilityAndCategory() = runTest {
        val tops = categoryDao.insert(Category(name = "上衣"))
        val pants = categoryDao.insert(Category(name = "褲子"))

        val availableTop = itemDao.insert(ClothingItem(name = "可用上衣", isAvailable = true))
        val laundryTop = itemDao.insert(ClothingItem(name = "送洗上衣", isAvailable = false))
        val pant = itemDao.insert(ClothingItem(name = "褲子一件", isAvailable = true))
        itemDao.insert(ClothingItem(name = "孤兒衣物", isAvailable = true)) // no category

        crossRefDao.setItemCategories(availableTop, listOf(tops))
        crossRefDao.setItemCategories(laundryTop, listOf(tops))
        crossRefDao.setItemCategories(pant, listOf(pants))

        val topPool = itemDao.getAvailableItemsByCategory(tops)
        assertEquals(listOf(availableTop), topPool.map { it.id })
    }

    @Test
    fun pagingSearch_filtersByNameAndMatchesAllWhenBlank() = runTest {
        val ids = listOf("Uniqlo 藍上衣", "Uniqlo 白上衣", "Nike 外套").map {
            itemDao.insert(ClothingItem(name = it))
        }

        val all = Pager(PagingConfig(pageSize = 20, enablePlaceholders = false)) {
            itemDao.pagingSearch("")
        }.flow.asSnapshot()
        assertEquals(3, all.size)

        val uniqlo = Pager(PagingConfig(pageSize = 20, enablePlaceholders = false)) {
            itemDao.pagingSearch("Uniqlo")
        }.flow.asSnapshot()
        assertEquals(2, uniqlo.size)
        assertTrue(uniqlo.all { it.item.name.contains("Uniqlo") })

        // Sanity: every inserted id is discoverable.
        assertEquals(ids.toSet(), all.map { it.item.id }.toSet())
    }

    @Test
    fun pagingSearch_matchesNameAndNotes() = runTest {
        itemDao.insert(ClothingItem(name = "plain tee"))
        itemDao.insert(ClothingItem(name = "gift shirt", notes = "from mom"))

        suspend fun search(q: String) = Pager(PagingConfig(pageSize = 20, enablePlaceholders = false)) {
            itemDao.pagingSearch(q)
        }.flow.asSnapshot()

        assertEquals(1, search("tee").size) // name match
        assertEquals(1, search("mom").size) // notes match
        assertEquals(2, search("").size)
    }

    @Test
    fun foreignKeyViolation_isRejected() = runTest {
        try {
            crossRefDao.insert(ItemCategoryCrossRef(itemId = 999, categoryId = 888))
            fail("Expected a foreign key constraint violation")
        } catch (e: Exception) {
            assertTrue(
                "Expected SQLite constraint failure, got: ${e::class.java.name}",
                e.message?.contains("FOREIGN KEY", ignoreCase = true) == true ||
                    e::class.java.simpleName.contains("Constraint"),
            )
        }
    }

    @Test
    fun observeAllWithCounts_countsLinkedItemsPerCategory() = runTest {
        val tops = categoryDao.insert(Category(name = "上衣"))
        val hat = categoryDao.insert(Category(name = "帽子"))
        val emptyCat = categoryDao.insert(Category(name = "鞋子"))
        val a = itemDao.insert(ClothingItem(name = "a"))
        val b = itemDao.insert(ClothingItem(name = "b"))
        crossRefDao.setItemCategories(a, listOf(tops))
        crossRefDao.setItemCategories(b, listOf(tops, hat))

        val counts = categoryDao.observeAllWithCounts().first()
            .associate { it.category.id to it.itemCount }
        assertEquals(2, counts[tops])
        assertEquals(1, counts[hat])
        assertEquals(0, counts[emptyCat])
    }

    @Test
    fun tags_filterByKindCountAndCascade() = runTest {
        val brand = tagDao.insert(Tag(kind = TagKind.BRAND, name = "Uniqlo"))
        val blue = tagDao.insert(Tag(kind = TagKind.COLOR, name = "Blue", swatchArgb = 0xFF0000FFL))
        tagDao.insert(Tag(kind = TagKind.COLOR, name = "Red"))
        val item = itemDao.insert(ClothingItem(name = "shirt"))
        tagCrossRefDao.setItemTags(item, listOf(brand, blue))

        // observeByKind filters by kind.
        assertEquals(listOf("Uniqlo"), tagDao.observeByKind(TagKind.BRAND).first().map { it.name })
        assertEquals(setOf("Blue", "Red"), tagDao.observeByKind(TagKind.COLOR).first().map { it.name }.toSet())

        // Counts per tag.
        val colorCounts = tagDao.observeByKindWithCounts(TagKind.COLOR).first()
            .associate { it.tag.name to it.itemCount }
        assertEquals(1, colorCounts["Blue"])
        assertEquals(0, colorCounts["Red"])

        // The item carries its tags.
        assertEquals(setOf(brand, blue), itemDao.getItemWithDetails(item)!!.tags.map { it.id }.toSet())

        // Deleting a tag cascades the link but keeps the item.
        tagDao.delete(Tag(id = blue, kind = TagKind.COLOR, name = "Blue"))
        assertNotNull(itemDao.getItemWithDetails(item))
        assertEquals(setOf(brand), itemDao.getItemWithDetails(item)!!.tags.map { it.id }.toSet())
    }

    @Test
    fun searchItems_facetedFilterAcrossDimensions() = runTest {
        val repo = WardrobeRepositoryImpl(db, categoryDao, itemDao, crossRefDao, tagDao, tagCrossRefDao)

        val tops = categoryDao.insert(Category(name = "Tops"))
        val pants = categoryDao.insert(Category(name = "Pants"))
        val uniqlo = tagDao.insert(Tag(kind = TagKind.BRAND, name = "UNIQLO"))
        val gu = tagDao.insert(Tag(kind = TagKind.BRAND, name = "GU"))
        val blue = tagDao.insert(Tag(kind = TagKind.COLOR, name = "Blue"))
        val black = tagDao.insert(Tag(kind = TagKind.COLOR, name = "Black"))

        val blueUniqloTop = itemDao.insert(ClothingItem(name = "Blue Tee"))
        crossRefDao.setItemCategories(blueUniqloTop, listOf(tops))
        tagCrossRefDao.setItemTags(blueUniqloTop, listOf(uniqlo, blue))

        val blackGuPants = itemDao.insert(ClothingItem(name = "Black Jeans"))
        crossRefDao.setItemCategories(blackGuPants, listOf(pants))
        tagCrossRefDao.setItemTags(blackGuPants, listOf(gu, black))

        val blackUniqloTop = itemDao.insert(ClothingItem(name = "Black Tee"))
        crossRefDao.setItemCategories(blackUniqloTop, listOf(tops))
        tagCrossRefDao.setItemTags(blackUniqloTop, listOf(uniqlo, black))

        suspend fun search(
            q: String = "",
            cats: List<Long> = emptyList(),
            brands: List<Long> = emptyList(),
            colors: List<Long> = emptyList(),
            occasions: List<Long> = emptyList(),
            fits: List<Long> = emptyList(),
        ): Set<Long> = Pager(PagingConfig(pageSize = 20, enablePlaceholders = false)) {
            repo.searchItems(q, cats, brands, colors, occasions, fits)
        }.flow.asSnapshot().map { it.item.id }.toSet()

        // No filter (fast path) → all three.
        assertEquals(setOf(blueUniqloTop, blackGuPants, blackUniqloTop), search())
        // OR within a dimension: Blue OR Black → all three (every item is blue or black).
        assertEquals(setOf(blueUniqloTop, blackGuPants, blackUniqloTop), search(colors = listOf(blue, black)))
        // AND across dimensions: (Blue OR Black) AND UNIQLO → the two UNIQLO tops, not the GU pants.
        assertEquals(setOf(blueUniqloTop, blackUniqloTop), search(colors = listOf(blue, black), brands = listOf(uniqlo)))
        // Category facet alone.
        assertEquals(setOf(blackGuPants), search(cats = listOf(pants)))
        // Cross-facet with no overlap → empty.
        assertTrue(search(cats = listOf(pants), brands = listOf(uniqlo)).isEmpty())
        // Text AND filter.
        assertEquals(setOf(blackUniqloTop), search(q = "Tee", colors = listOf(black)))
    }

    @Test
    fun seedCallback_insertsLocalizedDefaultsForAllKinds() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val seeded = Room.inMemoryDatabaseBuilder(context, WtfDatabase::class.java)
            .addCallback(WtfDatabase.seedCallback(context))
            .build()
        try {
            val expectedParts = WtfDatabase.DEFAULT_PART_NAME_RES.map { context.getString(it) }
            assertEquals(expectedParts, seeded.categoryDao().getAll().map { it.name })

            val colors = seeded.tagDao().observeByKind(TagKind.COLOR).first()
            assertEquals(WtfDatabase.DEFAULT_COLORS.map { context.getString(it.first) }, colors.map { it.name })
            assertTrue(colors.all { it.swatchArgb != null })

            val occasions = seeded.tagDao().observeByKind(TagKind.OCCASION).first().map { it.name }
            assertEquals(WtfDatabase.DEFAULT_OCCASION_NAME_RES.map { context.getString(it) }, occasions)

            val brands = seeded.tagDao().observeByKind(TagKind.BRAND).first().map { it.name }
            assertEquals(WtfDatabase.DEFAULT_BRANDS, brands)

            val fits = seeded.tagDao().observeByKind(TagKind.FIT).first().map { it.name }
            assertEquals(WtfDatabase.DEFAULT_FIT_NAME_RES.map { context.getString(it) }, fits)

            // Default clothes seed, linked to their part + color.
            assertEquals(
                WtfDatabase.DEFAULT_ITEMS.size,
                seeded.clothingItemDao().observeCount().first(),
            )
            val topId = seeded.categoryDao().getAll()
                .first { it.name == context.getString(R.string.part_top) }.id
            val tops = seeded.clothingItemDao().getAvailableItemsByCategory(topId).map { it.name }
            assertEquals(
                setOf(
                    context.getString(R.string.item_default_top_white),
                    context.getString(R.string.item_default_top_black),
                ),
                tops.toSet(),
            )
        } finally {
            seeded.close()
        }
    }
}
