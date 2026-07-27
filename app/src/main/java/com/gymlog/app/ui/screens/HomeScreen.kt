package com.gymlog.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.gymlog.app.ui.GymLogViewModel
import com.gymlog.app.ui.Screen
import com.gymlog.app.ui.components.ScreenTopBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(navController: NavHostController, padding: PaddingValues) {
    val vm: GymLogViewModel = viewModel()
    val sessions by vm.sessions.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        ScreenTopBar(
            title = "Gym Log",
            actions = {
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
                        items(sessions.take(5)) { s ->
                            SessionListRow(s.date, s.name, onClick = { navController.navigate(Screen.SessionDetail.build(s.id)) })
                        }
                    }
                }
            }
        }
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

@Composable
fun SessionListRow(dateMillis: Long, name: String, onClick: () -> Unit) {
    val fmt = remember(dateMillis) { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(name.ifBlank { "(Untitled)" }, style = MaterialTheme.typography.titleMedium)
            Text(fmt.format(Date(dateMillis)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
