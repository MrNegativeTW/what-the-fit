package com.txwstudio.app.whatthefit.ui

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExpandedFullScreenContainedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberContainedSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import com.txwstudio.app.whatthefit.ui.navigation.TopLevelDestination
import com.txwstudio.app.whatthefit.ui.navigation.WtfRoutes
import com.txwstudio.app.whatthefit.ui.ootd.OotdEditScreen
import com.txwstudio.app.whatthefit.ui.result.ResultScreen
import com.txwstudio.app.whatthefit.ui.search.SearchResults
import com.txwstudio.app.whatthefit.ui.search.SearchViewModel
import com.txwstudio.app.whatthefit.ui.settings.SettingsScreen
import com.txwstudio.app.whatthefit.ui.theme.WTFTheme
import kotlinx.coroutines.launch

/**
 * Shared duration for screen transitions. The app chrome, the search bar and bottom navigation,
 * animates on this same timeline as the NavHost content so navigating to a detail screen reads as a
 * single motion instead of the chrome being removed before the content finishes fading.
 */
private const val ScreenTransitionMillis = 300

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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

        // On the home/OOTD screen, back once shows a hint and a second back within 2s exits the app.
        val context = LocalContext.current
        var lastBackAt by remember { mutableStateOf(0L) }
        BackHandler(enabled = currentRoute == WtfRoutes.HOME) {
            val now = System.currentTimeMillis()
            if (now - lastBackAt < 2_000L) {
                (context as? Activity)?.finish()
            } else {
                lastBackAt = now
                Toast.makeText(context, context.getString(R.string.exit_press_again), Toast.LENGTH_SHORT)
                    .show()
            }
        }

        // Dedicated search: tapping the bar expands into a full-screen Contained search over clothes
        // and OOTD history; the bar's avatar still opens Settings while collapsed.
        val searchViewModel: SearchViewModel = hiltViewModel()
        val searchState = rememberContainedSearchBarState()
        val textFieldState = rememberTextFieldState()
        val scope = rememberCoroutineScope()
        LaunchedEffect(textFieldState) {
            snapshotFlow { textFieldState.text.toString() }.collect(searchViewModel::onQueryChange)
        }
        val searchExpanded = searchState.targetValue == SearchBarValue.Expanded
        val inputField: @Composable () -> Unit = {
            SearchBarDefaults.InputField(
                textFieldState = textFieldState,
                searchBarState = searchState,
                onSearch = {},
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = {
                    if (searchExpanded) {
                        IconButton(onClick = { scope.launch { searchState.animateToCollapsed() } }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
                            )
                        }
                    } else {
                        Icon(Icons.Default.Search, contentDescription = null)
                    }
                },
                trailingIcon = {
                    if (searchExpanded) {
                        if (textFieldState.text.isNotEmpty()) {
                            IconButton(onClick = { textFieldState.clearText() }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(R.string.action_search_clear),
                                )
                            }
                        }
                    } else {
                        IconButton(onClick = { navController.navigate(WtfRoutes.SETTINGS) }) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = stringResource(R.string.action_settings),
                            )
                        }
                    }
                },
            )
        }

        Scaffold(
            topBar = {
                AnimatedVisibility(
                    visible = topLevel,
                    enter = fadeIn(tween(ScreenTransitionMillis)) +
                            expandVertically(tween(ScreenTransitionMillis), expandFrom = Alignment.Top),
                    exit = fadeOut(tween(ScreenTransitionMillis)) +
                            shrinkVertically(tween(ScreenTransitionMillis), shrinkTowards = Alignment.Top),
                ) {
                    SearchBar(
                        state = searchState,
                        inputField = inputField,
                        colors = SearchBarDefaults.containedColors(searchState),
                        modifier = Modifier
                            .statusBarsPadding()
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = topLevel,
                    enter = fadeIn(tween(ScreenTransitionMillis)) + expandVertically(tween(ScreenTransitionMillis)),
                    exit = fadeOut(tween(ScreenTransitionMillis)) + shrinkVertically(tween(ScreenTransitionMillis)),
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
                        onAddOotd = { navController.navigate(WtfRoutes.ootdEdit()) },
                        onOpenOotd = { id -> navController.navigate(WtfRoutes.ootdEdit(id)) },
                    )
                }
                composable(WtfRoutes.CLOTHES) {
                    ClothesScreen(
                        onAddItem = { navController.navigate(WtfRoutes.itemEdit(0L)) },
                        onEditItem = { id -> navController.navigate(WtfRoutes.itemEdit(id)) },
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
                composable(
                    route = WtfRoutes.OOTD_EDIT_PATTERN,
                    arguments = listOf(
                        navArgument(WtfRoutes.ARG_OOTD_ID) {
                            type = NavType.LongType
                            defaultValue = 0L
                        },
                    ),
                ) {
                    OotdEditScreen(onDone = { navController.popBackStack() })
                }
            }
        }

        // Full-screen Contained search overlay; shows only while the search bar is expanded.
        ExpandedFullScreenContainedSearchBar(
            state = searchState,
            inputField = inputField,
        ) {
            SearchResults(
                viewModel = searchViewModel,
                onOpenClothes = { id ->
                    scope.launch { searchState.animateToCollapsed() }
                    navController.navigate(WtfRoutes.itemEdit(id))
                },
                onOpenOotd = { id ->
                    scope.launch { searchState.animateToCollapsed() }
                    navController.navigate(WtfRoutes.ootdEdit(id))
                },
            )
        }
    }
}
