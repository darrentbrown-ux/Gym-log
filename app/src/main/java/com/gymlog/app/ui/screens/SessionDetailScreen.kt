package com.gymlog.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
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
import com.gymlog.app.data.SessionExerciseDetail
import com.gymlog.app.data.SessionSet
import com.gymlog.app.ui.GymLogViewModel
import com.gymlog.app.ui.components.DropdownField
import com.gymlog.app.ui.components.ScreenTopBar
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * In-workout screen.
 *
 * Layout:
 *   [Scaffold top bar]
 *   ┌───────────────────────────────────────────┐
 *   │  date                                      │  <- fixed
 *   │  [chip][chip][chip][chip]  ⊕              │  <- QuickNav (wraps, pinned)
 *   ├───────────────────────────────────────────┤
 *   │  Exercise card 1                            │
 *   │  Exercise card 2                            │  <- LazyColumn fills rest
 *   │  Exercise card 3                            │
 *   │  ...                                        │
 *   ├───────────────────────────────────────────┤
 *   │  + Add exercise during workout             │
 *   └───────────────────────────────────────────┘
 *
 * - Cardio exercises get duration + distance only (no weight/reps/sets count).
 * - Each exercise card shows preferred-settings reminder (small grey text).
 * - Sets are presented one at a time: only the most-recent is fully editable,
 *   older ones are collapsed single lines. None start marked Done.
 * - QuickNav chips are tap-to-jump; long-press + drag = reorder.
 * - "Save to routine" (small icon-only button) appears when session came from a preset.
 */
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

    // Local in-memory ordering. Mirrors the Flow but lets us drag-reorder without
    // a DB roundtrip; only persisted to DB via "save to routine".
    val orderedIds = remember { mutableStateListOf<Long>() }
    LaunchedEffect(sessionExerciseList) {
        val flowIds = sessionExerciseList.map { it.sessionExerciseId }
        // First time, take flow order. After that, only follow flow when our list was empty
        // (otherwise concurrent edits would clobber user drag).
        if (orderedIds.isEmpty()) orderedIds.addAll(flowIds)
    }

    var showAddExercise by remember { mutableStateOf(false) }
    var pickedExercise by remember { mutableStateOf<Exercise?>(null) }
    var selectedIndex by remember { mutableStateOf(-1) }

    val listState = rememberLazyListState()

    fun jumpTo(i: Int) {
        selectedIndex = i
        scope.launch {
            // QuickNav now lives OUTSIDE the LazyColumn (pinned at top), so item 0 in
            // the LazyColumn is the first exercise. Bring it to the top of the viewport.
            listState.animateScrollToItem(i, scrollOffset = 0)
        }
    }

    // ---- REST timer state ----
    // The REST button replaces its label with the seconds-remaining countdown while
    // running. When it reaches 0, the RestAlarm fires off a beep sequence on the IO
    // dispatcher (handled by `scope`).
    val defaultRestSec by vm.prefs.restSeconds.collectAsState()
    var restRunning by remember { mutableStateOf(false) }
    var restRemaining by remember { mutableStateOf(0) }
    // Cancellation flag so we can cancel a running timer cleanly if the user taps again.
    var restJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    fun startRest() {
        if (restRunning) return
        restRunning = true
        restRemaining = defaultRestSec
        restJob = scope.launch {
            try {
                while (restRemaining > 0) {
                    kotlinx.coroutines.delay(1000)
                    restRemaining -= 1
                }
                com.gymlog.app.audio.RestAlarm.beep()
                snackbar.showSnackbar("Rest complete — back to it!")
            } finally {
                restRunning = false
            }
        }
    }

    fun cancelRest() {
        restJob?.cancel()
        restJob = null
        restRunning = false
    }

    Scaffold(
        topBar = {
            ScreenTopBar(
                title = sessionName,
                onBack = { navController.popBackStack() },
                actions = {
                    // REST button. Label = "REST" when idle; countdown replaces it
                    // when running. Tap-while-running cancels.
                    val restLabel = if (restRunning) "${restRemaining}s" else "REST"
                    TextButton(
                        onClick = { if (restRunning) cancelRest() else startRest() }
                    ) {
                        Text(
                            restLabel,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (restRunning) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (restRunning) androidx.compose.ui.text.font.FontWeight.Bold
                                         else androidx.compose.ui.text.font.FontWeight.Normal
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            // ---- Pinned header: date + QuickNavBar ----
            Text(
                SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault()).format(Date(sessionDate)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )

            QuickNavBar(
                exercises = orderedIds.mapNotNull { id ->
                    sessionExerciseList.firstOrNull { it.sessionExerciseId == id }
                },
                selectedIndex = selectedIndex,
                onTap = { idx -> jumpTo(idx) },
                onLongPressDrag = { from, to -> reorderInMemory(orderedIds, from, to) }
            )

            // Small, always-visible "Save to routine" icon when this session has a source preset.
            // Visible only when there's a preset AND the user has changed the order vs. DB.
            if (sessionPresetId != null && orderedIds.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = {
                            scope.launch {
                                val ids = orderedIds.toList()
                                vm.reorderSessionExercises(sessionId, ids)
                                sessionPresetId?.let { presetId ->
                                    vm.reorderPresetExercises(presetId, ids)
                                }
                                snackbar.showSnackbar("Order saved")
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Filled.Save,
                            contentDescription = "Save order to routine"
                        )
                    }
                }
            }

            // ---- Lazy list of exercise cards ----
            if (orderedIds.isEmpty()) {
                EmptyHint("This workout has no exercises yet. Tap + Add exercise below.")
            }
            val detailList: List<SessionExerciseDetail> = orderedIds.mapNotNull { id ->
                sessionExerciseList.firstOrNull { it.sessionExerciseId == id }
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                itemsIndexed(items = detailList, key = { _, d -> d.sessionExerciseId }) { idx, detail ->
                    val isSelected = idx == selectedIndex && selectedIndex >= 0
                    ExerciseLogCard(
                        detail = detail,
                        isHighlighted = isSelected,
                        vm = vm,
                        sessionId = sessionId
                    )
                }
            }

            HorizontalDivider()

            OutlinedButton(
                onClick = { showAddExercise = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Add exercise during workout")
            }
        }
    }

    if (showAddExercise) {
        val allExercises by vm.exercises.collectAsState(initial = emptyList())
        AddExerciseToSessionDialog(
            allExercises = allExercises,
            onDismiss = { showAddExercise = false; pickedExercise = null },
            onConfirm = { picked ->
                // We always have a real Exercise row in the DB by this point (either
                // a pre-existing one, or a freshly-inserted one from "Other — type
                // custom name"). vm.addSessionExercise just inserts a SessionExercise
                // pointing at the existing exercise id.
                scope.launch {
                    vm.addSessionExercise(sessionId, picked.id)
                    pickedExercise = null
                    showAddExercise = false
                }
            }
        )
    }
}

/**
 * Mid-workout "Add exercise" dialog. Same group-first UX as the routine-edit Add
 * dialog so the user has one consistent flow for picking exercises.
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun AddExerciseToSessionDialog(
    allExercises: List<Exercise>,
    onDismiss: () -> Unit,
    onConfirm: (Exercise) -> Unit
) {
    val scope = rememberCoroutineScope()
    val vm: GymLogViewModel = viewModel()
    var pickedCategory by remember { mutableStateOf<ExerciseCategory?>(null) }
    var pickedName by remember { mutableStateOf("") }
    var pickedLabel by remember { mutableStateOf("") }
    var isCustomName by remember { mutableStateOf(false) }
    var customNameField by remember { mutableStateOf("") }
    var pickedExercise by remember { mutableStateOf<Exercise?>(null) }

    val libraryOptions = remember(pickedCategory) {
        val cat = pickedCategory ?: return@remember emptyList<String>()
        val names = ExerciseCatalog.COMMON_BY_CATEGORY[cat].orEmpty()
        val libraryLower = names.map { it.lowercase() }.toSet()
        val customDb = allExercises
            .filter { it.category == cat }
            .filter { it.name.lowercase() !in libraryLower }
            .map { "${it.name} ★" }
        names + customDb + listOf("Other — type custom name…")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add exercise") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text("1. Pick a group", style = MaterialTheme.typography.titleSmall)
                // FlowRow so on narrow landscape widths all four group chips wrap to a
                // second line instead of overflowing off the right edge. Matches the
                // routine-edit dialog so both flows look the same.
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
                                customNameField = ""
                                pickedExercise = null
                            },
                            label = { Text(cat.label) }
                        )
                    }
                }

                if (pickedCategory != null) {
                    Text("2. Pick an exercise", style = MaterialTheme.typography.titleSmall)
                    val dropdownValue = when {
                        isCustomName && customNameField.isNotBlank() -> "$customNameField (custom)"
                        pickedLabel.isNotBlank() -> pickedLabel
                        else -> "Choose from ${libraryOptions.size}…"
                    }
                    DropdownField(
                        label = "Exercise",
                        value = dropdownValue,
                        options = libraryOptions,
                        onSelected = { picked ->
                            if (picked == "Other — type custom name…") {
                                isCustomName = true
                                pickedLabel = ""
                                pickedName = ""
                                pickedExercise = null
                            } else {
                                isCustomName = false
                                pickedLabel = picked
                                pickedName = picked.removeSuffix(" ★").trim()
                            }
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
            }
        },
        confirmButton = {
            Button(
                enabled = pickedName.isNotBlank() && pickedCategory != null,
                onClick = {
                    val cat = pickedCategory ?: return@Button
                    val chosenName = pickedName.trim()
                    if (chosenName.isEmpty()) return@Button
                    // Resolve or create the Exercise row, then fire onConfirm.
                    scope.launch {
                        val resolved = if (isCustomName) {
                            // Custom name: insert a fresh Exercise, returning its id.
                            val newId = vm.ensureExerciseInDb(chosenName, cat, emptyList())
                            allExercises.firstOrNull { it.id == newId }
                                ?: Exercise(id = newId, name = chosenName, category = cat)
                        } else {
                            // Library or DB-only entry — find the existing row.
                            allExercises.firstOrNull { it.name.equals(chosenName, ignoreCase = true) && it.category == cat }
                                ?: run {
                                    // Not in DB yet (e.g. fresh library pick) — insert it.
                                    val newId = vm.ensureExerciseInDb(chosenName, cat, emptyList())
                                    Exercise(id = newId, name = chosenName, category = cat)
                                }
                        }
                        onConfirm(resolved)
                    }
                }
            ) { Text("Add") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Wrapping row of chips at the top of the screen.
 *
 *  - Tap = callback with the chip's index in [exercises]
 *  - Long-press + drag horizontally = swap with the next chip
 *
 * Uses an in-screen MutableState copy so the swap is immediate and not affected by Compose's
 * recomposition timing. After the user lifts their finger, we clear the dragging flag.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickNavBar(
    exercises: List<SessionExerciseDetail>,
    selectedIndex: Int,
    onTap: (Int) -> Unit,
    onLongPressDrag: (from: Int, to: Int) -> Unit
) {
    // Local mutable copy so we can show the live drag preview without mutating upstream state
    val live = remember(exercises) { mutableStateListOf<Long>().also { it.addAll(exercises.map { e -> e.sessionExerciseId }) } }
    LaunchedEffect(exercises) {
        // Sync downstream: if upstream list changed (e.g. new exercise added), refresh
        live.clear()
        live.addAll(exercises.map { e -> e.sessionExerciseId })
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        // We deliberately wrap chips into multiple rows so long exercise lists don't
        // require horizontal scrolling to find a chip.
        FlowRowChips(
            ids = live,
            lookup = { id -> exercises.firstOrNull { it.sessionExerciseId == id } },
            selectedId = exercises.getOrNull(selectedIndex)?.sessionExerciseId,
            onTap = { id -> onTap(live.indexOf(id)) },
            onLongPressDrag = { fromId, toId ->
                val from = live.indexOf(fromId)
                val to = live.indexOf(toId)
                if (from >= 0 && to >= 0 && from != to) {
                    onLongPressDrag(from, to)
                }
            }
        )
    }
}

/**
 * Flow layout of workout chips.
 *
 * Reorder interaction (the reliable one):
 *   1. **Long-press** a chip — it gets a lifted style (border + tint) and becomes the "pickup".
 *   2. **Tap** another chip — the pickup and the tapped chip swap positions in `ids`.
 *   3. Tap the same lifted chip, or tap empty space outside any chip, to cancel the pickup.
 *
 * Tap (short) = callback to jump-scroll to that exercise's card.
 *
 * The previous version tried long-press + continuous horizontal drag, which fought with
 * FlowRow's layout pass and the chip visually snapped back to its slot every time the
 * underlying list reordered. Tap-to-swap is predictable and works.
 */
@OptIn(ExperimentalLayoutApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun FlowRowChips(
    ids: SnapshotStateList<Long>,
    lookup: (Long) -> SessionExerciseDetail?,
    selectedId: Long?,
    onTap: (Long) -> Unit,
    @Suppress("UNUSED_PARAMETER") onLongPressDrag: (fromId: Long, toId: Long) -> Unit
) {
    var pickedChipId by remember { mutableStateOf<Long?>(null) }

    androidx.compose.foundation.layout.FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ids.forEach { id ->
            val detail = lookup(id) ?: return@forEach
            val isSelected = id == selectedId
            val isPicked = id == pickedChipId

            val borderModifier = if (isPicked) {
                Modifier.border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                )
            } else if (isSelected) {
                Modifier.border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.tertiary,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                )
            } else Modifier

            // Outer Box hosts both the visible chip and an invisible overlay that owns the
            // long-press detector. Layering them in a Box lets the overlay use
            // `matchParentSize` so it exactly covers the chip's hit target without
            // intercepting short taps (the chip's own onClick fires first).
            Box(modifier = borderModifier) {
                AssistChip(
                    onClick = {
                        val picked = pickedChipId
                        when {
                            picked == null -> onTap(id)
                            picked == id -> pickedChipId = null  // cancel pickup
                            else -> {
                                // Swap picked with this chip.
                                val fromIdx = ids.indexOf(picked)
                                val toIdx = ids.indexOf(id)
                                if (fromIdx >= 0 && toIdx >= 0 && fromIdx != toIdx) {
                                    val moving = ids.removeAt(fromIdx)
                                    ids.add(toIdx, moving)
                                }
                                pickedChipId = null
                            }
                        }
                    },
                    label = { Text(detail.exerciseName) },
                    leadingIcon = when {
                        isPicked -> { { Icon(Icons.Filled.DragIndicator, contentDescription = null) } }
                        isSelected -> { { Icon(Icons.Filled.DragHandle, contentDescription = null) } }
                        else -> null
                    }
                )
                // Transparent overlay capturing long-press. Does not block clicks because
                // `detectTapGestures` only triggers `onLongPress` after the long-press
                // timer fires (short taps don't fire it). The chip's onClick handles the
                // short-tap case.
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .matchParentSize()
                        .pointerInput(id, pickedChipId) {
                            detectTapGestures(
                                onLongPress = {
                                    if (pickedChipId == null) pickedChipId = id
                                    else if (pickedChipId == id) pickedChipId = null
                                }
                            )
                        }
                )
            }
        }
    }
}

