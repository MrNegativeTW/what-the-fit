package com.txwstudio.app.whatthefit.ui.categories

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.txwstudio.app.whatthefit.R
import com.txwstudio.app.whatthefit.data.entity.Category
import com.txwstudio.app.whatthefit.data.entity.CategoryWithCount
import com.txwstudio.app.whatthefit.ui.components.FabListBottomPadding
import com.txwstudio.app.whatthefit.ui.theme.WTFTheme
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * Stateful entry point. Owns the [CategoryViewModel] and forwards its state to [CategoryContent].
 * Not previewable: it builds a Hilt ViewModel that reads Room. Preview [CategoryContent].
 */
@Composable
fun CategoryScreen(
    modifier: Modifier = Modifier,
    viewModel: CategoryViewModel = hiltViewModel(),
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    CategoryContent(
        categories = categories,
        onReorder = viewModel::reorder,
        onAdd = viewModel::addCategory,
        onRename = viewModel::renameCategory,
        onDelete = viewModel::deleteCategory,
        modifier = modifier,
    )
}

/** Stateless body. Drag, dialogs and the local reorder copy are view state, so it previews fine. */
@Composable
fun CategoryContent(
    categories: List<CategoryWithCount>,
    onReorder: (List<Long>) -> Unit,
    onAdd: (String) -> Unit,
    onRename: (Long, String) -> Unit,
    onDelete: (Category) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<Category?>(null) }
    var deleteTarget by remember { mutableStateOf<Category?>(null) }

    // Local working copy so drag reordering is smooth; re-synced whenever the DB emits.
    var localList by remember { mutableStateOf(categories) }
    LaunchedEffect(categories) { localList = categories }

    val lazyListState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        localList = localList.toMutableList().apply { add(to.index, removeAt(from.index)) }
    }

    Box(modifier.fillMaxSize()) {
        if (localList.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.category_list_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = FabListBottomPadding),
            ) {
                items(localList, key = { it.category.id }) { entry ->
                    ReorderableItem(reorderState, key = entry.category.id) { isDragging ->
                        CategoryListItem(
                            entry = entry,
                            dragging = isDragging,
                            dragHandle = {
                                Icon(
                                    imageVector = Icons.Default.DragHandle,
                                    contentDescription = stringResource(R.string.category_drag_handle),
                                    modifier = Modifier
                                        .draggableHandle(
                                            onDragStopped = { onReorder(localList.map { it.category.id }) },
                                        )
                                        .padding(end = 4.dp),
                                )
                            },
                            onRename = { renameTarget = entry.category },
                            onDelete = { deleteTarget = entry.category },
                        )
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.category_add))
        }
    }

    if (showAddDialog) {
        TextInputDialog(
            title = stringResource(R.string.category_add_title),
            initial = "",
            onConfirm = { onAdd(it); showAddDialog = false },
            onDismiss = { showAddDialog = false },
        )
    }
    renameTarget?.let { target ->
        TextInputDialog(
            title = stringResource(R.string.action_rename),
            initial = target.name,
            onConfirm = { onRename(target.id, it); renameTarget = null },
            onDismiss = { renameTarget = null },
        )
    }
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.category_delete_title, target.name)) },
            text = { Text(stringResource(R.string.category_delete_body)) },
            confirmButton = {
                TextButton(onClick = { onDelete(target); deleteTarget = null }) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun CategoryListItem(
    entry: CategoryWithCount,
    dragging: Boolean,
    dragHandle: @Composable () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val elevation = if (dragging) 6.dp else 0.dp
    ListItem(
        headlineContent = { Text(entry.category.name) },
        supportingContent = {
            Text(pluralStringResource(R.plurals.category_item_count, entry.itemCount, entry.itemCount))
        },
        leadingContent = dragHandle,
        trailingContent = {
            Row {
                IconButton(onClick = onRename) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.action_rename))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
                }
            }
        },
        tonalElevation = elevation,
        shadowElevation = elevation,
        colors = ListItemDefaults.colors(),
    )
}

@Composable
private fun TextInputDialog(
    title: String,
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.trim()) }, enabled = text.isNotBlank()) {
                Text(stringResource(R.string.action_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

private val sampleCategories = listOf(
    CategoryWithCount(Category(id = 1, name = "帽子"), itemCount = 3),
    CategoryWithCount(Category(id = 2, name = "上衣"), itemCount = 12),
    CategoryWithCount(Category(id = 3, name = "外套"), itemCount = 5),
    CategoryWithCount(Category(id = 4, name = "褲子"), itemCount = 7),
)

@Preview(name = "Categories", showBackground = true)
@Composable
private fun CategoryContentPreview() {
    WTFTheme(dynamicColor = false) {
        CategoryContent(
            categories = sampleCategories,
            onReorder = {},
            onAdd = {},
            onRename = { _, _ -> },
            onDelete = {},
        )
    }
}
