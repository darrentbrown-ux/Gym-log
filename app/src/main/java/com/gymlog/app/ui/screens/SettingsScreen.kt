package com.gymlog.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.gymlog.app.data.Repository
import com.gymlog.app.ui.GymLogViewModel
import com.gymlog.app.ui.components.ScreenTopBar
import kotlinx.coroutines.launch

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

    Scaffold(
        topBar = { ScreenTopBar(title = "Settings", onBack = { navController.popBackStack() }) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { inner ->
        Column(
            modifier = Modifier.fillMaxSize().padding(inner).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
