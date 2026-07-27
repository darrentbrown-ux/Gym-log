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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.gymlog.app.data.ExerciseCategory
import com.gymlog.app.ui.GymLogViewModel
import com.gymlog.app.ui.components.ScreenTopBar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseNewScreen(navController: NavHostController, padding: PaddingValues) {
    val vm: GymLogViewModel = viewModel()
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ExerciseCategory.FREE_WEIGHTS) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    val settings = remember { mutableStateListOf<String>() }
    var newSetting by remember { mutableStateOf("") }

    Scaffold(
        topBar = { ScreenTopBar("New exercise", onBack = { navController.popBackStack() }) }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Name (e.g. Hip Abductor)") },
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = !dropdownExpanded }
            ) {
                OutlinedTextField(
                    value = category.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(dropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExerciseCategory.values().forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(cat.label) },
                        onClick = {
                            category = cat
                            dropdownExpanded = false
                        }
                    )
                }
            }

            OutlinedTextField(
                value = notes, onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            // Settings (e.g. seat height, incline, speed)
            Text("Machine settings (optional)", style = MaterialTheme.typography.titleMedium)
            settings.forEachIndexed { idx, current ->
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = current,
                        onValueChange = { settings[idx] = it },
                        label = { Text("Setting name") },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { settings.removeAt(idx) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove")
                    }
                }
            }
            Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newSetting,
                    onValueChange = { newSetting = it },
                    label = { Text("e.g. Seat height") },
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    if (newSetting.isNotBlank()) {
                        settings.add(newSetting.trim())
                        newSetting = ""
                    }
                }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add")
                }
            }

            Button(
                onClick = {
                    scope.launch {
                        val id = vm.addExercise(
                            name = name.trim(),
                            category = category,
                            notes = notes.trim(),
                            settings = settings.toList()
                        )
                        if (id > 0) navController.popBackStack()
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save exercise") }

            // Quick-add chips for the user-described examples
            Text("Quick-add examples", style = MaterialTheme.typography.titleMedium)
            val suggestions = listOf(
                "Tricep" to ExerciseCategory.WEIGHT_MACHINE,
                "Hip Abductor" to ExerciseCategory.WEIGHT_MACHINE,
                "Pull Down" to ExerciseCategory.WEIGHT_MACHINE,
                "Deltoid Fly" to ExerciseCategory.WEIGHT_MACHINE,
                "Treadmill" to ExerciseCategory.CARDIO,
                "Stationary Bike" to ExerciseCategory.CARDIO,
                "Stair Master" to ExerciseCategory.CARDIO,
                "Elliptical" to ExerciseCategory.CARDIO,
                "Pullups" to ExerciseCategory.CALISTHENICS,
                "Dips" to ExerciseCategory.CALISTHENICS,
                "Push Ups" to ExerciseCategory.CALISTHENICS,
                "Situps" to ExerciseCategory.CALISTHENICS,
                "Planks" to ExerciseCategory.CALISTHENICS,
                "Deadlift" to ExerciseCategory.FREE_WEIGHTS,
                "Bicep Curls" to ExerciseCategory.FREE_WEIGHTS
            )
            suggestions.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (n, c) ->
                        AssistChip(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                scope.launch {
                                    vm.addExercise(n, c, "",
                                        if (c == ExerciseCategory.CARDIO) listOf("Speed", "Incline", "Duration") else emptyList())
                                }
                            },
                            label = { Text(n) }
                        )
                    }
                }
            }
        }
    }
}
