package com.gymlog.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.gymlog.app.data.Repository
import com.gymlog.app.ui.GymLogViewModel
import com.gymlog.app.ui.components.ScreenTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(navController: NavHostController, padding: PaddingValues) {
    val vm: GymLogViewModel = viewModel()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val repo = remember { Repository(ctx) }

    fun shareFile(file: java.io.File, mime: String, chooserTitle: String) {
        val uri = repo.shareUri(file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, chooserTitle)
            .also { it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        ctx.startActivity(chooser)
    }

    /**
     * Copy the user-picked content:// URI into the app's cache (so we can hand
     * the resulting File to the repository without holding a ContentResolver
     * reference across suspend boundaries) and then call [vm.importJson] on it.
     * Returns a snackbar message describing the outcome.
     */
    suspend fun importFromUri(uri: Uri): String = withContext(Dispatchers.IO) {
        val temp = java.io.File(ctx.cacheDir, "import_${System.currentTimeMillis()}.json")
        try {
            ctx.contentResolver.openInputStream(uri)?.use { input ->
                temp.outputStream().use { output -> input.copyTo(output) }
            } ?: return@withContext "Import failed: could not open file"
            val summary = vm.importJson(temp)
            "Imported ${summary.exercises} exercises, ${summary.presets} routines, ${summary.sessions} workouts, ${summary.sessionSets} sets"
        } catch (e: Exception) {
            "Import failed: ${e.message}"
        } finally {
            temp.delete()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val msg = importFromUri(uri)
            snackbar.showSnackbar(msg)
        }
    }

    val restSeconds by vm.prefs.restSeconds.collectAsState()
    var restText by remember(restSeconds) { mutableStateOf(restSeconds.toString()) }

    Scaffold(
        topBar = { ScreenTopBar(title = "Settings", onBack = { navController.popBackStack() }) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { inner ->
        Column(
            modifier = Modifier.fillMaxSize().padding(inner).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Workout defaults", style = MaterialTheme.typography.titleMedium)
            Text(
                "Default rest time used by the REST button in a workout.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                OutlinedTextField(
                    value = restText,
                    onValueChange = { v ->
                        // Only digits; keep state in sync, commit to prefs on focus-loss / Save.
                        if (v.isEmpty() || v.all { it.isDigit() }) {
                            restText = v
                        }
                    },
                    label = { Text("Rest time (seconds)") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        val n = restText.toIntOrNull()?.coerceIn(5, 600) ?: 60
                        restText = n.toString()
                        scope.launch { vm.prefs.setRestSeconds(n) }
                    }
                ) { Text("Save") }
            }
            // Quick presets.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(30, 60, 90, 120).forEach { secs ->
                    OutlinedButton(
                        onClick = {
                            restText = secs.toString()
                            scope.launch { vm.prefs.setRestSeconds(secs) }
                        }
                    ) { Text("${secs}s") }
                }
            }

            HorizontalDivider()
            Text("Data exports", style = MaterialTheme.typography.titleMedium)
            Text(
                "All exports are saved to the app's cache and shared via Android's share sheet, so you can save them to Drive, email them, or send them anywhere.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = {
                    scope.launch {
                        runCatching { shareFile(vm.exportCsv(), "text/csv", "Share workout log") }
                            .onFailure { e -> snackbar.showSnackbar("Export failed: ${e.message}") }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Download, contentDescription = null)
                Text("Export workout log (CSV)", modifier = Modifier.padding(start = 8.dp))
            }

            OutlinedButton(
                onClick = {
                    scope.launch {
                        runCatching { shareFile(vm.backupJson(), "application/json", "Share backup") }
                            .onFailure { e -> snackbar.showSnackbar("Backup failed: ${e.message}") }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Save, contentDescription = null)
                Text("Backup user settings (JSON)", modifier = Modifier.padding(start = 8.dp))
            }

            // v1.5.4: import button. Opens the system file picker filtered to JSON
            // files. The selected file is read into the cache, parsed by
            // BackupCodec.fromJson, and inserted as new rows (additive — existing
            // data is preserved, see Repository.importBackup).
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("application/json", "text/json", "text/plain", "*/*")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.UploadFile, contentDescription = null)
                Text("Restore from JSON backup", modifier = Modifier.padding(start = 8.dp))
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("About this format", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "The CSV contains one row per set with date, exercise, weight, reps, settings and notes. " +
                            "The JSON backup includes every exercise, machine setting, preset and session so you can " +
                            "reinstall and restore later.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
