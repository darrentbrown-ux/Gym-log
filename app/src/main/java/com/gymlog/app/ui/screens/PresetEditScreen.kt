package com.gymlog.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.gymlog.app.data.ExerciseCatalog
import com.gymlog.app.data.ExerciseCategory
import com.gymlog.app.data.MachineSettingDef
import com.gymlog.app.data.PresetExercise
import com.gymlog.app.ui.GymLogViewModel
import com.gymlog.app.ui.components.DropdownField
import com.gymlog.app.ui.components.ScreenTopBar
import kotlinx.coroutines.launch

/** A single picked exercise resolution request — the dialog passes back just the spec. */
data class PickedExerciseSpec(
    val name: String,
    val category: ExerciseCategory,
    val defaultWeight: Double?,
    val defaultReps: Int?,
    val defaultSets: Int,
    val settingsDefaults: Map<String, String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetEditScreen(navController: NavHostController, padding: PaddingValues, presetId: Long) {
    val vm: GymLogViewModel = viewModel()
    val scope = rememberCoroutineScope()
    var presetName by remember { mutableStateOf("") }
    val items by vm.presetExercises(presetId).collectAsState(initial = emptyList())
    val dbExercises by vm.exercises.collectAsState(initial = emptyList())

    LaunchedEffect(presetId) {
        presetName = vm.getPreset(presetId)?.name.orEmpty()
    }

    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { ScreenTopBar("Edit: $presetName", onBack = { navController.popBackStack() }) }
    ) { inner ->
        Column(modifier = Modifier.fillMaxSize().padding(inner).padding(16.dp)) {
            if (items.isEmpty()) {
                EmptyHint("No exercises yet. Tap + Add exercise below.")
            }
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(items, key = { it.presetExerciseId }) { item ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.exerciseName, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    describePresetEntry(item),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            IconButton(onClick = {
                                scope.launch {
                                    vm.removePresetExercise(
                                        PresetExercise(
                                            id = item.presetExerciseId,
                                            presetId = presetId,
                                            exerciseId = item.exerciseId
                                        )
                                    )
                                }
                            }) { Icon(Icons.Filled.Close, contentDescription = "Remove") }
                        }
                    }
                }
            }
            Button(
                onClick = { showAdd = true },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("Add exercise", modifier = Modifier.padding(start = 6.dp))
            }
        }
    }

    if (showAdd) {
        AddExerciseToPresetDialog(
            dbExercises = dbExercises,
            onDismiss = { showAdd = false },
            onConfirm = { spec ->
                scope.launch {
                    // Resolve to a DB row, creating one if needed.
                    val exerciseId = vm.ensureExerciseInDb(
                        name = spec.name,
                        category = spec.category,
                        settingDefNames = suggestedSettingNames(spec.name, spec.category)
                    )
                    vm.addPresetExerciseReturningId(
                        presetId = presetId,
                        exerciseId = exerciseId,
                        defaultWeight = spec.defaultWeight,
                        defaultReps = spec.defaultReps,
                        defaultSets = spec.defaultSets,
                        notes = encodeDefaultsInNotes(spec.settingsDefaults)
                    )
                    showAdd = false
                }
            }
        )
    }
}

