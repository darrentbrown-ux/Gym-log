package com.gymlog.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.gymlog.app.data.Exercise
import com.gymlog.app.ui.GymLogViewModel
import com.gymlog.app.ui.components.ScreenTopBar
import kotlinx.coroutines.launch

@Composable
fun ExerciseDetailScreen(navController: NavHostController, padding: PaddingValues, exerciseId: Long) {
    val vm: GymLogViewModel = viewModel()
    val scope = rememberCoroutineScope()
    var exercise by remember { mutableStateOf<Exercise?>(null) }
    val settings = remember { mutableStateListOf<String>() }
    var newSetting by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(exerciseId) {
        exercise = vm.getExercise(exerciseId)
        val current = vm.settingsSnapshot(exerciseId).map { it.name }
        settings.clear()
        settings.addAll(current)
    }

    Scaffold(
        topBar = {
            ScreenTopBar(
                title = exercise?.name ?: "Edit",
                onBack = { navController.popBackStack() },
                actions = {
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                }
            )
        }
    ) { inner ->
        val ex = exercise ?: return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Category: ${ex.category.label}", style = MaterialTheme.typography.titleMedium)
            if (ex.notes.isNotBlank()) {
                Text("Notes: ${ex.notes}", style = MaterialTheme.typography.bodyMedium)
            }

            Text("Machine settings", style = MaterialTheme.typography.titleMedium)
            Text(
                "Tap + below to add setting fields like \"Seat height\", \"Arm position\", \"Speed\", \"Incline\".",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            settings.forEachIndexed { idx, current ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = current,
                        onValueChange = { settings[idx] = it },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { settings.removeAt(idx) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove")
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newSetting,
                    onValueChange = { newSetting = it },
                    label = { Text("New setting") },
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    if (newSetting.isNotBlank()) {
                        settings.add(newSetting.trim()); newSetting = ""
                    }
                }) { Icon(Icons.Filled.Add, contentDescription = "Add") }
            }

            Button(
                onClick = {
                    scope.launch {
                        vm.updateExercise(ex.copy(), settings.toList())
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save settings") }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete exercise?") },
            text = { Text("This will also remove it from any presets and past sessions.") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDelete = false
                        scope.launch {
                            exercise?.let { vm.deleteExercise(it) }
                            navController.popBackStack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                OutlinedButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }
}
