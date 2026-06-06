package com.txwstudio.app.whatthefit.ui.generate

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.txwstudio.app.whatthefit.R
import com.txwstudio.app.whatthefit.data.entity.Category
import com.txwstudio.app.whatthefit.data.entity.ClothingItem
import com.txwstudio.app.whatthefit.data.entity.OotdItemCrossRef
import com.txwstudio.app.whatthefit.data.entity.OotdRecord
import com.txwstudio.app.whatthefit.data.entity.OotdSlot
import com.txwstudio.app.whatthefit.data.entity.OotdWithItems
import com.txwstudio.app.whatthefit.ui.theme.WTFTheme
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Stateful entry point. Owns the [GenerateViewModel], collects its state, and forwards everything to
 * the stateless [GenerateContent]. This composable is intentionally not previewable: it builds a
 * Hilt ViewModel that touches Room and DataStore. Preview [GenerateContent] instead.
 */
@Composable
fun GenerateScreen(
    onGenerate: (List<Long>) -> Unit,
    onAddOotd: () -> Unit,
    onOpenOotd: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GenerateViewModel = hiltViewModel(),
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val ootds by viewModel.ootds.collectAsStateWithLifecycle()

    GenerateContent(
        categories = categories,
        ootds = ootds,
        onGenerate = onGenerate,
        onAddOotd = onAddOotd,
        onOpenOotd = onOpenOotd,
        modifier = modifier,
    )
}

/** Stateless body. Takes plain state plus event callbacks, so it renders in @Preview without Hilt. */
@Composable
fun GenerateContent(
    categories: List<Category>,
    ootds: List<OotdWithItems>,
    onGenerate: (List<Long>) -> Unit,
    onAddOotd: () -> Unit,
    onOpenOotd: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (categories.isEmpty()) {
        Box(
            modifier
                .fillMaxSize()
                .padding(32.dp), contentAlignment = Alignment.Center
        ) {
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
                text = stringResource(R.string.ootd_section_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            if (ootds.isEmpty()) {
                Text(
                    text = stringResource(R.string.ootd_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                ootds.forEach { ootd ->
                    OotdHistoryRow(ootd = ootd, onClick = { onOpenOotd(ootd.record.id) })
                    HorizontalDivider()
                }
            }

            // Bottom clearance so content can scroll clear of the floating FAB menu.
            Spacer(Modifier.height(96.dp))
        }

        GenerateFabMenu(
            onRandomize = { onGenerate(categories.map { it.id }) },
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

/** One row in the Outfit-page OOTD history: photo thumbnail, date, and a summary of items. */
@Composable
private fun OotdHistoryRow(
    ootd: OotdWithItems,
    onClick: () -> Unit,
) {
    val dateLabel = remember(ootd.record.epochDay) {
        LocalDate.ofEpochDay(ootd.record.epochDay)
            .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    }
    val summary = ootd.slots.mapNotNull { it.item?.name }.joinToString("、")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            val photo = ootd.record.photoPath
            if (photo != null) {
                AsyncImage(
                    model = File(photo),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    Icons.Default.Checkroom,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(dateLabel, style = MaterialTheme.typography.titleSmall)
            if (summary.isNotEmpty()) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private val sampleCategories = listOf(
    Category(id = 1, name = "帽子"),
    Category(id = 2, name = "上衣"),
    Category(id = 3, name = "外套"),
    Category(id = 4, name = "褲子"),
    Category(id = 5, name = "鞋子"),
)

private val sampleOotds = listOf(
    OotdWithItems(
        record = OotdRecord(id = 1, epochDay = 20000),
        slots = listOf(
            OotdSlot(OotdItemCrossRef(1, 2, 10), Category(2, "上衣"), ClothingItem(10, "白色 T-Shirt")),
            OotdSlot(OotdItemCrossRef(1, 4, 12), Category(4, "褲子"), ClothingItem(12, "黑色長褲")),
        ),
    ),
)

@Preview(name = "Generate — selection", showBackground = true)
@Composable
private fun GenerateContentPreview() {
    WTFTheme(dynamicColor = false) {
        GenerateContent(
            categories = sampleCategories,
            ootds = sampleOotds,
            onGenerate = {},
            onAddOotd = {},
            onOpenOotd = {},
        )
    }
}

@Preview(name = "Generate — empty wardrobe", showBackground = true)
@Composable
private fun GenerateContentEmptyPreview() {
    WTFTheme(dynamicColor = false) {
        GenerateContent(
            categories = emptyList(),
            ootds = emptyList(),
            onGenerate = {},
            onAddOotd = {},
            onOpenOotd = {},
        )
    }
}