/**
 * Dialog that lets the user pick from the *entire* library (50+ common exercises across
 * 4 categories) plus any custom exercises they've added. After picking, fills default
 * weight/reps/sets and per-setting default values, which are saved on the PresetExercise.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExerciseToPresetDialog(
    dbExercises: List<Exercise>,
    onDismiss: () -> Unit,
    onConfirm: (PickedExerciseSpec) -> Unit
) {
    val libraryOptions = remember {
        ExerciseCategory.values().flatMap { cat ->
            ExerciseCatalog.LIBRARY_BY_CATEGORY[cat].orEmpty().map { "${it.name} (${cat.label})" }
        }.distinctBy { it.lowercase() }.sortedBy { it.lowercase() }
    }
    val customOptions = remember(dbExercises) {
        // Only show DB entries that aren't already represented in the library
        val libraryLower = libraryOptions.map { it.lowercase() }.toSet()
        dbExercises
            .filter { "${it.name} (${it.category.label})".lowercase() !in libraryLower }
            .map { "${it.name} (${it.category.label}) ★" }
            .sortedBy { it.lowercase() }
    }
    val allOptions = remember(libraryOptions, customOptions) {
        (libraryOptions + customOptions)
    }

    var pickedLabel by remember { mutableStateOf("") }
    var pickedCategory by remember { mutableStateOf<ExerciseCategory?>(null) }
    var pickedName by remember { mutableStateOf("") }
    var defWeight by remember { mutableStateOf("") }
    var defReps by remember { mutableStateOf("") }
    var defSets by remember { mutableStateOf("3") }
    val settingsVals = remember { mutableStateListOf<Pair<String, String>>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add exercise to routine") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                DropdownField(
                    label = "Exercise (library + any custom)",
                    value = if (pickedLabel.isBlank()) "Choose from ${allOptions.size} exercises\u2026" else pickedLabel,
                    options = allOptions,
                    onSelected = { picked ->
                        pickedLabel = picked
                        // Parse "Name (Category)" suffix; ★ indicates a custom DB-only row.
                        val cleaned = picked.removeSuffix(" \u2605")
                        val match = Regex("^(.*?) \\((.*?)\\)$").find(cleaned)
                        val n = match?.groupValues?.get(1)?.trim() ?: cleaned
                        val catLabel = match?.groupValues?.get(2)?.trim()
                        val cat = ExerciseCategory.values().firstOrNull { it.label == catLabel }
                            ?: ExerciseCategory.WEIGHT_MACHINE
                        pickedName = n
                        pickedCategory = cat

                        // Seed settings rows from whichever source fires first:
                        //  - the existing DB Exercise's MachineSettingDefs (looked up by dbExercises)
                        //  - otherwise the catalog's suggested settings for this category + name
                        val dbRow = dbExercises.firstOrNull { it.name.equals(n, ignoreCase = true) && it.category == cat }
                        // We don't have MachineSettingDefs here; query them async via the VM
                        // would be cleaner but for now use the catalog heuristic. The dialog
                        // shows the *name* fields the user *can* fill; the actual stored defs
                        // are created on Confirm if the row is brand-new.
                        val names = suggestedSettingNames(n, cat)
                        settingsVals.clear()
                        names.forEach { settingsVals.add(it to "") }

                        // Reset form fields when picking a new exercise
                        defWeight = ""
                        defReps = ""
                        defSets = "3"
                        if (dbRow == null) Unit // can't prefill weights from DB without a setter
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (pickedName.isNotBlank() && pickedCategory != null) {
                    val cat = pickedCategory!!
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Cardio uses duration + per-machine settings, never weight/reps.
                    if (cat != ExerciseCategory.CARDIO && cat != ExerciseCategory.CALISTHENICS) {
                        OutlinedTextField(
                            value = defWeight,
                            onValueChange = { defWeight = it },
                            label = { Text("Default weight (lb)") },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    // Calisthenics has reps but no weight.
                    OutlinedTextField(
                        value = defReps,
                        onValueChange = { defReps = it },
                        label = { Text("Default reps") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = defSets,
                        onValueChange = { defSets = it },
                        label = { Text("Default sets") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (settingsVals.isNotEmpty()) {
                        Text(
                            "Settings defaults",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            "Type the values you want pre-filled for new sets of this exercise.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        settingsVals.forEachIndexed { idx, (name, value) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    name,
                                    modifier = Modifier.width(110.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                OutlinedTextField(
                                    value = value,
                                    onValueChange = { v -> settingsVals[idx] = name to v },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("e.g. 5") }
                                )
                            }
                        }
                    } else {
                        Text(
                            "This exercise has no predefined settings. " +
                                "You can add setting fields by editing the exercise after creating it.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = pickedName.isNotBlank() && pickedCategory != null,
                onClick = {
                    val defaults = settingsVals
                        .filter { it.second.isNotBlank() }
                        .toMap()
                    onConfirm(
                        PickedExerciseSpec(
                            name = pickedName,
                            category = pickedCategory!!,
                            defaultWeight = defWeight.toDoubleOrNull(),
                            defaultReps = defReps.toIntOrNull(),
                            defaultSets = defSets.toIntOrNull() ?: 3,
                            settingsDefaults = defaults
                        )
                    )
                }
            ) { Text("Add") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/** Names of setting fields this exercise ought to have, per the catalog. */
