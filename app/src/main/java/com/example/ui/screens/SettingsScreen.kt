package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.data.AuthManager
import com.example.domain.AttendXDataSnapshot
import com.example.domain.IsoDate
import com.example.ui.AttendXComputedState
import com.example.ui.components.AttendXCard
import com.example.ui.theme.CriticalCoral
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAttendXColors
import com.example.ui.theme.SafeEmerald
import com.example.ui.theme.VioletPrimary
import com.example.ui.theme.WarningAmber

@Composable
fun SettingsScreen(
    state: AttendXComputedState,
    userEmail: String?,
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit,
    onUpdateDefaultPercent: (Double) -> Unit,
    onExportBackupJson: (AttendXDataSnapshot) -> String,
    onImportBackupJson: (String) -> Unit,
    onResetDemoData: () -> Unit,
    onSignOutOrExitLocal: () -> Unit,
    onGoogleSignInFromLocal: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = LocalAttendXColors.current
    val credentialManager = remember { CredentialManager.create(context) }

    var thresholdSlider by remember(state.snapshot.profile.defaultRequiredPercent) {
        mutableFloatStateOf(state.snapshot.profile.defaultRequiredPercent.toFloat())
    }
    var showResetDialog by remember { mutableStateOf(false) }

    val backupExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                val json = onExportBackupJson(state.snapshot)
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray())
                }
                onShowMessage("Backup JSON exported")
            } catch (e: Exception) {
                onShowMessage("Export failed: ${e.localizedMessage}")
            }
        }
    }

    val backupImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (!text.isNullOrBlank()) {
                    onImportBackupJson(text)
                }
            } catch (e: Exception) {
                onShowMessage("Import failed: ${e.localizedMessage}")
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Appearance: Toggleable Dark Navy Mode
        item {
            AttendXCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.DarkMode, contentDescription = null, tint = colors.accentViolet)
                        Column {
                            Text(
                                text = "Dark Navy Theme",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary
                            )
                            Text(
                                text = if (isDarkTheme) "Dark Navy with Purple & Blue gradients active" else "Light Mode active",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted
                            )
                        }
                    }
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = onToggleDarkTheme,
                        modifier = Modifier.testTag("dark_mode_switch")
                    )
                }
            }
        }

        // Default Minimum Attendance Requirement
        item {
            AttendXCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Default Minimum Attendance",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Applied to new subjects & overall status",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted
                        )
                    }
                    Text(
                        text = "${thresholdSlider.toInt()}%",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = colors.accentCyan
                    )
                }

                Slider(
                    value = thresholdSlider,
                    onValueChange = { thresholdSlider = it },
                    onValueChangeFinished = { onUpdateDefaultPercent(thresholdSlider.toInt().toDouble()) },
                    valueRange = 50f..95f
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(70, 75, 80, 85).forEach { preset ->
                        FilterChip(
                            selected = thresholdSlider.toInt() == preset,
                            onClick = {
                                thresholdSlider = preset.toFloat()
                                onUpdateDefaultPercent(preset.toDouble())
                            },
                            label = { Text("$preset%") }
                        )
                    }
                }
            }
        }

        // Account & Cloud Sync
        item {
            AttendXCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (state.isCloudSynced) Icons.Rounded.CloudDone else Icons.Rounded.PhoneAndroid,
                        contentDescription = null,
                        tint = if (state.isCloudSynced) SafeEmerald else WarningAmber
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (state.isCloudSynced) "Firebase Cloud Sync Active" else "Local Device Storage Mode",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                        Text(
                            text = if (state.isCloudSynced) {
                                "Signed in as ${userEmail ?: state.snapshot.profile.email.ifBlank { "Google Account" }}. Private per-user Firestore sync enabled."
                            } else {
                                "Data is stored only on this device. Sign in with Google to enable private cloud sync."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (state.isCloudSynced) {
                    OutlinedButton(
                        onClick = {
                            AuthManager.signOut(credentialManager, scope) {
                                onSignOutOrExitLocal()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sign_out_button"),
                        border = BorderStroke(1.dp, CriticalCoral.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, tint = CriticalCoral)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Sign Out", color = CriticalCoral)
                    }
                } else {
                    Button(
                        onClick = onGoogleSignInFromLocal,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_google_sign_in_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary)
                    ) {
                        Text("Sign in with Google to Sync")
                    }
                }
            }
        }

        // Backup & Restore + Demo Reset
        item {
            AttendXCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Backup, Restore & Demo Data",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            backupExportLauncher.launch("AttendX_Backup_${IsoDate.today().toIsoString()}.json")
                        },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Icon(Icons.Rounded.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Export JSON")
                    }
                    OutlinedButton(
                        onClick = { backupImportLauncher.launch(arrayOf("application/json", "*/*")) },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Restore JSON")
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { showResetDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reset_demo_data_button"),
                    border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Rounded.RestartAlt, contentDescription = null, tint = WarningAmber)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Reset to Sample ECE Demo Data", color = WarningAmber)
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = colors.cardBg,
            title = { Text("Restore Demo Data?", color = colors.textPrimary) },
            text = {
                Text(
                    "This replaces your current subjects, timetable, and attendance records with the 7 sample courses (Digital Electronics, VLSI Design, etc.).",
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetDemoData()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary)
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            }
        )
    }
}
