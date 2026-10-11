package com.medsurgery.kiruplus.feature.settings

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medsurgery.kiruplus.BuildConfig
import com.medsurgery.kiruplus.R
import com.medsurgery.kiruplus.core.prefs.AppLanguage
import com.medsurgery.kiruplus.core.prefs.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onSignedOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    onDataExport: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    onTerms: () -> Unit,
    onSubscriptions: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // GENERAL
            SettingsSection(title = stringResource(R.string.settings_section_general)) {
                LanguageRow(
                    selected = prefs.language,
                    onSelect = viewModel::setLanguage,
                )
                HorizontalDivider()
                ThemeRow(
                    selected = prefs.theme,
                    onSelect = viewModel::setTheme,
                )
                HorizontalDivider()
                HapticsRow(
                    enabled = prefs.hapticsEnabled,
                    onToggle = viewModel::setHapticsEnabled,
                )
            }

            // LEGAL
            SettingsSection(title = stringResource(R.string.settings_section_legal)) {
                TextRow(
                    title = stringResource(R.string.legal_privacy_policy),
                    onClick = onPrivacyPolicy,
                )
                HorizontalDivider()
                TextRow(
                    title = stringResource(R.string.legal_terms),
                    onClick = onTerms,
                )
                HorizontalDivider()
                TextRow(
                    title = stringResource(R.string.legal_subscriptions),
                    onClick = onSubscriptions,
                )
            }

            // PRIVACY
            SettingsSection(title = stringResource(R.string.settings_section_privacy)) {
                SentryRow(
                    enabled = prefs.sentryEnabled,
                    onToggle = viewModel::setSentryEnabled,
                )
            }

            // ACCOUNT
            SettingsSection(title = stringResource(R.string.settings_section_account)) {
                TextRow(
                    title = stringResource(R.string.settings_signout),
                    onClick = { viewModel.signOut(onSignedOut) },
                )
                HorizontalDivider()
                TextRow(
                    title = stringResource(R.string.data_export_menu),
                    onClick = onDataExport,
                )
                HorizontalDivider()
                TextRow(
                    title = stringResource(R.string.auth_delete_account),
                    color = MaterialTheme.colorScheme.error,
                    onClick = onDeleteAccount,
                )
            }

            // ABOUT
            val context = LocalContext.current
            SettingsSection(title = stringResource(R.string.settings_section_about)) {
                TextRow(
                    title = stringResource(R.string.settings_support),
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = "mailto:customer_service@medsurgery.academy".toUri()
                            putExtra(Intent.EXTRA_SUBJECT, "KIRU+ Support")
                        }
                        context.startActivity(Intent.createChooser(intent, null))
                    },
                )
                HorizontalDivider()
                TextRow(
                    title = stringResource(R.string.settings_support_store),
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = "mailto:customer_service@medsurgery.store".toUri()
                            putExtra(Intent.EXTRA_SUBJECT, "MedSurgery Store Support")
                        }
                        context.startActivity(Intent.createChooser(intent, null))
                    },
                )
                HorizontalDivider()
                TextRow(
                    title = stringResource(R.string.settings_support_website),
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, BuildConfig.SUPPORT_WEBSITE_URL.toUri())
                        )
                    },
                )
                HorizontalDivider()
                TextRow(
                    title = stringResource(R.string.settings_website),
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, BuildConfig.WEBSITE_URL.toUri())
                        )
                    },
                )
                HorizontalDivider()
                if (BuildConfig.SHOW_ENVIRONMENT_PANEL) {
                    Text(
                        text = stringResource(R.string.settings_environment_title),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                    Text(
                        text = stringResource(
                            R.string.settings_environment_body,
                            BuildConfig.KIRU_BUILD_CHANNEL,
                            BuildConfig.APP_ENVIRONMENT,
                            BuildConfig.SUPABASE_PROJECT_REF,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                    HorizontalDivider()
                }
                Text(
                    text = stringResource(
                        R.string.settings_version,
                        BuildConfig.VERSION_NAME,
                        BuildConfig.VERSION_CODE,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp),
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun LanguageRow(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val labels = mapOf(
        AppLanguage.System to stringResource(R.string.settings_language_system),
        AppLanguage.Spanish to stringResource(R.string.settings_language_es),
        AppLanguage.English to stringResource(R.string.settings_language_en),
    )
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_language),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(R.string.settings_language_restart_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(onClick = { expanded = true }) {
                Text(labels[selected].orEmpty())
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            labels.forEach { (lang, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        expanded = false
                        onSelect(lang)
                    },
                )
            }
        }
    }
}

@Composable
private fun ThemeRow(
    selected: AppTheme,
    onSelect: (AppTheme) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val labels = mapOf(
        AppTheme.System to stringResource(R.string.settings_theme_system),
        AppTheme.Light to stringResource(R.string.settings_theme_light),
        AppTheme.Dark to stringResource(R.string.settings_theme_dark),
    )
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.settings_theme),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(onClick = { expanded = true }) {
                Text(labels[selected].orEmpty())
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            labels.forEach { (theme, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        expanded = false
                        onSelect(theme)
                    },
                )
            }
        }
    }
}

@Composable
private fun HapticsRow(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.settings_haptics),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = enabled, onCheckedChange = onToggle)
    }
}

@Composable
private fun SentryRow(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.settings_sentry_crash_reporting),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(R.string.settings_sentry_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = enabled, onCheckedChange = onToggle)
    }
}

@Composable
private fun TextRow(
    title: String,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
    ) {
        Text(
            text = title,
            color = color,
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
