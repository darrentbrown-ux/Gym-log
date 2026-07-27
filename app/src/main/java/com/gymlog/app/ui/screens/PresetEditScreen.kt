package com.gymlog.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.gymlog.app.data.Exercise
import com.gymlog.app.ui.GymLogViewModel
import com.gymlog.app.ui.components.ScreenTopBar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetEditScreen(navController: NavHostController, padding: PaddingValues, presetId: Long) {
    val vm: GymLogViewModel = viewModel()
    val scope = rememberCoroutineScope()
    var presetName by remember { mutableStateOf("") }
    val items by vm.presetExercises(presetId).collectAsState(initial = emptyList())
    val allExercises by vm.exercises.collectAsState(initial = emptyList())

    LaunchedEffect(presetId) {
        presetName = vm.getPreset(presetId)?.name.orEmpty()
    }

    var showAdd by remember { mutableStateOf(false) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var pickedExercise by remember { mutableStateOf<Exercise?>(null) }
    var defWeight by remember { mutableStateOf("") }
    var defReps by remember { mutableStateOf("") }
    var defSets by remember { mutableStateOf("3") }

    Scaffold(
        topBar = { ScreenTopBar("Edit: $presetName", onBack = { navController.popBackStack() }) }
    ) { inner ->
        Column(modifier = Modifier.fillMaxSize().padding(inner).padding(16.dp)) {
            if (items.isEmpty()) {
                EmptyHint("No exercises yet. Tap + Add exercise below.")
            }
            LazyColumn {
                items(items, key = { it.presetExerciseId }) { item ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.exerciseName, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    buildString {
                                        if (item.defaultWeight != null) append("${item.defaultWeight} lb × ")
                                        append("${item.defaultReps ?: "?"} reps")
                                        append(" • ${item.defaultSets} sets")
                                    },
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            IconButton(onClick = {
                                scope.launch { vm.removePresetExercise(
                                    com.gymlog.app.data.PresetExercise(id = item.presetExerciseId, presetId = presetId, exerciseId = item.exerciseId)
                                ) }
                            }) { Icon(Icons.Filled.Close, contentDescription = "Remove") }
                        }
                    }
                }
            }
            Button(
                onClick = { showAdd = true },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            ) { Text("+ Add exercise") }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = {
                showAdd = false
                pickedExercise = null
                defWeight = ""; defReps = ""; defSets = "3"
            },
            title = { Text("Add exercise to routine") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = pickedExercise?.name ?: "Select exercise",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(dropdownExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        allExercises.forEach { ex ->
                            DropdownMenuItem(
                                text = { Text("${ex.name} (${ex.category.label})") },
                                onClick = { pickedExercise = ex; dropdownExpanded = false }
                            )
                        }
                    }
                    OutlinedTextField(defWeight, { defWeight = it }, label = { Text("Default weight (lb)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(defReps, { defReps = it }, label = { Text("Default reps") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(defSets, { defSets = it }, label = { Text("Default sets") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    enabled = pickedExercise != null,
                    onClick = {
                        val ex = pickedExercise ?: return@Button
                        scope.launch {
                            vm.addPresetExercise(
                                presetId = presetId,
                                exerciseId = ex.id,
                                defaultWeight = defWeight.toDoubleOrNull(),
                                defaultReps = defReps.toIntOrNull(),
                                defaultSets = defSets.toIntOrNull() ?: 3
                            )
                            showAdd = false
                            pickedExercise = null
                            defWeight = ""; defReps = ""; defSets = "3"
                        }
                    }
                ) { Text("Add") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAdd = false }) { Text("Cancel") }
            }
        )
    }
}
