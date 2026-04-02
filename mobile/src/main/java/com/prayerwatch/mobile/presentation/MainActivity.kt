package com.prayerwatch.mobile.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                SettingsScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(viewModel: SettingsViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    // Pull-to-refresh state
    val pullToRefreshState = rememberPullToRefreshState()

    // When the user pulls down, kick off a refresh
    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(Unit) {
            viewModel.refreshPrayerTimes()
        }
    }

    // When syncing finishes, tell the refresh indicator to stop spinning
    LaunchedEffect(uiState.isSyncing) {
        if (!uiState.isSyncing) {
            pullToRefreshState.endRefresh()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Prayer Watch Settings") })
        }
    ) { padding ->
        // Outer Box provides the nestedScroll connection for pull-to-refresh
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .nestedScroll(pullToRefreshState.nestedScrollConnection)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(Modifier.height(8.dp))

                SectionTitle("Location")

                OutlinedTextField(
                    value = uiState.settings.city,
                    onValueChange = viewModel::onCityChanged,
                    label = { Text("City") },
                    placeholder = { Text("e.g. Salt Lake City") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = uiState.settings.country,
                    onValueChange = viewModel::onCountryChanged,
                    label = { Text("Country Code") },
                    placeholder = { Text("e.g. US") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                SectionTitle("Calculation")

                MethodDropdown(
                    selected = uiState.settings.method,
                    onMethodSelected = viewModel::onMethodChanged
                )

                OutlinedTextField(
                    value = uiState.settings.methodSettings,
                    onValueChange = viewModel::onMethodSettingsChanged,
                    label = { Text("Custom Angles (optional)") },
                    placeholder = { Text("e.g. 17.5,null,15") },
                    supportingText = { Text("Fajr angle, Maghrib, Isha — leave blank to use method defaults") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                SectionTitle("Juristic School")

                SchoolRadioGroup(
                    selected = uiState.settings.school,
                    onSchoolSelected = viewModel::onSchoolChanged
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Status row
                if (uiState.lastSyncTime.isNotBlank()) {
                    Text(
                        text = "Last sync: ${uiState.lastSyncTime}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (uiState.syncError != null) {
                    Text(
                        text = "Error: ${uiState.syncError}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (uiState.syncSuccess) {
                    Text(
                        text = "✓ Settings saved & synced to watch",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Button(
                    onClick = viewModel::saveAndSync,
                    enabled = !uiState.isSyncing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Syncing…")
                    } else {
                        Text("Save & Sync to Watch")
                    }
                }

                // Hint to the user about the pull-to-refresh gesture
                Text(
                    text = "↓ Pull down to refresh prayer times",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(Modifier.height(24.dp))
            }

            // Pull-to-refresh indicator overlaid at the top
            PullToRefreshContainer(
                state = pullToRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )
}

// ---- Method Dropdown -------------------------------------------------------

private val METHODS = listOf(
    0 to "Jafari – Leva Institute, Qum",
    1 to "Karachi – Univ. of Islamic Sciences",
    2 to "ISNA – North America",
    3 to "Muslim World League",
    4 to "Umm Al-Qura, Makkah",
    5 to "Egyptian General Authority",
    7 to "Tehran – Univ. of Tehran",
    8 to "Gulf Region",
    9 to "Kuwait",
    10 to "Qatar",
    11 to "Singapore (MUIS)",
    12 to "France – UOIF",
    13 to "Diyanet – Turkey",
    14 to "Russia – SAMR",
    15 to "Moonsighting Committee Worldwide",
    16 to "Dubai (experimental)",
    17 to "Malaysia – JAKIM",
    18 to "Tunisia",
    19 to "Algeria",
    20 to "Indonesia – Kemenag",
    21 to "Morocco",
    22 to "Comunidade Islâmica de Lisboa",
    23 to "Jordan – Ministry of Awqaf"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MethodDropdown(
    selected: Int,
    onMethodSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = METHODS.firstOrNull { it.first == selected }?.second ?: "Method $selected"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text("Calculation Method") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            METHODS.forEach { (id, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onMethodSelected(id)
                        expanded = false
                    }
                )
            }
        }
    }
}

// ---- School Radio ----------------------------------------------------------

@Composable
private fun SchoolRadioGroup(
    selected: Int,
    onSchoolSelected: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        listOf(0 to "Shafi (Standard)", 1 to "Hanafi").forEach { (value, label) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                RadioButton(
                    selected = selected == value,
                    onClick = { onSchoolSelected(value) }
                )
                Text(label, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
