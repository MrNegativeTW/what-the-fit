package com.txwstudio.app.whatthefit.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.StarRate
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.txwstudio.app.whatthefit.R
import com.txwstudio.app.whatthefit.domain.model.ThemeMode
import com.txwstudio.app.whatthefit.ui.theme.WTFTheme

private val THEME_OPTIONS = listOf(
    ThemeMode.SYSTEM to R.string.theme_system,
    ThemeMode.LIGHT to R.string.theme_light,
    ThemeMode.DARK to R.string.theme_dark,
)

private fun themeLabelRes(mode: ThemeMode): Int =
    THEME_OPTIONS.first { it.first == mode }.second

/**
 * Stateful entry point. Owns the [SettingsViewModel], resolves the system language intent, and
 * derives the current locale label, then forwards plain state to [SettingsContent]. Not
 * previewable: it builds a Hilt ViewModel. Preview [SettingsContent].
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val languageLabel = context.resources.configuration.locales[0]
        .let { locale -> locale.getDisplayName(locale) }
        .replaceFirstChar { it.uppercase() }
    SettingsContent(
        themeMode = themeMode,
        languageLabel = languageLabel,
        versionLabel = viewModel.versionLabel,
        onBack = onBack,
        onSelectTheme = viewModel::setThemeMode,
        onOpenLanguageSettings = { openAppLanguageSettings(context) },
        onRateUs = { openPlayStorePage(context) },
        modifier = modifier,
    )
}

/** Stateless body. Takes plain state plus event callbacks, so it renders in @Preview without Hilt. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    themeMode: ThemeMode,
    languageLabel: String,
    versionLabel: String,
    onBack: () -> Unit,
    onSelectTheme: (ThemeMode) -> Unit,
    onOpenLanguageSettings: () -> Unit,
    onRateUs: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showThemeDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Appearance
            SettingsCard {
                SettingsRow(
                    icon = Icons.Outlined.Palette,
                    title = stringResource(R.string.settings_theme),
                    description = stringResource(themeLabelRes(themeMode)),
                    onClick = { showThemeDialog = true },
                )
                HorizontalDivider(Modifier.padding(start = 16.dp))
                SettingsRow(
                    icon = Icons.Outlined.Translate,
                    title = stringResource(R.string.settings_language),
                    description = languageLabel,
                    onClick = onOpenLanguageSettings,
                )
            }

            // About
            SettingsCard {
                SettingsRow(
                    icon = Icons.Outlined.StarRate,
                    title = stringResource(R.string.settings_rate),
                    description = stringResource(R.string.settings_rate_desc),
                    onClick = onRateUs,
                )
                HorizontalDivider(Modifier.padding(start = 16.dp))
                SettingsRow(
                    icon = Icons.Outlined.Info,
                    title = stringResource(R.string.settings_version),
                    description = versionLabel,
                    onClick = null,
                    showChevron = false,
                )
            }
        }
    }

    if (showThemeDialog) {
        ThemePickerDialog(
            selected = themeMode,
            onSelect = {
                onSelectTheme(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false },
        )
    }
}

/** Rounded card grouping a column of [SettingsRow]s, matching the native Settings layout. */
@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        content = content,
    )
}

/**
 * Single settings row: leading [icon], [title], and [description] subtitle. Clickable when
 * [onClick] is provided, in which case a trailing chevron is shown unless [showChevron] overrides.
 */
@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    description: String?,
    onClick: (() -> Unit)? = null,
    showChevron: Boolean = onClick != null,
) {
    ListItem(
        leadingContent = { Icon(icon, contentDescription = null) },
        headlineContent = { Text(title) },
        supportingContent = description?.let { { Text(it) } },
        trailingContent = if (showChevron) {
            { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) }
        } else {
            null
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
    )
}

/** Single-choice theme picker: System / Light / Dark. Selecting an option applies it immediately. */
@Composable
private fun ThemePickerDialog(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_theme)) },
        text = {
            Column {
                THEME_OPTIONS.forEach { (mode, labelRes) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selected == mode,
                                role = Role.RadioButton,
                                onClick = { onSelect(mode) },
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selected == mode, onClick = null)
                        Text(
                            stringResource(labelRes),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

/** Opens the system per-app language screen (Android 13+), falling back to the app details screen. */
private fun openAppLanguageSettings(context: Context) {
    val uri = Uri.fromParts("package", context.packageName, null)
    val opened = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_APP_LOCALE_SETTINGS, uri))
        }.isSuccess
    } else {
        false
    }
    if (!opened) {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri))
        }
    }
}

/**
 * Opens this app's Google Play listing in the Play Store app, falling back to the web listing.
 * Placeholder target: the listing resolves once the app is published under [Context.getPackageName].
 */
private fun openPlayStorePage(context: Context) {
    val packageName = context.packageName
    val opened = runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
            .setPackage("com.android.vending")
        context.startActivity(intent)
    }.isSuccess
    if (!opened) {
        runCatching {
            val uri = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }
}

@Preview(name = "Settings", showBackground = true)
@Composable
private fun SettingsContentPreview() {
    WTFTheme(dynamicColor = false) {
        SettingsContent(
            themeMode = ThemeMode.SYSTEM,
            languageLabel = "繁體中文",
            versionLabel = "1.0.0 (1)",
            onBack = {},
            onSelectTheme = {},
            onOpenLanguageSettings = {},
            onRateUs = {},
        )
    }
}
