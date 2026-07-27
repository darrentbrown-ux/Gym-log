package com.gymlog.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import com.gymlog.app.data.ExerciseCatalog
import com.gymlog.app.data.ExerciseCategory
import com.gymlog.app.ui.GymLogViewModel
import com.gymlog.app.ui.Screen
import com.gymlog.app.ui.components.DropdownField
import com.gymlog.app.ui.components.ScreenTopBar
import kotlinx.coroutines.launch

/** "Other" sentinel for the name dropdown — reveals a free text entry field. */
private const val OTHER_NAME = "Other (type manually)"

/** A single "Setting name + value" pair entered by the user. */
private data class SettingField(var name: String = "", var value: String = "")

/**
 * Pre-fill state passed when launching from a "Quick add" chip.
 * Lets us display the dialog with sensible defaults rather than persisting immediately.
 */
data class NewExercisePrefill(
    val name: String,
    val category: ExerciseCategory,
    val settings: List<String> = emptyList()
)

@Composable
fun ExerciseNewScreen(
    navController: NavHostController,
    padding: PaddingValues,
    prefill: NewExercisePrefill? = null
) {
    val vm: GymLogViewModel = viewModel()
    val scope = rememberCoroutineScope()

    // Form state
    var category by remember { mutableStateOf(prefill?.category ?: ExerciseCategory.WEIGHT_MACHINE) }
    var name by remember { mutableStateOf(prefill?.name ?: "") }
    var isCustomName by remember {
        mutableStateOf(
            prefill != null && prefill.name !in ExerciseCatalog.COMMON_BY_CATEGORY[prefill.category].orEmpty()
        )
    }
    var notes by remember { mutableStateOf("") }
    var defaultWeight by remember { mutableStateOf("") }
    val settings = remember {
        mutableStateListOf<SettingField>().apply {
            prefill?.settings?.forEach { add(SettingField(name = it)) }
            if (prefill == null && category == ExerciseCategory.CARDIO) {
                ExerciseCatalog.suggestedSettings(category).forEach { add(SettingField(name = it)) }
            }
            if (isEmpty()) {
                ExerciseCatalog.suggestedSettings(category).forEach { add(SettingField(name = it)) }
            }
        }
    }

    // Re-seed hints when category changes
    LaunchedEffect(category) {
        if (settings.isEmpty()) {
            ExerciseCatalog.suggestedSettings(category).forEach { settings.add(SettingField(name = it)) }
        }
    }

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
            // ---- 1) CATEGORY (first) ----
            DropdownField(
                label = "Category",
                value = category.label,
                options = ExerciseCategory.values().map { it.label },
                onSelected = { picked ->
                    val cat = ExerciseCategory.values().first { it.label == picked }
                    if (category != cat) {
                        settings.clear()
                        ExerciseCatalog.suggestedSettings(cat).forEach {
                            settings.add(SettingField(name = it))
                        }
                        if (!ExerciseCatalog.usesWeight(cat)) defaultWeight = ""
                    }
                    category = cat
                },
                modifier = Modifier.fillMaxWidth()
            )

            // ---- 2) NAME (scoped to category's common picks + Other) ----
            val commonOptions = remember(category) {
                ExerciseCatalog.COMMON_BY_CATEGORY[category].orEmpty()
            }
            DropdownField(
                label = "Name",
                value = if (isCustomName) OTHER_NAME else name,
                options = commonOptions + OTHER_NAME,
                onSelected = { picked ->
                    if (picked == OTHER_NAME) {
                        isCustomName = true
                        name = ""
                    } else {
                        isCustomName = false
                        name = picked
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (isCustomName) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Custom name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // ---- 3) Default Weight (only when category uses weight) ----
            if (ExerciseCatalog.usesWeight(category)) {
                OutlinedTextField(
                    value = defaultWeight,
                    onValueChange = { defaultWeight = it },
                    label = {
                        Text(if (category == ExerciseCategory.CARDIO) "Default level / resistance"
                             else "Default weight (lb)")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            // ---- 4) Machine settings: name + value pair rows ----
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text("Machine settings", style = MaterialTheme.typography.titleMedium)
            Text(
                "Add the names + values you want recorded with each set. " +
                    "Examples: \"Seat height: 5\", \"Speed: 2.8\", \"Incline: 6\".",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (settings.isEmpty()) {
                Text("No settings yet.", style = MaterialTheme.typography.bodySmall)
            }

            settings.forEachIndexed { idx, field ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = field.name,
                        onValueChange = { v -> settings[idx] = field.copy(name = v) },
                        label = { Text("Setting name") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = field.value,
                        onValueChange = { v -> settings[idx] = field.copy(value = v) },
                        label = { Text("Value") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    IconButton(onClick = { settings.removeAt(idx) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove")
                    }
                }
            }
            OutlinedButton(
                onClick = { settings.add(SettingField()) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("Add setting", modifier = Modifier.padding(start = 6.dp))
            }

            // ---- Save ----
            Button(
                onClick = {
                    scope.launch {
                        val settingsToPersist = settings.map { it.name.trim() }.filter { it.isNotBlank() }
                        val id = vm.addExercise(
                            name = name.trim(),
                            category = category,
                            notes = "${if (notes.isNotBlank()) notes.trim() + "\n\n" else ""}Default weight: ${defaultWeight.ifBlank { "—" }}",
                            settings = settingsToPersist
                        )
                        if (id > 0) navController.popBackStack()
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Check, contentDescription = null)
                Text("Save exercise", modifier = Modifier.padding(start = 6.dp))
            }

            // ---- Quick-add suggestions (scoped to current category, opens form pre-filled) ----
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text("Quick-add (${category.label})", style = MaterialTheme.typography.titleMedium)
            Text(
                "Tap to open New exercise with this name pre-filled so you can review and save.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            commonOptions.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { n ->
                        AssistChip(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val settingsForCat = ExerciseCatalog.suggestedSettings(category)
                                navController.navigate(
                                    Screen.ExerciseNew.build(
                                        NewExercisePrefill(name = n, category = category, settings = settingsForCat)
                                    )
                                )
                            },
                            label = { Text(n) }
                        )
                    }
                }
            }

            Spacer(Modifier.padding(bottom = 24.dp))
        }
    }
}