private fun suggestedSettingNames(name: String, category: ExerciseCategory): List<String> {
    val base = ExerciseCatalog.suggestedSettings(category)
    val extras = when (name.lowercase()) {
        "treadmill" -> listOf("Speed", "Incline", "Duration")
        "rowing machine", "indoor rower" -> listOf("Speed", "Resistance", "Duration")
        "stair master", "elliptical", "arc trainer" -> listOf("Speed", "Resistance", "Incline")
        else -> emptyList()
    }
    return (base + extras).distinct()
}

/** Encode a defaults map as a JSON-prefix envelope stored in the PresetExercise.notes column. */
private fun encodeDefaultsInNotes(defaults: Map<String, String>): String {
    if (defaults.isEmpty()) return ""
    val obj = org.json.JSONObject()
    defaults.forEach { (k, v) -> obj.put(k, v) }
    return "gym_log_defaults:${obj.toString()}"
}

/**
 * Human-friendly description for the routine list — adapts to category:
 *   CARDIO       → "Speed 2.8 / Incline 6 / 10 min"
 *   CALISTHENICS → "12 reps • 3 sets"
 *   strength     → "100 lb × 15 reps • 3 sets"
 * Appends a defaults line for any saved setting defaults.
 */
private fun describePresetEntry(item: com.gymlog.app.data.PresetExerciseJoined): String {
    val defaults = parseDefaultsFromNotes(item.presetNotes ?: "")
    val sets = "${item.defaultSets} set${if (item.defaultSets != 1) "s" else ""}"
    val main = when (item.exerciseCategory) {
        ExerciseCategory.CARDIO -> {
            val speed = defaults["Speed"] ?: defaults["Resistance"] ?: "—"
            val incline = defaults["Incline"]
            val parts = buildList {
                add("Speed $speed")
                if (incline != null) add("Incline $incline")
            }
            if (parts.isEmpty()) sets else "${parts.joinToString(" / ")} • $sets"
        }
        ExerciseCategory.CALISTHENICS ->
            "${item.defaultReps ?: "—"} reps • $sets"
        else -> {
            val weight = item.defaultWeight
            val reps = item.defaultReps ?: "—"
            if (weight == null) "$reps reps • $sets" else "${weight.toInt()} lb × $reps reps • $sets"
        }
    }
    val otherDefaults = defaults.filterKeys { it !in setOf("Speed", "Resistance", "Incline") }
    return if (otherDefaults.isEmpty()) main
    else main + "\n  " + otherDefaults.entries.joinToString(" / ") { "${it.key}: ${it.value}" }
}

/** Public helper — parsed by the session builder when starting a workout from a preset. */
fun parseDefaultsFromNotes(notes: String?): Map<String, String> {
    if (notes.isNullOrBlank()) return emptyMap()
    if (!notes.startsWith("gym_log_defaults:")) return emptyMap()
    val json = notes.removePrefix("gym_log_defaults:")
    return try {
        val obj = org.json.JSONObject(json)
        buildMap {
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                put(k, obj.optString(k, ""))
            }
        }
    } catch (_: Throwable) { emptyMap() }
}
