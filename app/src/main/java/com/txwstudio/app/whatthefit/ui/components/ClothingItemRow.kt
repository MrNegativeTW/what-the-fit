package com.txwstudio.app.whatthefit.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.txwstudio.app.whatthefit.R
import com.txwstudio.app.whatthefit.data.entity.ItemWithDetails
import com.txwstudio.app.whatthefit.domain.model.TagKind

/**
 * One clothing item as a list row: name with inline color swatches and a "brand · parts" subtitle.
 * [trailing] fills the end of the row; the wardrobe list passes an availability Switch, search passes
 * nothing for a read-only row.
 */
@Composable
fun ClothingItemRow(
    item: ItemWithDetails,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.item.name, style = MaterialTheme.typography.bodyLarge)
                val swatches = item.tags.mapNotNull { tag ->
                    tag.swatchArgb.takeIf { tag.kind == TagKind.COLOR }
                }
                if (swatches.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        swatches.forEach { ColorSwatch(argb = it, size = 12.dp) }
                    }
                }
            }
            val categoryText = if (item.categories.isEmpty()) {
                stringResource(R.string.item_uncategorized)
            } else {
                item.categories.joinToString("、") { it.name }
            }
            val brandText =
                item.tags.filter { it.kind == TagKind.BRAND }.joinToString(" / ") { it.name }
            Text(
                text = if (brandText.isNotBlank()) "$brandText · $categoryText" else categoryText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        trailing?.invoke()
    }
}
