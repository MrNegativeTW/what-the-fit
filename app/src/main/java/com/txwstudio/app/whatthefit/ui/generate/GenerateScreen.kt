package com.txwstudio.app.whatthefit.ui.generate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.txwstudio.app.whatthefit.R
import com.txwstudio.app.whatthefit.data.entity.Category
import com.txwstudio.app.whatthefit.ui.theme.WTFTheme

/**
 * Stateful entry point. Owns the [GenerateViewModel], collects its state, and forwards everything to
 * the stateless [GenerateContent]. This composable is intentionally not previewable: it builds a
 * Hilt ViewModel that touches Room and DataStore. Preview [GenerateContent] instead.
 */
@Composable
fun GenerateScreen(
    onGenerate: (List<Long>) -> Unit,
    onAddOotd: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GenerateViewModel = hiltViewModel(),
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()

    GenerateContent(
        categories = categories,
        selectedIds = selectedIds,
        onToggle = viewModel::toggle,
        onGenerate = onGenerate,
        onAddOotd = onAddOotd,
        modifier = modifier,
    )
}

/** Stateless body. Takes plain state plus event callbacks, so it renders in @Preview without Hilt. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GenerateContent(
    categories: List<Category>,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit,
    onGenerate: (List<Long>) -> Unit,
    onAddOotd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (categories.isEmpty()) {
        Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.generate_empty),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    Box(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.generate_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { category ->
                    FilterChip(
                        selected = category.id in selectedIds,
                        onClick = { onToggle(category.id) },
                        label = { Text(category.name) },
                    )
                }
            }
            // Bottom clearance so the last chips can scroll clear of the floating FAB menu.
            Spacer(Modifier.height(96.dp))
        }

        GenerateFabMenu(
            onRandomize = { onGenerate(categories.filter { it.id in selectedIds }.map { it.id }) },
            onAddOotd = onAddOotd,
            modifier = Modifier.align(Alignment.BottomEnd),
        )
    }
}

/**
 * Medium FAB that expands into a menu. The toggle morphs the AI sparkle into a close mark; the menu
 * offers the outfit generator and an OOTD entry point (stubbed until the records feature ships).
 */
@Composable
private fun GenerateFabMenu(
    onRandomize: () -> Unit,
    onAddOotd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    FloatingActionButtonMenu(
        modifier = modifier,
        expanded = expanded,
        button = {
            ToggleFloatingActionButton(
                checked = expanded,
                onCheckedChange = { expanded = it },
                containerSize = ToggleFloatingActionButtonDefaults.containerSize(),
                containerCornerRadius = ToggleFloatingActionButtonDefaults.containerCornerRadiusMedium(),
            ) {
                val icon by remember {
                    derivedStateOf {
                        if (checkedProgress > 0.5f) Icons.Filled.Close else Icons.Filled.AutoAwesome
                    }
                }
                Icon(
                    painter = rememberVectorPainter(icon),
                    contentDescription = stringResource(R.string.generate_fab_menu_toggle),
                    modifier = with(ToggleFloatingActionButtonDefaults) {
                        Modifier.animateIcon(
                            checkedProgress = { checkedProgress },
                            size = ToggleFloatingActionButtonDefaults.iconSize(),
                        )
                    },
                )
            }
        },
    ) {
        FloatingActionButtonMenuItem(
            onClick = {
                expanded = false
                onRandomize()
            },
            icon = { Icon(Icons.Filled.Casino, contentDescription = null) },
            text = { Text(stringResource(R.string.generate_action)) },
        )
        FloatingActionButtonMenuItem(
            onClick = {
                expanded = false
                onAddOotd()
            },
            icon = { Icon(Icons.Filled.AddAPhoto, contentDescription = null) },
            text = { Text(stringResource(R.string.fab_add_ootd)) },
        )
    }
}

private val sampleCategories = listOf(
    Category(id = 1, name = "帽子"),
    Category(id = 2, name = "上衣"),
    Category(id = 3, name = "外套"),
    Category(id = 4, name = "褲子"),
    Category(id = 5, name = "鞋子"),
)

@Preview(name = "Generate — selection", showBackground = true)
@Composable
private fun GenerateContentPreview() {
    WTFTheme(dynamicColor = false) {
        GenerateContent(
            categories = sampleCategories,
            selectedIds = setOf(2, 4, 5),
            onToggle = {},
            onGenerate = {},
            onAddOotd = {},
        )
    }
}

@Preview(name = "Generate — empty wardrobe", showBackground = true)
@Composable
private fun GenerateContentEmptyPreview() {
    WTFTheme(dynamicColor = false) {
        GenerateContent(
            categories = emptyList(),
            selectedIds = emptySet(),
            onToggle = {},
            onGenerate = {},
            onAddOotd = {},
        )
    }
}
