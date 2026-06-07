package com.txwstudio.app.whatthefit.ui.clothing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.txwstudio.app.whatthefit.R
import com.txwstudio.app.whatthefit.data.entity.ItemWithDetails
import com.txwstudio.app.whatthefit.ui.components.ClothingItemRow

// M3 "Expand" list shape language: the group's outer corners are large, the seams between connected
// segments are small, and segments sit a hair apart instead of being divided by a rule.
private val GroupCornerLarge = 24.dp
private val GroupCornerSmall = 4.dp
private val GroupGap = 2.dp

/**
 * A collapsible part section rendered as an M3 connected group: a rounded header card over an
 * [AnimatedVisibility] stack of rounded item cards. Stateless — the caller owns [expanded] and
 * supplies the already-grouped [items]. Reuses [ClothingItemRow] for each row.
 */
@Composable
fun ClothingSection(
    title: String,
    count: Int,
    expanded: Boolean,
    items: List<ItemWithDetails>,
    onToggleExpanded: () -> Unit,
    onItemClick: (Long) -> Unit,
    onToggleAvailable: (Long, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val showItems = expanded && items.isNotEmpty()
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "chevronRotation",
    )
    val stateDesc = stringResource(
        if (expanded) R.string.cd_section_expanded else R.string.cd_section_collapsed,
    )
    val headerShape = if (showItems) {
        RoundedCornerShape(
            topStart = GroupCornerLarge,
            topEnd = GroupCornerLarge,
            bottomStart = GroupCornerSmall,
            bottomEnd = GroupCornerSmall,
        )
    } else {
        RoundedCornerShape(GroupCornerLarge)
    }

    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Surface(
            onClick = onToggleExpanded,
            shape = headerShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { stateDescription = stateDesc },
        ) {
            Row(
                modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = pluralStringResource(R.plurals.category_item_count, count, count),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.rotate(chevronRotation),
                    )
                }
            }
        }
        AnimatedVisibility(visible = showItems) {
            Column {
                items.forEachIndexed { index, item ->
                    Spacer(Modifier.height(GroupGap))
                    val bottomCorner =
                        if (index == items.lastIndex) GroupCornerLarge else GroupCornerSmall
                    Surface(
                        shape = RoundedCornerShape(
                            topStart = GroupCornerSmall,
                            topEnd = GroupCornerSmall,
                            bottomStart = bottomCorner,
                            bottomEnd = bottomCorner,
                        ),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        ClothingItemRow(
                            item = item,
                            onClick = { onItemClick(item.item.id) },
                            showParts = false,
                            trailing = {
                                Switch(
                                    checked = item.item.isAvailable,
                                    onCheckedChange = { onToggleAvailable(item.item.id, it) },
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}
