package com.txwstudio.app.whatthefit.ui.clothing

import com.txwstudio.app.whatthefit.data.entity.Category
import com.txwstudio.app.whatthefit.data.entity.ClothingItem
import com.txwstudio.app.whatthefit.data.entity.ItemWithDetails
import org.junit.Assert.assertEquals
import org.junit.Test

class BuildSectionsTest {
    private fun cat(id: Long, order: Int) = Category(id = id, name = "C$id", sortOrder = order)
    private fun item(id: Long, cats: List<Category>) =
        ItemWithDetails(ClothingItem(id = id, name = "I$id"), categories = cats, tags = emptyList())

    private val top = cat(1, 0)
    private val outer = cat(2, 1)
    private val shoes = cat(3, 2)

    @Test
    fun browse_showsAllPartsInOrder_includingEmpty_uncategorizedLast() {
        val jacket = item(10, listOf(top, outer)) // belongs to two parts
        val sneaker = item(11, listOf(shoes))
        val orphan = item(12, emptyList())

        val sections = buildSections(
            items = listOf(jacket, sneaker, orphan),
            categories = listOf(top, outer, shoes),
            filtering = false,
        )

        assertEquals(listOf(1L, 2L, 3L, UNCATEGORIZED_KEY), sections.map { it.key })
        // The two-part jacket appears under BOTH top and outer.
        assertEquals(listOf(10L), sections[0].items.map { it.item.id })
        assertEquals(listOf(10L), sections[1].items.map { it.item.id })
        assertEquals(listOf(11L), sections[2].items.map { it.item.id })
        assertEquals(listOf(12L), sections[3].items.map { it.item.id })
    }

    @Test
    fun browse_keepsEmptyParts_butHidesEmptyUncategorized() {
        val sneaker = item(11, listOf(shoes))

        val sections = buildSections(listOf(sneaker), listOf(top, outer, shoes), filtering = false)

        assertEquals(listOf(1L, 2L, 3L), sections.map { it.key }) // no Uncategorized section
        assertEquals(0, sections[0].count)
        assertEquals(1, sections[2].count)
    }

    @Test
    fun filtering_hidesEmptySections() {
        val jacket = item(10, listOf(top, outer))

        val sections = buildSections(listOf(jacket), listOf(top, outer, shoes), filtering = true)

        assertEquals(listOf(1L, 2L), sections.map { it.key }) // shoes (empty) dropped
    }

    @Test
    fun filtering_showsUncategorizedOnlyWhenMatched() {
        val orphan = item(12, emptyList())

        val sections = buildSections(listOf(orphan), listOf(top), filtering = true)

        assertEquals(listOf(UNCATEGORIZED_KEY), sections.map { it.key })
    }
}
