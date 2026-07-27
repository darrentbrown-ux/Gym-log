package com.gymlog.app.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.gymlog.app.data.Exercise
import com.gymlog.app.data.ExerciseCategory
import com.gymlog.app.data.MachineSettingDef
import com.gymlog.app.data.SessionExerciseDetail
import com.gymlog.app.data.SessionSet
import com.gymlog.app.ui.GymLogViewModel
import com.gymlog.app.ui.components.DropdownField
import com.gymlog.app.ui.components.KeyValueRow
import com.gymlog.app.ui.components.ScreenTopBar
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(navController: NavHostController, padding: PaddingValues, sessionId: Long) {
    val vm: GymLogViewModel = viewModel()
    val scope = rememberCoroutineScope()

    val sessionFlow = remember { vm.sessionDetail(sessionId) }
    val sessionDetailList by sessionFlow.collectAsState(initial = emptyList())

    var sessionName by remember { mutableStateOf("Workout") }
    var sessionDate by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(sessionId) {
        vm.getSession(sessionId)?.let {
            sessionName = it.name.ifBlank { "Workout" }
            sessionDate = it.date
        }
    }

    var showAddExercise by remember { mutableStateOf(false) }
    var addExpanded by remember { mutableStateOf(false) }
    var pickedExercise by remember { mutableStateOf<Exercise?>(null) }

    Scaffold(
        topBar = {
            ScreenTopBar(
                title = sessionName,
                onBack = { navController.popBackStack() }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault()).format(Date(sessionDate)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            if (sessionDetailList.isEmpty()) {
                EmptyHint("This workout has no exercises yet. Tap + to add one.")
            }

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(sessionDetailList, key = { it.sessionExerciseId }) { detail ->
                    ExerciseCard(
                        detail = detail,
                        vm = vm,
                        onDeleteExercise = {
                            scope.launch {
                                vm.deleteSessionExercise(
                                    com.gymlog.app.data.SessionExercise(
                                        id = detail.sessionExerciseId,
                                        sessionId = sessionId,
                                        exerciseId = detail.exerciseId
                                    )
                                )
                            }
                        }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            OutlinedButton(
                onClick = { showAddExercise = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Add exercise during workout")
            }
        }
    }

    // ----- Add-exercise dialog -----
    if (showAddExercise) {
        val allExercises by vm.exercises.collectAsState(initial = emptyList())
        AlertDialog(
            onDismissRequest = { showAddExercise = false; pickedExercise = null },
            title = { Text("Add exercise") },
            text = {
                Column {
                    DropdownField(
                        label = "Exercise",
                        value = pickedExercise?.name ?: "Choose…",
                        options = allExercises.map { it.name },
                        onSelected = { picked ->
                            pickedExercise = allExercises.firstOrNull { it.name == picked }
                            addExpanded = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = pickedExercise != null,
                    onClick = {
                        val ex = pickedExercise ?: return@Button
                        scope.launch {
                            vm.addSessionExercise(sessionId, ex.id)
                            pickedExercise = null
                            showAddExercise = false
                        }
                    }
                ) { Text("Add") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddExercise = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ExerciseCard(
    detail: SessionExerciseDetail,
    vm: GymLogViewModel,
    onDeleteExercise: () -> Unit
) {
    val scope = rememberCoroutineScope()
    // Pre-populate rows from the existing sets list so the user can edit rather than re-enter
    val existingSets by vm.setsOf(detail.sessionExerciseId).collectAsState(initial = emptyList())
    val settingDefs by vm.settingsFor(detail.exerciseId).collectAsState(initial = emptyList())

    // Local "rows" mirror the set table.  We always render at least one empty row to add to.
    // Each row tracks: setNumber, reps, weight, settingsValues (JSON string), duration, distance.
    val rows = remember(detail.sessionExerciseId, existingSets) {
        mutableStateListOf<SetRowState>().also { list ->
            if (existingSets.isEmpty()) {
                list.add(SetRowState(setNumber = 1))
            } else {
                existingSets.forEach { list.add(SetRowState.fromExisting(it)) }
            }
        }
    }

    fun saveRow(row: SetRowState) = scope.launch {
        val payload = SetRowState.toSet(row, detail.sessionExerciseId)
        val currentId = row.existingId
        if (currentId != null) {
            vm.updateSet(payload.copy(id = currentId))
        } else {
            val id = vm.addSet(detail.sessionExerciseId, payload)
            row.existingId = id
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(detail.exerciseName, style = MaterialTheme.typography.titleMedium)
                    Text(detail.exerciseCategory.label, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDeleteExercise) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove exercise")
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

            if (settingDefs.isEmpty()) {
                Text(
                    "Tip: edit this exercise to predefine setting fields like Seat height, Incline, etc.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            rows.forEachIndexed { idx, row ->
                SetRowEditor(
                    row = row,
                    setIdx = idx,
                    settingDefs = settingDefs,
                    onChanged = { updated ->
                        rows[idx] = updated
                        saveRow(updated)
                    },
                    onDelete = {
                        scope.launch {
                            row.existingId?.let { vm.deleteSet(com.gymlog.app.data.SessionSet(id = it, sessionExerciseId = detail.sessionExerciseId, setNumber = row.setNumber)) }
                            rows.removeAt(idx)
                            // Re-number subsequent rows so they stay monotonic
                            for (i in rows.indices) rows[i] = rows[i].copy(setNumber = i + 1)
                        }
                    }
                )
            }

            OutlinedButton(
                onClick = {
                    val nextNum = (rows.maxOfOrNull { it.setNumber } ?: 0) + 1
                    val newRow = SetRowState(setNumber = nextNum)
                    rows.add(newRow)
                    saveRow(newRow)
                },
                modifier = Modifier.padding(top = 6.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null); Spacer(Modifier.width(4.dp)); Text("Add set")
            }
        }
    }
}

private class SetRowState(
    val setNumber: Int,
    var reps: String = "",
    var weight: String = "",
    var durationMin: String = "",
    var distance: String = "",
    val settings: MutableMap<String, String> = mutableMapOf(),
    var existingId: Long? = null
) {
    companion object {
        fun fromExisting(s: SessionSet) = SetRowState(
            setNumber = s.setNumber,
            reps = s.reps?.toString().orEmpty(),
            weight = s.weight?.toString().orEmpty(),
            durationMin = s.durationSeconds?.let { (it / 60).toString() }.orEmpty(),
            distance = s.distance?.toString().orEmpty(),
            existingId = s.id,
            settings = parseSettings(s.settingsValues)
        )

        fun toSet(row: SetRowState, sessionExerciseId: Long): SessionSet = SessionSet(
            id = row.existingId ?: 0,
            sessionExerciseId = sessionExerciseId,
            setNumber = row.setNumber,
            reps = row.reps.toIntOrNull(),
            weight = row.weight.toDoubleOrNull(),
            settingsValues = encodeSettings(row.settings),
            durationSeconds = row.durationMin.toIntOrNull()?.let { it * 60 },
            distance = row.distance.toDoubleOrNull(),
            completed = true
        )

        fun parseSettings(json: String): MutableMap<String, String> {
            val out = mutableMapOf<String, String>()
            if (json.isBlank() || json == "{}") return out
            runCatching {
                val obj = JSONObject(json)
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    out[k] = obj.optString(k, "")
                }
            }
            return out
        }

        fun encodeSettings(m: Map<String, String>): String {
            val obj = JSONObject()
            m.forEach { (k, v) -> if (v.isNotBlank()) obj.put(k, v) }
            return obj.toString()
        }
    }

    fun copy(
        setNumber: Int = this.setNumber,
        reps: String = this.reps,
        weight: String = this.weight,
        durationMin: String = this.durationMin,
        distance: String = this.distance,
        settings: Map<String, String> = this.settings
    ): SetRowState {
        val newState = SetRowState(setNumber, reps, weight, durationMin, distance, existingId = existingId)
        newState.settings.putAll(settings)
        return newState
    }
}

@Composable
private fun SetRowEditor(
    row: SetRowState,
    setIdx: Int,
    settingDefs: List<MachineSettingDef>,
    onChanged: (SetRowState) -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text("Set ${row.setNumber}", style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = row.weight,
                onValueChange = { onChanged(row.copy(weight = it, settings = row.settings)) },
                label = { Text("Weight") },
                modifier = Modifier.weight(1f).padding(end = 4.dp),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = row.reps,
                onValueChange = { onChanged(row.copy(reps = it, settings = row.settings)) },
                label = { Text("Reps") },
                modifier = Modifier.weight(1f).padding(start = 4.dp),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete set") }
        }

        // Optional settings (e.g. seat height, incline)
        if (settingDefs.isNotEmpty()) {
            Column(modifier = Modifier.padding(top = 4.dp)) {
                settingDefs.forEach { def ->
                    OutlinedTextField(
                        value = row.settings[def.name].orEmpty(),
                        onValueChange = { v ->
                            row.settings[def.name] = v
                            onChanged(row.copy(settings = row.settings))
                        },
                        label = { Text(def.name) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }

        // Cardio extras
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = row.durationMin,
                onValueChange = { onChanged(row.copy(durationMin = it, settings = row.settings)) },
                label = { Text("Duration (min)") },
                modifier = Modifier.weight(1f).padding(end = 4.dp),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = row.distance,
                onValueChange = { onChanged(row.copy(distance = it, settings = row.settings)) },
                label = { Text("Distance") },
                modifier = Modifier.weight(1f).padding(start = 4.dp),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
    }
}
