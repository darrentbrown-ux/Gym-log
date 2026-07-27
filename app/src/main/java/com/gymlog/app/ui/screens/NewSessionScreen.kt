package com.gymlog.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.gymlog.app.ui.GymLogViewModel
import com.gymlog.app.ui.Screen
import com.gymlog.app.ui.components.DropdownField
import com.gymlog.app.ui.components.ScreenTopBar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSessionScreen(navController: NavHostController, padding: PaddingValues, presetId: Long?) {
    val vm: GymLogViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val presets by vm.presets.collectAsState(initial = emptyList())

    // Pre-selected preset (passed in from a "Start from this routine" button)
    var selectedPresetId by remember { mutableStateOf(presetId) }
    var expanded by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    // When the preset list arrives, default the session name to the preset name (only
    // if the user hasn't typed anything yet). This used to be a one-shot initial value
    // captured in `remember`, but `presets` starts as `emptyList()` from
    // `collectAsState(initial=...)`, so the first composition always produced "" and
    // `name` got stuck on "". A `LaunchedEffect` that reacts when presets arrive fixes
    // it.
    LaunchedEffect(presets, presetId) {
        if (name.isNotBlank() || presetId == null) return@LaunchedEffect
        val preset = presets.firstOrNull { it.id == presetId } ?: return@LaunchedEffect
        name = preset.name
    }
    val dateLabel = remember {
        SimpleDateFormat("EEE, MMM d, yyyy • h:mm a", Locale.getDefault())
            .format(Date())
    }

    Scaffold(topBar = { ScreenTopBar("Start workout", onBack = { navController.popBackStack() }) }) { inner ->
        Column(
            modifier = Modifier.fillMaxSize().padding(inner).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(dateLabel, style = MaterialTheme.typography.bodyLarge)

            DropdownField(
                label = "Pre-fill from routine",
                value = presets.firstOrNull { it.id == selectedPresetId }?.name ?: "Empty / blank routine",
                options = listOf("None — start blank") + presets.map { it.name },
                onSelected = { picked ->
                    if (picked == "None — start blank") {
                        selectedPresetId = null
                    } else {
                        val p = presets.first { it.name == picked }
                        selectedPresetId = p.id
                        if (name.isBlank()) name = p.name
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Session name (e.g. Morning push)") },
                modifier = Modifier.fillMaxWidth()
            )

            Card(colors = CardDefaults.outlinedCardColors(), modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Tip: weights, reps, and machine settings are editable per set. Add exercises anytime during the workout.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = {
                    scope.launch {
                        val id = vm.buildSessionFromPreset(name.ifBlank { "Workout" }, selectedPresetId)
                        navController.navigate(Screen.SessionDetail.build(id)) {
                            popUpTo(Screen.Home.route)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = true
            ) { Text("Start workout") }
        }
    }
}
