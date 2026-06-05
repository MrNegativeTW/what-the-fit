package com.txwstudio.app.whatthefit.ui.navigation

import androidx.annotation.StringRes
import com.txwstudio.app.whatthefit.R

/** All navigation routes and argument keys for the app. */
object WtfRoutes {
    const val HOME = "home" // Generate
    const val CLOTHES = "clothes" // item + category management container
    const val SETTINGS = "settings"

    const val ARG_CATEGORY_IDS = "categoryIds"
    const val ARG_ITEM_ID = "itemId"

    // result?categoryIds=1,2,3
    const val RESULT_PATTERN = "result?$ARG_CATEGORY_IDS={$ARG_CATEGORY_IDS}"
    fun result(categoryIds: List<Long>) =
        "result?$ARG_CATEGORY_IDS=${categoryIds.joinToString(",")}"

    // item_edit?itemId=0  (0 = add mode)
    const val ITEM_EDIT_PATTERN = "item_edit?$ARG_ITEM_ID={$ARG_ITEM_ID}"
    fun itemEdit(itemId: Long = 0L) = "item_edit?$ARG_ITEM_ID=$itemId"
}

/** Top-level destinations shown in the bottom navigation bar. */
enum class TopLevelDestination(val route: String, @get:StringRes val labelRes: Int) {
    HOME(WtfRoutes.HOME, R.string.nav_home),
    CLOTHES(WtfRoutes.CLOTHES, R.string.nav_wardrobe),
}
