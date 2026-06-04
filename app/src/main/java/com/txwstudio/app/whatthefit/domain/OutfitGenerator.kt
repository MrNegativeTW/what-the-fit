package com.txwstudio.app.whatthefit.domain

import com.txwstudio.app.whatthefit.data.entity.ClothingItem
import com.txwstudio.app.whatthefit.data.repository.WardrobeRepository
import com.txwstudio.app.whatthefit.domain.model.OutfitSlot
import javax.inject.Inject
import kotlin.random.Random

/**
 * Builds a random outfit by sampling one available item per selected category. Categories are
 * sampled independently with no cross-category de-duplication, so the same item may appear in
 * more than one slot. [random] is injectable for deterministic tests.
 */
class OutfitGenerator(
    private val repository: WardrobeRepository,
    private val random: Random,
) {
    @Inject
    constructor(repository: WardrobeRepository) : this(repository, Random.Default)

    /** One slot per selected category (in the given order); slot.item is null when the pool is empty. */
    suspend fun generate(selectedCategoryIds: List<Long>): List<OutfitSlot> =
        selectedCategoryIds.mapNotNull { categoryId ->
            val category = repository.getCategory(categoryId) ?: return@mapNotNull null
            val pool = repository.getAvailableItems(categoryId)
            OutfitSlot(category, pool.randomOrNull(random))
        }

    /**
     * Re-pick a single category. Returns a different item than [currentItemId] when at least two
     * are available, the same item when exactly one is available, and null when the pool is empty.
     */
    suspend fun rerollSingle(categoryId: Long, currentItemId: Long?): ClothingItem? {
        val pool = repository.getAvailableItems(categoryId)
        if (pool.isEmpty()) return null
        val others = pool.filter { it.id != currentItemId }
        return others.ifEmpty { pool }.random(random)
    }
}
