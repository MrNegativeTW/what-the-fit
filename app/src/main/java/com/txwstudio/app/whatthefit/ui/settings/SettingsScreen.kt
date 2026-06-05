package com.txwstudio.app.whatthefit.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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

/**
 * Stateful entry point. Owns the [SettingsViewModel] and resolves the system language intent, then
 * forwards plain state to [SettingsContent]. Not previewable: it builds a Hilt ViewModel. Preview
 * [SettingsContent].
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val context = LocalContext.current
    SettingsContent(
        themeMode = themeMode,
        versionLabel = viewModel.versionLabel,
        onBack = onBack,
        onSelectTheme = viewModel::setThemeMode,
        onOpenLanguageSettings = { openAppLanguageSettings(context) },
        modifier = modifier,
    )
}

/** Stateless body. Takes plain state plus event callbacks, so it renders in @Preview without Hilt. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    themeMode: ThemeMode,
    versionLabel: String,
    onBack: () -> Unit,
    onSelectTheme: (ThemeMode) -> Unit,
    onOpenLanguageSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Theme
            Text(
                stringResource(R.string.settings_theme),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 12.dp),
            )
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                THEME_OPTIONS.forEachIndexed { index, (mode, labelRes) ->
                    SegmentedButton(
                        selected = themeMode == mode,
                        onClick = { onSelectTheme(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index, THEME_OPTIONS.size),
                    ) {
                        Text(stringResource(labelRes))
                    }
                }
            }

            HorizontalDivider()

            // Language — handled by the system per-app language screen.
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_language)) },
                trailingContent = {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                },
                modifier = Modifier.clickable { onOpenLanguageSettings() },
            )

            HorizontalDivider()

            // Version
            Row(
                modifier = Modifier.padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.settings_version),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    versionLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
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

@Preview(name = "Settings", showBackground = true)
@Composable
private fun SettingsContentPreview() {
    WTFTheme(dynamicColor = false) {
        SettingsContent(
            themeMode = ThemeMode.SYSTEM,
            versionLabel = "1.0.0 (1)",
            onBack = {},
            onSelectTheme = {},
            onOpenLanguageSettings = {},
        )
    }
}
