package com.gymlog.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
import com.gymlog.app.ui.components.ScreenTopBar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    navController: NavHostController,
    padding: PaddingValues,
    sessionId: Long
) {
    val vm: GymLogViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    val sessionFlow = remember { vm.sessionDetail(sessionId) }
    val sessionExerciseList by sessionFlow.collectAsState(initial = emptyList())

    var sessionName by remember { mutableStateOf("Workout") }
    var sessionPresetId by remember { mutableStateOf<Long?>(null) }
    var sessionDate by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(sessionId) {
        vm.getSession(sessionId)?.let {
            sessionName = it.name.ifBlank { "Workout" }
            sessionDate = it.date
            sessionPresetId = it.presetId
        }
    }

    // Local in-memory ordering. Mirrors the Flow but lets us drag-reorder without DB roundtrips.
    val orderedIds = remember { mutableStateListOf<Long>() }
    LaunchedEffect(sessionExerciseList) {
        // Sync: if a new id appears, append; otherwise reset to flow's order minus our edits.
        val flowIds = sessionExerciseList.map { it.sessionExerciseId }
        if (orderedIds.toList() != flowIds && orderedIds.isEmpty()) {
            orderedIds.addAll(flowIds)
        }
    }

    var showAddExercise by remember { mutableStateOf(false) }
    var pickedExercise by remember { mutableStateOf<Exercise?>(null) }
    var selectedIndex by remember { mutableStateOf(-1) }        // exercises index in orderedIds

    val listState = rememberLazyListState()

    fun scrollToIndex(i: Int) {
        selectedIndex = i
        scope.launch {
            listState.animateScrollToItem(i + 1)  // +1 because index 0 is the QuickNav header
        }
    }

    Scaffold(
        topBar = {
            ScreenTopBar(
                title = sessionName,
                onBack = { navController.popBackStack() }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 8.dp)
        ) {
            Text(
                SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault()).format(Date(sessionDate)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )

            if (orderedIds.isEmpty()) {
                EmptyHint("This workout has no exercises yet. Tap + Add exercise below.")
            }

            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                item("quicknav") {
                    QuickNavBar(
                        exercises = orderedIds.mapNotNull { id ->
                            sessionExerciseList.firstOrNull { it.sessionExerciseId == id }
                        },
                        selectedIndex = selectedIndex,
                        onTap = { scrollToIndex(it) },
                        onLongPressDrag = { from, to ->
                            reorderInMemory(orderedIds, from, to)
                        }
                    )
                }
                if (sessionPresetId != null && orderedIds.isNotEmpty()) {
                    item("save-order") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(onClick = {
                                scope.launch {
                                    vm.reorderSessionExercises(sessionId, orderedIds.toList())
                                    sessionPresetId?.let { presetId ->
                                        vm.reorderPresetExercises(presetId, orderedIds.toList())
                                    }
                                    snackbar.showSnackbar("Order saved")
                                }
                            }) {
                                Icon(Icons.Filled.Save, contentDescription = null)
                                Text(" Save to routine", modifier = Modifier.padding(start = 4.dp))
                            }
                        }
                    }
                }
                val detailList: List<SessionExerciseDetail> = orderedIds.mapNotNull { id ->
                    sessionExerciseList.firstOrNull { it.sessionExerciseId == id }
                }
                itemsIndexed(items = detailList, key = { _, d -> d.sessionExerciseId }) { idx, detail ->
                    val isSelected = detail.sessionExerciseId == orderedIds.getOrNull(selectedIndex)
                    ExerciseLogCard(
                        detail = detail,
                        isHighlighted = isSelected && selectedIndex >= 0,
                        vm = vm,
                        sessionId = sessionId,
                        onJumpTop = { scrollToIndex(0) }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            OutlinedButton(
                onClick = { showAddExercise = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Add exercise during workout")
            }
        }
    }

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

/**
 * Top-of-screen scrollable row of chips: each chip = one exercise. Tap to scroll;
 * long-press + drag to reorder (writes only to in-memory state — see Save-to-routine button).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickNavBar(
    exercises: List<SessionExerciseDetail>,
    selectedIndex: Int,
    onTap: (Int) -> Unit,
    onLongPressDrag: (from: Int, to: Int) -> Unit
) {
    val density = LocalDensity.current
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        tonalElevation = 1.dp,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                "Tap to jump · long-press a chip + drag to reorder",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(items = exercises) { idx, detail ->
                    val isDragging = draggingIndex == idx
                    Box(
                        modifier = Modifier
                            .then(
                                if (isDragging) Modifier
                                    .offset(x = dragOffset.x.toInt().dp, y = 0.dp)
                                else Modifier
                            )
                            .pointerInput(detail.sessionExerciseId) {
                                detectTapGestures(onTap = { onTap(idx) })
                            }
                            .pointerInput(detail.sessionExerciseId) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggingIndex = idx
                                        dragOffset = Offset.Zero
                                    },
                                    onDragEnd = {
                                        draggingIndex = null
                                        dragOffset = Offset.Zero
                                    },
                                    onDragCancel = {
                                        draggingIndex = null
                                        dragOffset = Offset.Zero
                                    },
                                    onDrag = { change, drag ->
                                        change.consume()
                                        dragOffset = dragOffset + drag
                                        val thresh = with(density) { 60.dp.toPx() }
                                        if (kotlin.math.abs(dragOffset.x) > thresh) {
                                            val direction = if (dragOffset.x < 0) 1 else -1
                                            val target = (idx + direction).coerceIn(0, exercises.size - 1)
                                            if (target != idx) onLongPressDrag(idx, target)
                                            dragOffset = Offset.Zero
                                        }
                                    }
                                )
                            }
                    ) {
                        AssistChip(
                            onClick = { onTap(idx) },
                            label = { Text(detail.exerciseName) },
                            leadingIcon = if (idx == selectedIndex) {
                                { Icon(Icons.Filled.DragHandle, contentDescription = null) }
                            } else null
                        )
                    }
                }
            }
        }
    }
}

private fun reorderInMemory(list: androidx.compose.runtime.snapshots.SnapshotStateList<Long>, from: Int, to: Int) {
    if (from == to) return
    val v = list.toMutableList()
    val item = v.removeAt(from)
    v.add(to, item)
    list.clear()
    list.addAll(v)
}

/**
 * Per-exercise card with collapsible sets, preferred settings reminder, and per-machine
 * value editor.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseLogCard(
    detail: SessionExerciseDetail,
    isHighlighted: Boolean,
    vm: GymLogViewModel,
    sessionId: Long,
    onJumpTop: () -> Unit
) {
    val scope = rememberCoroutineScope()

    val existingSets by vm.setsOf(detail.sessionExerciseId).collectAsState(initial = emptyList())
    val settingDefs by vm.settingsFor(detail.exerciseId).collectAsState(initial = emptyList())

    // Local rows mirror SessionSet rows. Order matches existingSets, then any new rows.
    val rows = remember(detail.sessionExerciseId, existingSets) {
        mutableStateListOf<SetRowState>().apply {
            if (existingSets.isEmpty()) add(SetRowState(setNumber = 1))
            else existingSets.forEach { add(SetRowState.fromExisting(it)) }
        }
    }

    var settingsExpanded by remember(detail.sessionExerciseId) { mutableStateOf(false) }

    fun saveRow(row: SetRowState) = scope.launch {
        val payload = SetRowState.toSet(row, detail.sessionExerciseId)
        val currentId = row.existingId
        if (currentId != null) vm.updateSet(payload.copy(id = currentId))
        else {
            val id = vm.addSet(detail.sessionExerciseId, payload)
            row.existingId = id
        }
    }

    val borderColor = if (isHighlighted) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outlineVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(detail.exerciseName, style = MaterialTheme.typography.titleMedium)
                    if (settingDefs.any { it.value.isNotBlank() }) {
                        Text(
                            settingDefs.filter { it.value.isNotBlank() }
                                .joinToString(" · ") { "${it.name} ${it.value}" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = {
                    settingsExpanded = !settingsExpanded
                }) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit preferred settings")
                }
                IconButton(onClick = {
                    scope.launch {
                        vm.deleteSessionExercise(
                            com.gymlog.app.data.SessionExercise(
                                id = detail.sessionExerciseId,
                                sessionId = sessionId,
                                exerciseId = detail.exerciseId
                            )
                        )
                    }
                }) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove exercise")
                }
            }

            // Preferred settings reminder + editable values (collab)
            if (settingDefs.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                Text(
                    "Preferred settings on this machine",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                for (def in settingDefs) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            def.name,
                            modifier = Modifier.width(110.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        OutlinedTextField(
                            value = def.value,
                            onValueChange = { v ->
                                scope.launch { vm.updateSettingValue(def, v) }
                            },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("not set") }
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
            Text(
                "Sets",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Sets: the most recent is expanded; older are collapsed (one-line summary).
            rows.forEachIndexed { idx, row ->
                val isCurrent = idx == rows.lastIndex
                if (isCurrent || settingsExpanded) {
                    SetRowEditor(
                        row = row,
                        setIdx = idx,
                        category = detail.exerciseCategory,
                        onChanged = { updated ->
                            rows[idx] = updated
                            saveRow(updated)
                        },
                        onDelete = {
                            scope.launch {
                                row.existingId?.let {
                                    vm.deleteSet(SessionSet(id = it, sessionExerciseId = detail.sessionExerciseId, setNumber = row.setNumber))
                                }
                                rows.removeAt(idx)
                                for (i in rows.indices) rows[i] = rows[i].copy(setNumber = i + 1)
                            }
                        }
                    )
                } else {
                    CollapsedSetRow(
                        row = row,
                        category = detail.exerciseCategory,
                        onToggleComplete = {
                            val next = row.copy(completed = !row.completed)
                            rows[idx] = next
                            scope.launch {
                                val payload = SetRowState.toSet(next, detail.sessionExerciseId)
                                val currentId = next.existingId
                                if (currentId != null) vm.updateSet(payload.copy(id = currentId, completed = next.completed))
                            }
                        },
                        onTapToExpand = {
                            // Move this row to the end of rows list to make it current
                            val snapshot = rows.toList()
                            rows.clear()
                            rows.addAll(snapshot.filterIndexed { i, _ -> i != idx } + snapshot[idx])
                        }
                    )
                }
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
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Add set")
            }
        }
    }
}

@Composable
private fun CollapsedSetRow(
    row: SetRowState,
    category: ExerciseCategory,
    onToggleComplete: () -> Unit,
    onTapToExpand: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .pointerInput(Unit) { detectTapGestures(onTap = { onTapToExpand() }) }
    ) {
        Checkbox(
            checked = row.completed,
            onCheckedChange = { onToggleComplete() }
        )
        Text(
            "Set ${row.setNumber}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            summarize(row, category),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.weight(1f))
        Text(
            "Tap to edit",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun summarize(row: SetRowState, category: ExerciseCategory): String = when (category) {
    ExerciseCategory.CARDIO -> {
        val dur = row.durationMin
        val dist = row.distance
        when {
            dur.isNotBlank() && dist.isNotBlank() -> "$dur min · $dist"
            dur.isNotBlank() -> "$dur min"
            dist.isNotBlank() -> "$dist"
            else -> "Tap to fill in"
        }
    }
    ExerciseCategory.CALISTHENICS -> "${row.reps.ifBlank { "—" }} reps"
    else -> {
        val w = row.weight.ifBlank { "—" }
        val r = row.reps.ifBlank { "—" }
        "$w lb × $r reps"
    }
}

private class SetRowState(
    val setNumber: Int,
    var reps: String = "",
    var weight: String = "",
    var durationMin: String = "",
    var distance: String = "",
    val settings: MutableMap<String, String> = mutableMapOf(),
    var existingId: Long? = null,
    var completed: Boolean = false
) {
    companion object {
        fun fromExisting(s: SessionSet) = SetRowState(
            setNumber = s.setNumber,
            reps = s.reps?.toString().orEmpty(),
            weight = s.weight?.toString().orEmpty(),
            durationMin = s.durationSeconds?.let { (it / 60).toString() }.orEmpty(),
            distance = s.distance?.toString().orEmpty(),
            existingId = s.id,
            settings = parseSettings(s.settingsValues),
            completed = s.completed
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
            completed = row.completed
        )

        fun parseSettings(json: String): MutableMap<String, String> {
            val out = mutableMapOf<String, String>()
            if (json.isBlank() || json == "{}") return out
            runCatching {
                val obj = org.json.JSONObject(json)
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    out[k] = obj.optString(k, "")
                }
            }
            return out
        }

        fun encodeSettings(m: Map<String, String>): String {
            val obj = org.json.JSONObject()
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
        settings: Map<String, String> = this.settings,
        completed: Boolean = this.completed
    ): SetRowState {
        val newState = SetRowState(
            setNumber = setNumber,
            reps = reps,
            weight = weight,
            durationMin = durationMin,
            distance = distance,
            settings = mutableMapOf<String, String>().also { it.putAll(settings) },
            existingId = existingId,
            completed = completed
        )
        newState.settings.putAll(settings)
        return newState
    }
}

@Composable
private fun SetRowEditor(
    row: SetRowState,
    setIdx: Int,
    category: ExerciseCategory,
    onChanged: (SetRowState) -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Set ${row.setNumber}",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Done",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Checkbox(
                    checked = row.completed,
                    onCheckedChange = { onChanged(row.copy(completed = it, settings = row.settings)) }
                )
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete set") }
            }
        }
        // Show weight/reps for strength categories, duration/distance for cardio, reps only for calisthenics
        when (category) {
            ExerciseCategory.CARDIO -> {
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
                        singleLine = true
                    )
                }
            }
            ExerciseCategory.CALISTHENICS -> {
                OutlinedTextField(
                    value = row.reps,
                    onValueChange = { onChanged(row.copy(reps = it, settings = row.settings)) },
                    label = { Text("Reps") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            else -> {
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = row.weight,
                        onValueChange = { onChanged(row.copy(weight = it, settings = row.settings)) },
                        label = { Text("Weight (lb)") },
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
                }
            }
        }
    }
}
