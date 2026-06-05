package com.txwstudio.app.whatthefit.ui

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.txwstudio.app.whatthefit.R
import com.txwstudio.app.whatthefit.domain.model.ThemeMode
import com.txwstudio.app.whatthefit.ui.clothes.ClothesScreen
import com.txwstudio.app.whatthefit.ui.generate.GenerateScreen
import com.txwstudio.app.whatthefit.ui.items.ItemEditScreen
import com.txwstudio.app.whatthefit.ui.items.ItemListViewModel
import com.txwstudio.app.whatthefit.ui.navigation.TopLevelDestination
import com.txwstudio.app.whatthefit.ui.navigation.WtfRoutes
import com.txwstudio.app.whatthefit.ui.result.ResultScreen
import com.txwstudio.app.whatthefit.ui.settings.SettingsScreen
import com.txwstudio.app.whatthefit.ui.theme.WTFTheme

/** Search-field height matching Google's apps (Contacts/Drive) — sleeker than M3's 56dp default. */
private val SearchBarHeight = 48.dp

/**
 * Shared duration for screen transitions. The app chrome, the search bar and bottom navigation,
 * animates on this same timeline as the NavHost content so navigating to a detail screen reads as a
 * single motion instead of the chrome being removed before the content finishes fading.
 */
private const val ScreenTransitionMillis = 300

@Composable
fun WtfApp(appViewModel: AppViewModel = hiltViewModel()) {
    val themeMode by appViewModel.themeMode.collectAsStateWithLifecycle()
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    // Keep the status/navigation bar icon contrast in sync with the in-app theme — not the system
    // theme — so light icons show on our dark background and vice versa, even when they differ.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    // Material You dynamic color is always on. Language is handled by the system per-app locale.
    WTFTheme(darkTheme = darkTheme, dynamicColor = true) {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route
        // Top-level destinations share the persistent chrome: bottom nav + the app-wide search bar.
        val topLevel = currentRoute == WtfRoutes.HOME || currentRoute == WtfRoutes.CLOTHES

        // App-scoped so the search field and the Clothes list/filters are one instance.
        val searchViewModel: ItemListViewModel = hiltViewModel()
        val query by searchViewModel.query.collectAsStateWithLifecycle()

        Scaffold(
            topBar = {
                AnimatedVisibility(
                    visible = topLevel,
                    enter = fadeIn(tween(ScreenTransitionMillis)) +
                            expandVertically(
                                tween(ScreenTransitionMillis),
                                expandFrom = Alignment.Top
                            ),
                    exit = fadeOut(tween(ScreenTransitionMillis)) +
                            shrinkVertically(
                                tween(ScreenTransitionMillis),
                                shrinkTowards = Alignment.Top
                            ),
                ) {
                    WardrobeSearchBar(
                        query = query,
                        onQueryChange = searchViewModel::onQueryChange,
                        onOpenSettings = { navController.navigate(WtfRoutes.SETTINGS) },
                    )
                }
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = topLevel,
                    enter = fadeIn(tween(ScreenTransitionMillis)) + expandVertically(
                        tween(
                            ScreenTransitionMillis
                        )
                    ),
                    exit = fadeOut(tween(ScreenTransitionMillis)) + shrinkVertically(
                        tween(
                            ScreenTransitionMillis
                        )
                    ),
                ) {
                    NavigationBar {
                        TopLevelDestination.entries.forEach { dest ->
                            val label = stringResource(dest.labelRes)
                            NavigationBarItem(
                                selected = currentRoute == dest.route,
                                onClick = {
                                    navController.navigate(dest.route) {
                                        popUpTo(WtfRoutes.HOME) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    when (dest) {
                                        TopLevelDestination.HOME -> Icon(
                                            painter = painterResource(R.drawable.outline_apparel_24),
                                            contentDescription = label,
                                        )

                                        TopLevelDestination.CLOTHES -> Icon(
                                            imageVector = Icons.Filled.Checkroom,
                                            contentDescription = label,
                                        )
                                    }
                                },
                                label = { Text(label) },
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = WtfRoutes.HOME,
                // Fade on the same timeline as the chrome above (see ScreenTransitionMillis) so the
                // search bar and bottom nav move together with the content rather than ahead of it.
                enterTransition = { fadeIn(tween(ScreenTransitionMillis)) },
                exitTransition = { fadeOut(tween(ScreenTransitionMillis)) },
                // padding() moves content into the safe area; consumeWindowInsets() marks those
                // insets as handled so each screen's own TopAppBar doesn't re-apply the status-bar
                // inset (which caused a doubled gap above the title).
                modifier = Modifier
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding),
            ) {
                composable(WtfRoutes.HOME) {
                    GenerateScreen(
                        onGenerate = { ids -> navController.navigate(WtfRoutes.result(ids)) },
                        // TODO: navigate to OOTD record creation once the records feature ships.
                        onAddOotd = {},
                    )
                }
                composable(WtfRoutes.CLOTHES) {
                    ClothesScreen(
                        onAddItem = { navController.navigate(WtfRoutes.itemEdit(0L)) },
                        onEditItem = { id -> navController.navigate(WtfRoutes.itemEdit(id)) },
                        itemListViewModel = searchViewModel,
                    )
                }
                composable(
                    route = WtfRoutes.ITEM_EDIT_PATTERN,
                    arguments = listOf(
                        navArgument(WtfRoutes.ARG_ITEM_ID) {
                            type = NavType.LongType
                            defaultValue = 0L
                        },
                    ),
                ) {
                    ItemEditScreen(onDone = { navController.popBackStack() })
                }
                composable(
                    route = WtfRoutes.RESULT_PATTERN,
                    arguments = listOf(
                        navArgument(WtfRoutes.ARG_CATEGORY_IDS) {
                            type = NavType.StringType
                            defaultValue = ""
                        },
                    ),
                ) {
                    ResultScreen(onBack = { navController.popBackStack() })
                }
                composable(WtfRoutes.SETTINGS) {
                    SettingsScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}

/**
 * App-wide search bar shown on the top-level destinations. A 48dp pill (Google-Contacts feel) with
 * the search icon leading and the settings avatar trailing (it morphs to a clear ✕ while typing).
 * Per M3 Expressive, the side margin animates from 24dp at rest to 12dp when focused. Typing filters
 * the clothes list (visible on the Wardrobe tab).
 */
@Composable
private fun WardrobeSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val sideMargin by animateDpAsState(
        targetValue = if (focused) 12.dp else 24.dp,
        label = "searchBarMargin",
    )

    Box(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = sideMargin, vertical = 8.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth()
                .height(SearchBarHeight),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(16.dp))
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(12.dp))
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(R.string.item_search_hint),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focused = it.isFocused },
                    )
                }
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.action_search_clear),
                        )
                    }
                } else {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.action_settings),
                        )
                    }
                }
                Spacer(Modifier.width(4.dp))
            }
        }
    }
}

@Preview(name = "Search bar — empty", showBackground = true)
@Composable
private fun WardrobeSearchBarEmptyPreview() {
    WTFTheme(dynamicColor = false) {
        WardrobeSearchBar(query = "", onQueryChange = {}, onOpenSettings = {})
    }
}

@Preview(name = "Search bar — typing", showBackground = true)
@Composable
private fun WardrobeSearchBarTypingPreview() {
    WTFTheme(dynamicColor = false) {
        WardrobeSearchBar(query = "Uniqlo", onQueryChange = {}, onOpenSettings = {})
    }
}
