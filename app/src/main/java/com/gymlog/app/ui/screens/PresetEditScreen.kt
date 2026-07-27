package com.gymlog.app.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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

/**
 * State passed in to pre-fill the AddExerciseToPresetDialog when EDITING an existing
 * PresetExercise. When null, the dialog starts fresh (the Add flow).
 */
data class ExerciseEditPrefill(
    val presetExerciseId: Long,
    val exerciseId: Long,
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
    var showRename by remember { mutableStateOf(false) }
    var renameField by remember { mutableStateOf("") }
    val items by vm.presetExercises(presetId).collectAsState(initial = emptyList())
    val dbExercises by vm.exercises.collectAsState(initial = emptyList())

    LaunchedEffect(presetId) {
        presetName = vm.getPreset(presetId)?.name.orEmpty()
    }

    var showAdd by remember { mutableStateOf(false) }
    var editingPrefill by remember { mutableStateOf<ExerciseEditPrefill?>(null) }

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
                onBack = {
                    // Pop back to the previous screen, which is the Routine detail screen
                    // (the user navigated Edit ← Routine). Using `popBackStack` (no args)
                    // returns us there; we don't need an explicit destination because the
                    // detail screen sits directly on the back stack.
                    navController.popBackStack()
                },
                actions = {
                    // Rename button. Opens the same AlertDialog as PresetDetailScreen
                    // so both flows have a single rename UX.
                    IconButton(onClick = {
                        renameField = presetName.removePrefix("Edit: ")
                        showRename = true
                    }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Rename routine")
                    }
                    // "Done" — returns to the previous screen (Routine screen).
                    // All edits are already persisted inline on every change, so "Done"
                    // just navigates back rather than saving again.
                    TextButton(onClick = { navController.popBackStack() }) {
                        Text("Done")
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp)
        ) {
            if (orderedItems.isEmpty()) {
                EmptyHint("No exercises yet. Tap + Add exercise below.")
            }

            // Long-press a card to pick it up, then drag vertically to reorder. We
            // use a plain Column (not LazyColumn) so the per-card pointerInput for
            // detectDragGesturesAfterLongPress doesn't fight a LazyList scroll
            // detector — that was the source of the v1.3.x "drag always snaps back"
            // bug. Routine lists are typically <30 items, so plain Column with
            // verticalScroll is fine.
            val listScroll = rememberScrollState()
            val draggingId = remember { mutableStateOf<Long?>(null) }
            // Approximate row height; the cards render a 1-line title plus a 1-line
            // summary (≈ 64 dp). We swap when the finger has moved at least half a
            // row past the current position.
            val rowHeightPx = with(androidx.compose.ui.platform.LocalDensity.current) { 64.dp.toPx() }
            val swapThreshold = rowHeightPx / 2f

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(listScroll)
            ) {
                orderedItems.forEachIndexed { idx, item ->
                    // Per-row drag accumulator so the swap fires once per half-row
                    // crossed (rather than firing on every tiny delta).
                    val cumulative = remember(item.presetExerciseId) { mutableStateOf(0f) }
                    PresetExerciseRow(
                        item = item,
                        isDragging = item.presetExerciseId == draggingId.value,
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
                                if (draggingId.value == item.presetExerciseId) {
                                    draggingId.value = null
                                }
                            }
                        },
                        onDragStart = { draggingId.value = item.presetExerciseId },
                        onDragEnd = {
                            draggingId.value = null
                            cumulative.value = 0f
                            // Persist the new order to the DB. Each individual swap
                            // also updates position, but the user may have done a
                            // single drag that swapped multiple pairs — call once at
                            // drag-end to be safe.
                            scope.launch {
                                vm.reorderPresetExercises(
                                    presetId,
                                    orderedItems.map { it.presetExerciseId }
                                )
                            }
                        },
                        onDragVertically = { deltaY ->
                            val fromIdx = orderedItems.indexOfFirst { it.presetExerciseId == item.presetExerciseId }
                            if (fromIdx < 0) return@PresetExerciseRow
                            cumulative.value += deltaY
                            if (kotlin.math.abs(cumulative.value) >= swapThreshold) {
                                val direction = if (cumulative.value < 0) -1 else 1
                                val toIdx = (fromIdx + direction).coerceIn(0, orderedItems.lastIndex)
                                if (toIdx != fromIdx) {
                                    val moving = orderedItems.removeAt(fromIdx)
                                    orderedItems.add(toIdx, moving)
                                }
                                cumulative.value = 0f
                            }
                        },
                        onEdit = {
                            // Open the same dialog in edit mode, pre-filled from this row.
                            editingPrefill = ExerciseEditPrefill(
                                presetExerciseId = item.presetExerciseId,
                                exerciseId = item.exerciseId,
                                name = item.exerciseName,
                                category = item.exerciseCategory,
                                defaultWeight = item.defaultWeight,
                                defaultReps = item.defaultReps,
                                defaultSets = item.defaultSets,
                                settingsDefaults = parseDefaultsFromNotes(item.presetNotes)
                            )
                        }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
            prefill = null,
            onDismiss = { showAdd = false },
            onConfirm = { spec ->
                scope.launch {
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
                    val refreshed = vm.presetExercisesList(presetId)
                    orderedItems.clear()
                    orderedItems.addAll(refreshed)
                    showAdd = false
                }
            }
        )
    }

    editingPrefill?.let { prefill ->
        AddExerciseToPresetDialog(
            dbExercises = dbExercises,
            prefill = prefill,
            onDismiss = { editingPrefill = null },
            onConfirm = { spec ->
                scope.launch {
                    // For EDIT mode we update the existing PresetExercise row in place
                    // rather than inserting a new one. The exerciseId stays the same
                    // (the user can't rename the exercise here — that's the encyclopedia).
                    vm.updatePresetExerciseDefaults(
                        presetExerciseId = prefill.presetExerciseId,
                        presetId = presetId,
                        exerciseId = prefill.exerciseId,
                        defaultWeight = spec.defaultWeight,
                        defaultReps = spec.defaultReps,
                        defaultSets = spec.defaultSets,
                        notes = encodeDefaultsInNotes(spec.settingsDefaults)
                    )
                    val refreshed = vm.presetExercisesList(presetId)
                    orderedItems.clear()
                    orderedItems.addAll(refreshed)
                    editingPrefill = null
                }
            }
        )
    }

    if (showRename) {
        AlertDialog(
            onDismissRequest = { showRename = false },
            title = { Text("Rename routine") },
            text = {
                OutlinedTextField(
                    value = renameField,
                    onValueChange = { renameField = it },
                    label = { Text("Routine name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    enabled = renameField.isNotBlank(),
                    onClick = {
                        scope.launch {
                            val current = vm.getPreset(presetId) ?: return@launch
                            vm.updatePreset(current.copy(name = renameField))
                            presetName = renameField
                            showRename = false
                        }
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRename = false }) { Text("Cancel") }
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
 *
 * Edit interaction: tap the pencil icon to open the dialog pre-filled with this row's
 * current defaults, so the user can tweak weight/reps/sets/settings without removing
 * and re-adding the exercise.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresetExerciseRow(
    item: com.gymlog.app.data.PresetExerciseJoined,
    isDragging: Boolean,
    onRemove: () -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onDragVertically: (deltaY: Float) -> Unit,
    onEdit: () -> Unit
) {
    val borderColor = if (isDragging) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outlineVariant
    val borderWidth = if (isDragging) 2.dp else 1.dp
    val rowElevation = if (isDragging) 8.dp else 1.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(
                width = borderWidth,
                color = borderColor,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
            )
            // Real long-press + drag detection. While dragging, the row gets a coloured
            // border and elevated shadow. Vertical drag delta is forwarded to the
            // parent which swaps this row with its neighbour.
            .pointerInput(item.presetExerciseId) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onDragStart() },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDragVertically(dragAmount.y)
                    }
                )
            },
        elevation = CardDefaults.cardElevation(defaultElevation = rowElevation)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Persistent drag handle on the left so the user knows they can long-press
            // to drag. The handle tints to the primary colour while the row is being
            // dragged.
            Icon(
                Icons.Filled.DragIndicator,
                contentDescription = "Drag to reorder",
                tint = if (isDragging) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.exerciseName, style = MaterialTheme.typography.titleMedium)
                Text(describePresetEntry(item), style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit")
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Close, contentDescription = "Remove")
            }
        }
    }
}

