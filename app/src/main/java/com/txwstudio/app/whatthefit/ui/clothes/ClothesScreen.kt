package com.txwstudio.app.whatthefit.ui.clothes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.txwstudio.app.whatthefit.R
import com.txwstudio.app.whatthefit.domain.model.TagKind
import com.txwstudio.app.whatthefit.ui.categories.CategoryScreen
import com.txwstudio.app.whatthefit.ui.items.ItemListScreen
import com.txwstudio.app.whatthefit.ui.items.ItemListViewModel
import com.txwstudio.app.whatthefit.ui.tags.TagListScreen
import kotlinx.coroutines.launch

private val TAB_LABELS = listOf(
    R.string.clothes_tab_items,
    R.string.clothes_tab_parts,
    R.string.clothes_tab_fit,
    R.string.clothes_tab_brands,
    R.string.clothes_tab_colors,
    R.string.clothes_tab_occasions,
)

private const val ITEMS_PAGE = 0

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClothesScreen(
    onAddItem: () -> Unit,
    onEditItem: (Long) -> Unit,
    modifier: Modifier = Modifier,
    // App-scoped instance passed from WtfApp so the top-bar search field and this list/filter
    // share one source of truth. The default is only for previews.
    itemListViewModel: ItemListViewModel = hiltViewModel(),
) {
    val pagerState = rememberPagerState(pageCount = { TAB_LABELS.size })
    val scope = rememberCoroutineScope()

    // From a sub-tab (Parts/Fit/Brands/...), back first returns to the Clothes tab before leaving Wardrobe.
    BackHandler(enabled = pagerState.currentPage != ITEMS_PAGE) {
        scope.launch { pagerState.animateScrollToPage(ITEMS_PAGE) }
    }

    Column(modifier.fillMaxSize()) {
        PrimaryScrollableTabRow(selectedTabIndex = pagerState.currentPage) {
            TAB_LABELS.forEachIndexed { index, labelRes ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = { Text(stringResource(labelRes)) },
                )
            }
        }
        // Swipe left/right within the content area to move between tabs.
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) { page ->
            when (page) {
                ITEMS_PAGE -> ItemListScreen(
                    onAddItem = onAddItem,
                    onEditItem = onEditItem,
                    viewModel = itemListViewModel,
                )

                1 -> CategoryScreen()
                2 -> TagListScreen(kind = TagKind.FIT)
                3 -> TagListScreen(kind = TagKind.BRAND)
                4 -> TagListScreen(kind = TagKind.COLOR)
                else -> TagListScreen(kind = TagKind.OCCASION)
            }
        }
    }
}
