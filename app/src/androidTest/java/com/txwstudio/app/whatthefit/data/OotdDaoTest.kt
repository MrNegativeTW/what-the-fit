package com.txwstudio.app.whatthefit.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.txwstudio.app.whatthefit.data.dao.CategoryDao
import com.txwstudio.app.whatthefit.data.dao.ClothingItemDao
import com.txwstudio.app.whatthefit.data.dao.OotdDao
import com.txwstudio.app.whatthefit.data.db.WtfDatabase
import com.txwstudio.app.whatthefit.data.entity.Category
import com.txwstudio.app.whatthefit.data.entity.ClothingItem
import com.txwstudio.app.whatthefit.data.entity.OotdRecord
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OotdDaoTest {
    private lateinit var db: WtfDatabase
    private lateinit var categoryDao: CategoryDao
    private lateinit var itemDao: ClothingItemDao
    private lateinit var ootdDao: OotdDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, WtfDatabase::class.java).build()
        categoryDao = db.categoryDao()
        itemDao = db.clothingItemDao()
        ootdDao = db.ootdDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun getById_resolvesCategoryAndItem() = runTest {
        val catId = categoryDao.insert(Category(name = "上衣"))
        val itemId = itemDao.insert(ClothingItem(name = "白色 T-Shirt"))
        val ootdId = ootdDao.insert(OotdRecord(epochDay = 100))
        ootdDao.setSlots(ootdId, listOf(catId to itemId))

        val record = ootdDao.getById(ootdId)
        assertNotNull(record)
        assertEquals(1, record!!.slots.size)
        assertEquals(catId, record.slots[0].category?.id)
        assertEquals(itemId, record.slots[0].item?.id)
    }

    @Test
    fun observeAll_ordersByDateDescending() = runTest {
        val catId = categoryDao.insert(Category(name = "上衣"))
        val itemId = itemDao.insert(ClothingItem(name = "白色 T-Shirt"))
        val older = ootdDao.insert(OotdRecord(epochDay = 100))
        val newer = ootdDao.insert(OotdRecord(epochDay = 200))
        ootdDao.setSlots(older, listOf(catId to itemId))
        ootdDao.setSlots(newer, listOf(catId to itemId))

        val all = ootdDao.observeAll().first()
        assertEquals(listOf(newer, older), all.map { it.record.id })
    }

    @Test
    fun deletingItem_removesItFromRecordSlots() = runTest {
        val catId = categoryDao.insert(Category(name = "上衣"))
        val item = ClothingItem(name = "白色 T-Shirt")
        val itemId = itemDao.insert(item)
        val ootdId = ootdDao.insert(OotdRecord(epochDay = 100))
        ootdDao.setSlots(ootdId, listOf(catId to itemId))

        itemDao.delete(item.copy(id = itemId))

        val record = ootdDao.getById(ootdId)
        assertNotNull(record)
        assertTrue(record!!.slots.isEmpty())
    }

    @Test
    fun deletingRecord_removesItAndItsSlots() = runTest {
        val catId = categoryDao.insert(Category(name = "上衣"))
        val itemId = itemDao.insert(ClothingItem(name = "白色 T-Shirt"))
        val record = OotdRecord(epochDay = 100)
        val ootdId = ootdDao.insert(record)
        ootdDao.setSlots(ootdId, listOf(catId to itemId))

        ootdDao.delete(record.copy(id = ootdId))

        assertNull(ootdDao.getById(ootdId))
        assertTrue(ootdDao.observeAll().first().isEmpty())
    }
}
