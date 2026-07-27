package com.gymlog.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.gymlog.app.ui.GymLogViewModel
import com.gymlog.app.ui.Screen
import com.gymlog.app.ui.components.ScreenTopBar

@Composable
fun PresetDetailScreen(navController: NavHostController, padding: PaddingValues, presetId: Long) {
    val vm: GymLogViewModel = viewModel()
    var presetName by remember { mutableStateOf("") }
    LaunchedEffect(presetId) {
        presetName = vm.getPreset(presetId)?.name ?: "(deleted)"
    }
    val exercises by vm.presetExercises(presetId).collectAsState(initial = emptyList())

    Scaffold(
        topBar = { ScreenTopBar(presetName, onBack = { navController.popBackStack() }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate(Screen.NewSession.build(presetId)) },
                icon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                text = { Text("Start") }
            )
        }
    ) { inner ->
        Column(modifier = Modifier.fillMaxSize().padding(inner).padding(horizontal = 16.dp)) {
            if (exercises.isEmpty()) {
                EmptyHint("No exercises in this routine yet. Tap Edit to add some.")
            }
            LazyColumn {
                items(exercises, key = { it.presetExerciseId }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.exerciseName, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                                val txt = buildString {
                                    if (item.defaultWeight != null) append("${item.defaultWeight} lb × ")
                                    append("${item.defaultReps ?: "?"} reps")
                                    append(" • ${item.defaultSets} sets")
                                }
                                Text(txt, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
