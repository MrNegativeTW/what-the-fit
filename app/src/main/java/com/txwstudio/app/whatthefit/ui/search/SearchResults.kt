package com.txwstudio.app.whatthefit.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.txwstudio.app.whatthefit.R
import com.txwstudio.app.whatthefit.data.entity.OotdWithItems
import com.txwstudio.app.whatthefit.ui.components.ClothingItemRow
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Results body for the contained search: a 'Clothes' section then an 'OOTDs' section. Shows nothing
 * until the query is non-blank, and a "no results" message when a non-blank query matches neither.
 */
@Composable
fun SearchResults(
    viewModel: SearchViewModel,
    onOpenClothes: (Long) -> Unit,
    onOpenOotd: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val clothes by viewModel.clothesResults.collectAsStateWithLifecycle()
    val ootds by viewModel.ootdResults.collectAsStateWithLifecycle()

    if (query.isBlank()) return

    if (clothes.isEmpty() && ootds.isEmpty()) {
        Box(
            modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(R.string.search_no_results),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    LazyColumn(modifier.fillMaxSize()) {
        if (clothes.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.search_section_clothes)) }
            items(clothes, key = { "c${it.item.id}" }) { item ->
                ClothingItemRow(item = item, onClick = { onOpenClothes(item.item.id) })
            }
        }
        if (ootds.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.search_section_ootds)) }
            items(ootds, key = { "o${it.record.id}" }) { ootd ->
                OotdResultRow(ootd = ootd, onClick = { onOpenOotd(ootd.record.id) })
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun OotdResultRow(ootd: OotdWithItems, onClick: () -> Unit) {
    val dateLabel = remember(ootd.record.epochDay) {
        LocalDate.ofEpochDay(ootd.record.epochDay)
            .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    }
    val summary = ootd.slots.mapNotNull { it.item?.name }.joinToString("、")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
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
