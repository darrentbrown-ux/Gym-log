package com.gymlog.app.ui.screens

import android.content.pm.PackageManager
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.gymlog.app.BuildConfig
import com.gymlog.app.data.Session
import com.gymlog.app.ui.GymLogViewModel
import com.gymlog.app.ui.Screen
import com.gymlog.app.ui.components.ScreenTopBar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(navController: NavHostController, padding: PaddingValues) {
    val vm: GymLogViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val sessions by vm.sessions.collectAsState(initial = emptyList())

    // Confirm-delete state — when set, the AlertDialog asks the user to confirm
    // before we hard-delete a session (and its exercises + sets via FK cascade).
    var confirmDelete by remember { mutableStateOf<Session?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        ScreenTopBar(
            title = "Gym Log",
            actions = {
                // Version label, top-right. We pull it from BuildConfig so it stays in
                // sync with the APK release tag — no manual string updates needed.
                Text(
                    text = "v${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 4.dp)
                )
                IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings")
                }
            }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Quick-start tiles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickAction(
                    modifier = Modifier.weight(1f),
                    title = "Start\nWorkout",
                    icon = Icons.Filled.PlayArrow,
                    onClick = { navController.navigate(Screen.NewSession.build()) }
                )
                QuickAction(
                    modifier = Modifier.weight(1f),
                    title = "Routines",
                    icon = Icons.Filled.CalendarMonth,
                    onClick = { navController.navigate(Screen.Presets.route) }
                )
            }

            Text("Recent workouts", style = MaterialTheme.typography.titleMedium)
            if (sessions.isEmpty()) {
                EmptyHint("No workouts yet. Tap Start to log your first session.")
            } else {
                Card(modifier = Modifier.fillMaxWidth()) {
                    LazyColumn {
                        items(sessions.take(5), key = { it.id }) { s ->
                            // Long-press on a recent workout → confirm-delete dialog.
                            // Short tap → open the workout detail screen as before.
                            SessionListRow(
                                dateMillis = s.date,
                                name = s.name,
                                onClick = { navController.navigate(Screen.SessionDetail.build(s.id)) },
                                onLongClick = { confirmDelete = s }
                            )
                        }
                    }
                }
            }
        }
    }

    // Confirm-delete dialog. We delete via the VM's coroutine scope so the cascade
    // to session_exercises / session_sets happens on the IO dispatcher.
    confirmDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            icon = { Icon(Icons.Filled.Delete, contentDescription = null) },
            title = { Text("Delete this workout?") },
            text = {
                Text(
                    "\"${target.name.ifBlank { "(Untitled)" }}\" on ${
                        SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
                            .format(Date(target.date))
                    } and all its logged sets will be permanently deleted."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val session = target
                    confirmDelete = null
                    scope.launch { vm.deleteSession(session) }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun QuickAction(modifier: Modifier = Modifier, title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.height(36.dp))
            Spacer(Modifier.height(4.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
fun EmptyHint(text: String) {
    Card(colors = CardDefaults.outlinedCardColors()) {
        Text(
            text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Session-list row that supports both short tap (open) and long press (delete).
 * Used by Home (recent workouts) and the History screen.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SessionListRow(
    dateMillis: Long,
    name: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val fmt = remember(dateMillis) { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    // `combinedClickable` is the only Compose modifier that exposes both
    // onClick and onLongClick. It is in ExperimentalFoundationApi so we OptIn.
    val mod = if (onLongClick != null) {
        Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        )
    } else {
        Modifier.combinedClickable(onClick = onClick)
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .then(mod)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(name.ifBlank { "(Untitled)" }, style = MaterialTheme.typography.titleMedium)
            Text(fmt.format(Date(dateMillis)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
