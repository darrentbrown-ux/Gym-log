package com.gymlog.app.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.input.pointer.pointerInput
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

    // Local ordering so we can do drag-reorder visually, then persist with "Save order".
    val orderedItems = remember { mutableStateListOf<com.gymlog.app.data.PresetExerciseJoined>() }
    LaunchedEffect(items) {
        // First time, take upstream order; otherwise, don't clobber user reorders.
        if (orderedItems.isEmpty()) {
            orderedItems.addAll(items)
        }
    }

    Scaffold(
        topBar = {
            ScreenTopBar(
                "Edit: $presetName",
                onBack = { navController.popBackStack() },
                actions = {
                    // "Done" — returns to the previous screen. All edits are already
                    // persisted (we save inline on every change), so "Done" just navigates
                    // back rather than saving again. The Save-order button below persists
                    // the user-driven reorder.
                    TextButton(onClick = { navController.popBackStack() }) {
                        Text("Done")
                    }
                }
            )
        }
    ) { inner ->
        Column(modifier = Modifier.fillMaxSize().padding(inner).padding(16.dp)) {
            if (orderedItems.isEmpty()) {
                EmptyHint("No exercises yet. Tap + Add exercise below.")
            }
            // Pickup-id for tap-to-swap reorder. Long-press a card → it becomes picked;
            // tapping another card swaps them; tapping the picked card again cancels.
            var pickedPresetExerciseId by remember { mutableStateOf<Long?>(null) }

            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(
                    items = orderedItems,
                    key = { _, it -> it.presetExerciseId }
                ) { idx, item ->
                    val isPicked = item.presetExerciseId == pickedPresetExerciseId
                    PresetExerciseRow(
                        item = item,
                        isPicked = isPicked,
                        onRemove = {
                            scope.launch {
                                vm.removePresetExercise(
                                    PresetExercise(
                                        id = item.presetExerciseId,
                                        presetId = presetId,
                                        exerciseId = item.exerciseId
                                    )
                                )
                                orderedItems.remove(item)
                                if (pickedPresetExerciseId == item.presetExerciseId) {
                                    pickedPresetExerciseId = null
                                }
                            }
                        },
                        onTogglePick = {
                            // Long-press toggles pickup. If a different row is already
                            // picked, cancel that and pick this one instead.
                            pickedPresetExerciseId =
                                if (isPicked) null else item.presetExerciseId
                        },
                        onSwapWithPicked = {
                            val picked = pickedPresetExerciseId
                            when {
                                picked == null -> Unit  // tap on its own does nothing
                                picked == item.presetExerciseId -> pickedPresetExerciseId = null
                                else -> {
                                    val fromIdx = orderedItems.indexOfFirst { it.presetExerciseId == picked }
                                    val toIdx = idx
                                    if (fromIdx >= 0 && fromIdx != toIdx) {
                                        val moving = orderedItems.removeAt(fromIdx)
                                        orderedItems.add(toIdx, moving)
                                    }
                                    pickedPresetExerciseId = null
                                }
                            }
                        },
                        onDragByIndex = { /* unused in tap-to-swap model */ }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Save order icon-only, shown only when order was changed AND there's a preset.
                IconButton(
                    onClick = {
                        scope.launch {
                            vm.reorderPresetExercises(
                                presetId,
                                orderedItems.map { it.presetExerciseId }
                            )
                        }
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Filled.Save,
                        contentDescription = "Save exercise order"
                    )
                }
                Spacer(Modifier.width(12.dp))
                Button(onClick = { showAdd = true }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("Add exercise", modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }

    if (showAdd) {
        AddExerciseToPresetDialog(
            dbExercises = dbExercises,
            onDismiss = { showAdd = false },
            onConfirm = { spec ->
                scope.launch {
                    val exerciseId = vm.ensureExerciseInDb(
                        name = spec.name,
                        category = spec.category,
                        settingDefNames = suggestedSettingNames(spec.name, spec.category)
                    )
                    val newId = vm.addPresetExerciseReturningId(
                        presetId = presetId,
                        exerciseId = exerciseId,
                        defaultWeight = spec.defaultWeight,
                        defaultReps = spec.defaultReps,
                        defaultSets = spec.defaultSets,
                        notes = encodeDefaultsInNotes(spec.settingsDefaults)
                    )
                    // Refresh the local ordered list so the new row appears immediately.
                    // We pull the upstream Flow's current value (not a never-terminating
                    // `flow.collect { … }`, which used to hang here forever and prevent
                    // `showAdd = false` from ever running — meaning the dialog stayed open).
                    val refreshed = vm.presetExercisesList(presetId)
                    orderedItems.clear()
                    orderedItems.addAll(refreshed)
                    showAdd = false
                }
            }
        )
    }
}

/**
 * Card row showing one PresetExercise.
 *
 * Reorder interaction (the reliable one):
 *   - Long-press the card — it becomes "picked" with a coloured border.
 *   - Tap another card to swap positions with it. Tap the picked card again to cancel.
 *   - Long-press a card while one is already picked also cancels.
 *
 * The previous version tried long-press + continuous horizontal drag, which fought with
 * the parent LazyColumn and the row visually snapped back to its slot after each swap.
 * Tap-to-swap is predictable and works in any list layout.
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun PresetExerciseRow(
    item: com.gymlog.app.data.PresetExerciseJoined,
    isPicked: Boolean,
    onRemove: () -> Unit,
    onTogglePick: () -> Unit,
    onSwapWithPicked: () -> Unit,
    onDragByIndex: (direction: Int) -> Unit
) {
    val borderColor = if (isPicked) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outlineVariant
    val borderWidth = if (isPicked) 2.dp else 1.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(
                width = borderWidth,
                color = borderColor,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
            )
            .pointerInput(item.presetExerciseId) {
                detectTapGestures(
                    onLongPress = { onTogglePick() }
                )
            }
            .clickable {
                onSwapWithPicked()
            }
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isPicked) {
                Icon(
                    Icons.Filled.DragIndicator,
                    contentDescription = "Picked",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(item.exerciseName, style = MaterialTheme.typography.titleMedium)
                Text(describePresetEntry(item), style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Close, contentDescription = "Remove")
            }
        }
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
    // Cardio doesn't use "sets"; defaults to blank (so a Treadmill routine item has no
    // meaningless "3 sets" attached to it).
    var defSets by remember { mutableStateOf("") }
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

                        // Reset form fields when picking a new exercise. Strength/calisthenics
                        // default to 3 sets; cardio leaves it blank (duration suffices).
                        defWeight = ""
                        defReps = ""
                        defSets = if (cat == ExerciseCategory.CARDIO) "" else "3"
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
                    // Cardio uses Duration + speed/incline, never "sets".
                    if (cat != ExerciseCategory.CARDIO) {
                        OutlinedTextField(
                            value = defSets,
                            onValueChange = { defSets = it },
                            label = { Text("Default sets") },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

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
                            defaultSets = if (pickedCategory == ExerciseCategory.CARDIO) 1
                                          else (defSets.toIntOrNull() ?: 3),
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
    val main = when (item.exerciseCategory) {
        ExerciseCategory.CARDIO -> {
            val speed = defaults["Speed"] ?: defaults["Resistance"] ?: "—"
            val incline = defaults["Incline"]
            buildList {
                add("Speed $speed")
                if (incline != null) add("Incline $incline")
            }.joinToString(" / ")
        }
        ExerciseCategory.CALISTHENICS -> {
            val sets = "${item.defaultSets} set${if (item.defaultSets != 1) "s" else ""}"
            "${item.defaultReps ?: "—"} reps • $sets"
        }
        else -> {
            val sets = "${item.defaultSets} set${if (item.defaultSets != 1) "s" else ""}"
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