/**
 * Add (or edit) an exercise in a routine.
 *
 * UX:
 *   1. The user picks a *category first* via a row of FilterChips. This is the
 *      primary navigation — the dropdown shows only exercises from the chosen group
 *      plus "Other" for custom-name entry.
 *   2. After picking a category, the dropdown is filled with that category's exercises.
 *   3. The user picks (or types) a name. Then default weight/reps/sets + per-setting
 *      defaults appear.
 *   4. Submit.
 *
 * When `prefill` is non-null, the dialog is in EDIT mode — the category is locked to
 * the pre-filled one, the dropdown shows its name preselected, and the submit button
 * reads "Save changes". The category chips are hidden in this mode (the user can't
 * move an exercise to a different category via the routine edit; that would be the
 * encyclopedia screen's job).
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun AddExerciseToPresetDialog(
    dbExercises: List<Exercise>,
    prefill: ExerciseEditPrefill?,
    onDismiss: () -> Unit,
    onConfirm: (PickedExerciseSpec) -> Unit
) {
    // Selected group.  In EDIT mode, locked to the pre-fill's category.
    var pickedCategory by remember { mutableStateOf<ExerciseCategory?>(prefill?.category) }

    // Exercise name. In EDIT mode, locked to the pre-fill's name.
    var pickedName by remember { mutableStateOf(prefill?.name.orEmpty()) }
    // Display string for the dropdown (includes "Other" marker when the user is typing
    // a custom name).
    var pickedLabel by remember { mutableStateOf(prefill?.name.orEmpty()) }
    // True if the user is manually typing a custom name (not from the library).
    var isCustomName by remember { mutableStateOf(false) }

    var defWeight by remember {
        mutableStateOf(prefill?.defaultWeight?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }.orEmpty())
    }
    var defReps by remember { mutableStateOf(prefill?.defaultReps?.toString().orEmpty()) }
    var defSets by remember {
        mutableStateOf(
            prefill?.let { if (it.category == ExerciseCategory.CARDIO) "" else it.defaultSets.toString() }
                ?: ""
        )
    }
    val settingsVals = remember {
        mutableStateListOf<Pair<String, String>>().apply {
            val initialNames = prefill?.let { it.settingsDefaults.keys.toList() }
                ?: emptyList()
            val defaults = prefill?.settingsDefaults ?: emptyMap()
            initialNames.forEach { add(it to (defaults[it].orEmpty())) }
        }
    }
    var customNameField by remember { mutableStateOf("") }

    // When the user picks a category, seed settings rows from the catalog defaults
    // (unless we're in EDIT mode with pre-fill — those already have their values).
    LaunchedEffect(pickedCategory) {
        if (prefill != null) return@LaunchedEffect
        val cat = pickedCategory ?: return@LaunchedEffect
        val names = ExerciseCatalog.suggestedSettings(cat)
        if (settingsVals.isEmpty()) {
            names.forEach { settingsVals.add(it to "") }
        }
    }

    // Build dropdown options filtered by category. We always add an "Other" option
    // that switches the field into free-text mode.
    val libraryOptions = remember(pickedCategory) {
        val cat = pickedCategory ?: return@remember emptyList<String>()
        val names = ExerciseCatalog.COMMON_BY_CATEGORY[cat].orEmpty()
        // Plus DB-only entries in the same category.
        val libraryLower = names.map { it.lowercase() }.toSet()
        val customDb = dbExercises
            .filter { it.category == cat }
            .filter { it.name.lowercase() !in libraryLower }
            .map { "${it.name} ★" }
        names + customDb + listOf("Other — type custom name…")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (prefill != null) "Edit exercise defaults" else "Add exercise to routine") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // Group picker (hidden in EDIT mode).
                if (prefill == null) {
                    Text("1. Pick a group", style = MaterialTheme.typography.titleSmall)
                    // Use FlowRow so on narrow landscape widths (e.g. Android 16 landscape
                    // emulator @ 2340×10080) all four group chips wrap to a second line
                    // instead of overflowing off the right edge. The previous plain
                    // Row layout was clipping the third/fourth chip on those displays.
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ExerciseCategory.values().forEach { cat ->
                            FilterChip(
                                selected = pickedCategory == cat,
                                onClick = {
                                    pickedCategory = cat
                                    pickedName = ""
                                    pickedLabel = ""
                                    isCustomName = false
                                },
                                label = { Text(cat.label) }
                            )
                        }
                    }
                }

                if (pickedCategory != null) {
                    Text(
                        if (prefill == null) "2. Pick an exercise" else "Exercise",
                        style = MaterialTheme.typography.titleSmall
                    )

                    val dropdownValue = when {
                        isCustomName && customNameField.isNotBlank() ->
                            "$customNameField (custom)"
                        pickedLabel.isNotBlank() -> pickedLabel
                        else -> "Choose from ${libraryOptions.size}…"
                    }

                    DropdownField(
                        label = "Exercise",
                        value = dropdownValue,
                        options = libraryOptions,
                        enabled = prefill == null,  // lock in edit mode
                        onSelected = { picked ->
                            if (picked == "Other — type custom name…") {
                                isCustomName = true
                                pickedLabel = ""
                                pickedName = ""
                            } else {
                                isCustomName = false
                                pickedLabel = picked
                                val cleaned = picked.removeSuffix(" ★")
                                pickedName = cleaned.trim()
                            }
                            // Re-seed settings rows for the new category.
                            if (prefill == null && pickedCategory != null) {
                                val names = ExerciseCatalog.suggestedSettings(pickedCategory!!)
                                settingsVals.clear()
                                names.forEach { settingsVals.add(it to "") }
                            }
                            // Reset form fields on switch.
                            defWeight = ""
                            defReps = ""
                            defSets = if (pickedCategory == ExerciseCategory.CARDIO) "" else "3"
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (isCustomName) {
                        OutlinedTextField(
                            value = customNameField,
                            onValueChange = { customNameField = it; pickedName = it },
                            label = { Text("Custom exercise name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (pickedName.isNotBlank() && pickedCategory != null) {
                    val cat = pickedCategory!!
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        if (prefill == null) "3. Defaults (pre-fill new sets)" else "Defaults",
                        style = MaterialTheme.typography.titleSmall
                    )

                    // Weight field for non-calisthenics, non-cardio (i.e. weight-machine + free-weights).
                    if (ExerciseCatalog.usesWeight(cat) && cat != ExerciseCategory.CARDIO) {
                        OutlinedTextField(
                            value = defWeight,
                            onValueChange = { defWeight = it },
                            label = { Text("Default weight (lb)") },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    // Reps for non-cardio.
                    if (cat != ExerciseCategory.CARDIO) {
                        OutlinedTextField(
                            value = defReps,
                            onValueChange = { defReps = it },
                            label = { Text("Default reps") },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    // Sets for non-cardio.
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

                    // Settings rows: for WEIGHT_MACHINE → Seat height / Arm position;
                    // for CARDIO → Speed / Incline / Duration; etc.
                    val settings = settingsVals.toList()
                    if (settings.isNotEmpty()) {
                        Text(
                            "Settings defaults",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            "Pre-filled values you want for new sets of this exercise.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        settings.forEachIndexed { idx, (name, value) ->
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
            ) { Text(if (prefill != null) "Save changes" else "Add") }
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
    val body = defaults.entries.joinToString(",") { (k, v) ->
        "\"${escapeJson(k)}\":\"${escapeJson(v)}\""
    }
    return "gym_log_defaults:{$body}"
}

/** Decode the JSON envelope produced by [encodeDefaultsInNotes]. */
internal fun parseDefaultsFromNotes(notes: String?): Map<String, String> {
    val raw = notes?.takeIf { it.isNotBlank() }?.let { n ->
        val idx = n.indexOf("gym_log_defaults:")
        if (idx < 0) null else n.substring(idx + "gym_log_defaults:".length).trim()
    } ?: return emptyMap()
    if (!raw.startsWith("{") || !raw.endsWith("}")) return emptyMap()
    val inner = raw.substring(1, raw.length - 1)
    if (inner.isBlank()) return emptyMap()
    val out = mutableMapOf<String, String>()
    val re = Regex("\"([^\"]*)\"\\s*:\\s*\"([^\"]*)\"")
    re.findAll(inner).forEach { m ->
        out[m.groupValues[1]] = m.groupValues[2]
    }
    return out
}

