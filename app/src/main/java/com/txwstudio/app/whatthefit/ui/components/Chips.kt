package com.txwstudio.app.whatthefit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.txwstudio.app.whatthefit.R
import com.txwstudio.app.whatthefit.data.Season
import com.txwstudio.app.whatthefit.data.entity.Category
import com.txwstudio.app.whatthefit.data.entity.Tag

@Composable
private fun seasonLabel(season: Int): String = stringResource(
    when (season) {
        Season.SPRING -> R.string.season_spring
        Season.SUMMER -> R.string.season_summer
        Season.AUTUMN -> R.string.season_autumn
        Season.WINTER -> R.string.season_winter
        else -> R.string.season_spring
    },
)

/** Multi-select chips for assigning a clothing item to one or more categories. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryChips(
    categories: List<Category>,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.forEach { category ->
            FilterChip(
                selected = category.id in selectedIds,
                onClick = { onToggle(category.id) },
                label = { Text(category.name) },
            )
        }
    }
}

/** Multi-select chips for the four season tags (labels only — no generation filtering). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SeasonChips(
    selectedSeasons: Set<Int>,
    onToggle: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Season.ENTRIES.forEach { season ->
            FilterChip(
                selected = season in selectedSeasons,
                onClick = { onToggle(season) },
                label = { Text(seasonLabel(season)) },
            )
        }
    }
}

/** Multi-select chips for configurable [Tag]s (brand / color / occasion), with optional swatch. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagChips(
    tags: List<Tag>,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        tags.forEach { tag ->
            val swatch = tag.swatchArgb
            FilterChip(
                selected = tag.id in selectedIds,
                onClick = { onToggle(tag.id) },
                label = { Text(tag.name) },
                leadingIcon = if (swatch != null) {
                    { ColorSwatch(argb = swatch) }
                } else {
                    null
                },
            )
        }
    }
}

@Composable
fun ColorSwatch(argb: Long, modifier: Modifier = Modifier, size: Dp = 16.dp) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(argb))
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
    )
}