private fun reorderInMemory(list: SnapshotStateList<Long>, from: Int, to: Int) {
    if (from == to || from !in list.indices || to !in list.indices) return
    val item = list.removeAt(from)
    list.add(to, item)
}

/**
 * One card per exercise. Cardio hides Sets count default and uses Duration+Distance,
 * strength uses Weight+Reps, calisthenics uses Reps only.
 *
 * Sets:
 *   - First card mount: exactly **one** set row, expanded, NOT marked Done.
 *   - User taps "+ Add set" to create another set. The new set becomes "current"
 *     (expanded); older ones collapse to one-line summaries.
 *   - User taps a collapsed row → it becomes current (expanded). Tap the checkbox to
 *     toggle complete (saves to DB).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseLogCard(
    detail: SessionExerciseDetail,
    isHighlighted: Boolean,
    vm: GymLogViewModel,
    sessionId: Long
) {
    val scope = rememberCoroutineScope()

    // Setting definitions for this exercise (Seat Height, Chest Pad Depth, etc.) — observed
    // so the inline-settings header shows the live per-machine preferred values, and edits
    // persist across all routines/workouts.
    val settingDefs by vm.settingsFor(detail.exerciseId).collectAsState(initial = emptyList())

    // We need direct repo access to await the Flow's FIRST emission before seeding `rows`.
    val repo = vm.repo

    // Mirror DB sets into local rows. We DON'T use `collectAsState(initial = emptyList())`
    // because the empty initial value is indistinguishable from "this exercise has no sets
    // yet" — which used to cause us to seed a dummy set row that was then clobbered when
    // the real sets arrived. Instead, we kick off a coroutine that awaits the Flow's
    // first emit and only THEN seeds rows.
    var hasInitialised by remember(detail.sessionExerciseId) { mutableStateOf(false) }
    val rows = remember(detail.sessionExerciseId) { mutableStateListOf<SetRowState>() }
    LaunchedEffect(detail.sessionExerciseId) {
        if (hasInitialised) return@LaunchedEffect
        // Await the first emission of the Room Flow so we know whether the DB has any sets
        // for this exercise.
        val first = repo.sets(detail.sessionExerciseId).first()
        if (first.isEmpty()) {
            rows.add(SetRowState(setNumber = 1))
        } else {
            rows.clear()
            first.forEach { rows.add(SetRowState.fromExisting(it)) }
        }
        hasInitialised = true
    }

    var settingsExpanded by remember(detail.sessionExerciseId) { mutableStateOf(false) }

    // When all sets are Done AND we've finished initialising rows, AUTO-collapse the
    // card to a single-line summary. The user can tap the header to expand it back
    // out (e.g. to edit a set or to review the settings). Tapping the header toggles
    // the override-expand state; unmarking a set (via checkbox) automatically
    // re-expands because allDone flips back to false.
    var userExpanded by remember(detail.sessionExerciseId) { mutableStateOf(false) }
    val allDone = hasInitialised && rows.isNotEmpty() && rows.all { it.completed }
    // Effective expanded state: AUTO-collapsed when allDone is true, unless the
    // user has explicitly tapped to expand.
    val expanded = !allDone || userExpanded

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
            // ----- Header (clickable to expand/collapse when auto-collapsed) -----
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (allDone) userExpanded = !userExpanded
                    }
            ) {
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
                if (allDone) {
                    // Compact "Done" badge — tapping the header toggles the override-expand.
                    Text(
                        if (expanded) "Done" else "Done — tap to expand",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                } else {
                    IconButton(onClick = { settingsExpanded = !settingsExpanded }) {
                        Icon(
                            if (settingsExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = "Edit preferred settings"
                        )
                    }
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

            // When auto-collapsed and not user-expanded, skip the settings editor and
            // the set rows — the header alone is the visible summary.
            if (expanded) {

            // ----- Preferred settings editor -----
            if (settingsExpanded && settingDefs.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                Text(
                    "Preferred settings",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                settingDefs.forEach { def ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            def.name,
                            modifier = Modifier.width(120.dp),
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

            // ----- Sets -----
            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Sets",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
            rows.forEachIndexed { idx, row ->
                val isCurrent = idx == rows.lastIndex
                if (isCurrent || settingsExpanded) {
                    SetRowEditor(
                        row = row,
                        isCurrent = isCurrent,
                        category = detail.exerciseCategory,
                        exerciseName = detail.exerciseName,
                        onChanged = { updated ->
                            rows[idx] = updated
                            saveRow(updated)
                        },
                        onDelete = {
                            scope.launch {
                                row.existingId?.let {
                                    vm.deleteSet(
                                        SessionSet(
                                            id = it,
                                            sessionExerciseId = detail.sessionExerciseId,
                                            setNumber = row.setNumber
                                        )
                                    )
                                }
                                if (rows.size > 1) {
                                    rows.removeAt(idx)
                                    for (i in rows.indices) rows[i] = rows[i].copy(setNumber = i + 1)
                                }
                            }
                        }
                    )
                } else {
                    CollapsedSetRow(
                        row = row,
                        category = detail.exerciseCategory,
                        exerciseName = detail.exerciseName,
                        onToggleComplete = {
                            val next = row.copy(completed = !row.completed)
                            rows[idx] = next
                            scope.launch {
                                val currentId = next.existingId
                                if (currentId != null) {
                                    vm.updateSet(
                                        SessionSet(
                                            id = currentId,
                                            sessionExerciseId = detail.sessionExerciseId,
                                            setNumber = next.setNumber,
                                            reps = next.reps.toIntOrNull(),
                                            weight = next.weight.toDoubleOrNull(),
                                            settingsValues = SetRowState.encodeSettings(next.settings),
                                            durationSeconds = next.durationMin.toIntOrNull()?.let { it * 60 },
                                            distance = next.distance.toDoubleOrNull(),
                                            completed = next.completed
                                        )
                                    )
                                }
                            }
                        },
                        onTapToExpand = {
                            // Move this row to the end of `rows` so it becomes "current" (expanded).
                            // We re-number rows by their new position so set labels stay 1..N.
                            val snapshot = rows.toList()
                            val moving = snapshot[idx]
                            val filtered = snapshot.filterIndexed { i, _ -> i != idx }.toMutableList()
                            filtered.add(moving.copy(setNumber = filtered.size + 1))
                            // Re-sequence the in-between rows
                            for (i in filtered.indices) filtered[i] = filtered[i].copy(setNumber = i + 1)
                            rows.clear()
                            rows.addAll(filtered)
                            // Persist the renumber for non-current rows that have a DB id
                            scope.launch {
                                rows.forEach { r ->
                                    r.existingId?.let { id ->
                                        if (r.setNumber != row.setNumber) {
                                            vm.updateSet(
                                                SessionSet(
                                                    id = id,
                                                    sessionExerciseId = detail.sessionExerciseId,
                                                    setNumber = r.setNumber
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.padding(top = 6.dp))
            OutlinedButton(
                onClick = {
                    val nextNum = (rows.maxOfOrNull { it.setNumber } ?: 0) + 1
                    val newRow = SetRowState(setNumber = nextNum)
                    rows.add(newRow)
                    saveRow(newRow)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Add set")
            }
            } // end if (expanded)
        }
    }
}

@Composable
private fun CollapsedSetRow(
    row: SetRowState,
    category: ExerciseCategory,
    exerciseName: String,
    onToggleComplete: () -> Unit,
    onTapToExpand: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(enabled = true) { onTapToExpand() }
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
            summarize(row, category, exerciseName),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun summarize(row: SetRowState, category: ExerciseCategory, exerciseName: String = ""): String = when (category) {
    ExerciseCategory.CARDIO -> {
        if (exerciseName.equals("Treadmill", ignoreCase = true)) {
            val speed = row.settings["Speed"]?.takeIf { it.isNotBlank() }?.let { "Speed $it" }
            val dur = row.durationMin.takeIf { it.isNotBlank() }?.let { "$it min" }
            val incline = row.settings["Incline"]?.takeIf { it.isNotBlank() }?.let { "Incline $it" }
            listOfNotNull(speed, dur, incline).joinToString(" · ").takeIf { it.isNotBlank() } ?: "Tap to fill in"
        } else {
            val dur = row.durationMin
            val dist = row.distance
            when {
                dur.isNotBlank() && dist.isNotBlank() -> "$dur min · $dist"
                dur.isNotBlank() -> "$dur min"
                dist.isNotBlank() -> dist
                else -> "Tap to fill in"
            }
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
    isCurrent: Boolean,
    category: ExerciseCategory,
    exerciseName: String = "",
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
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete set")
                }
            }
        }

        when (category) {
            ExerciseCategory.CARDIO -> {
                // Treadmill gets Speed + Duration + Incline (the standard treadmill HUD).
                // Other cardio machines keep Duration + Distance (or Distance alone for
                // bikes). The user's preferred values are seeded from the routine's
                // setting-def envelope into `row.settings`.
                val isTreadmill = exerciseName.equals("Treadmill", ignoreCase = true)
                if (isTreadmill) {
                    val speed = row.settings["Speed"].orEmpty()
                    val incline = row.settings["Incline"].orEmpty()
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = speed,
                            onValueChange = { v ->
                                val ns = row.settings.toMutableMap(); ns["Speed"] = v
                                onChanged(row.copy(settings = ns))
                            },
                            label = { Text("Speed") },
                            modifier = Modifier.weight(1f).padding(end = 4.dp),
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = row.durationMin,
                            onValueChange = { onChanged(row.copy(durationMin = it, settings = row.settings)) },
                            label = { Text("Duration (min)") },
                            modifier = Modifier.weight(1f).padding(start = 4.dp, end = 4.dp),
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = incline,
                            onValueChange = { v ->
                                val ns = row.settings.toMutableMap(); ns["Incline"] = v
                                onChanged(row.copy(settings = ns))
                            },
                            label = { Text("Incline") },
                            modifier = Modifier.weight(1f).padding(start = 4.dp),
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                } else {
                    // Generic cardio: Duration + Distance.
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
