package com.txwstudio.app.whatthefit.data

/**
 * Season tags stored as a bit flag on [com.txwstudio.app.whatthefit.data.entity.ClothingItem.seasons].
 * Labels only in this version — seasons are displayed/edited but do not filter generation.
 */
object Season {
    const val SPRING = 1 // 0001
    const val SUMMER = 2 // 0010
    const val AUTUMN = 4 // 0100
    const val WINTER = 8 // 1000
    const val ALL = SPRING or SUMMER or AUTUMN or WINTER // 1111

    /** All season flags in display order. */
    val ENTRIES = listOf(SPRING, SUMMER, AUTUMN, WINTER)

    fun has(value: Int, season: Int): Boolean = (value and season) != 0

    /** Decompose a stored bit flag into the list of set season flags (in [ENTRIES] order). */
    fun toList(value: Int): List<Int> = ENTRIES.filter { has(value, it) }

    /** Combine season flags into a single stored bit flag. */
    fun fromList(seasons: Collection<Int>): Int = seasons.fold(0) { acc, season -> acc or season }
}