private fun escapeJson(s: String): String =
    s.replace("\\", "\\\\").replace("\"", "\\\"")

/** Returns a one-line summary of a PresetExercise for the routine list. */
private fun describePresetEntry(item: com.gymlog.app.data.PresetExerciseJoined): String {
    val defaults = parseDefaultsFromNotes(item.presetNotes)
    val main = when (item.exerciseCategory) {
        ExerciseCategory.CARDIO -> buildString {
            val speed = defaults["Speed"] ?: item.presetNotesSpeedFallback()
            val incline = defaults["Incline"] ?: item.presetNotesInclineFallback()
            if (speed.isNotBlank()) append("Speed ").append(speed)
            if (incline.isNotBlank()) {
                if (isNotEmpty()) append(" · ")
                append("Incline ").append(incline)
            }
            if (isNotBlank() && item.defaultSets > 1) {
                // Cardio usually has 1 set (one duration row) but keep display for legacy data.
                append(" · ").append(item.defaultSets).append(" set").append(if (item.defaultSets == 1) "" else "s")
            }
        }.trim()
        ExerciseCategory.CALISTHENICS -> {
            val reps = item.defaultReps?.toString() ?: "?"
            "$reps reps · ${item.defaultSets} set${if (item.defaultSets == 1) "" else "s"}"
        }
        ExerciseCategory.WEIGHT_MACHINE, ExerciseCategory.FREE_WEIGHTS -> {
            val w = item.defaultWeight?.let { "${it.toInt()} lb" } ?: "?"
            val r = item.defaultReps?.toString() ?: "?"
            "$w × $r · ${item.defaultSets} set${if (item.defaultSets == 1) "" else "s"}"
        }
    }
    // Append seat height / arm position reminder if present.
    val reminder = listOfNotNull(
        defaults["Seat height"]?.takeIf { it.isNotBlank() }?.let { "Seat $it" },
        defaults["Arm position"]?.takeIf { it.isNotBlank() }?.let { "Arms $it" },
        defaults["Chest pad depth"]?.takeIf { it.isNotBlank() }?.let { "Pad $it" }
    ).joinToString(" · ").takeIf { it.isNotBlank() }
    return if (reminder != null) "$main · $reminder" else main
}

private fun com.gymlog.app.data.PresetExerciseJoined.presetNotesSpeedFallback(): String {
    // If we don't have the parsed setting, don't fake a value — return blank.
    return ""
}

private fun com.gymlog.app.data.PresetExerciseJoined.presetNotesInclineFallback(): String {
    return ""
}
