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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.gymlog.app.ui.GymLogViewModel
import com.gymlog.app.ui.Screen
import com.gymlog.app.ui.components.ScreenTopBar
import kotlinx.coroutines.launch

@Composable
fun PresetsScreen(navController: NavHostController, padding: PaddingValues) {
    val vm: GymLogViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val presets by vm.presets.collectAsState(initial = emptyList())
    var showNew by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var deleteConfirm by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = { ScreenTopBar("Routines") },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showNew = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New routine") }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier.fillMaxSize().padding(inner).padding(horizontal = 16.dp)
        ) {
            Text(
                "Routines are reusable workout templates. Tap a routine to start a workout " +
                    "with those exercises pre-filled, or tap Edit to change them.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            if (presets.isEmpty()) {
                EmptyHint("No routines yet. Tap the + New routine button below to create your first one.")
            }
            LazyColumn {
                items(presets, key = { it.id }) { p ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        onClick = { navController.navigate(Screen.PresetDetail.build(p.id)) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(p.name, style = MaterialTheme.typography.titleMedium)
                            }
                            IconButton(onClick = { navController.navigate(Screen.PresetEdit.build(p.id)) }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit")
                            }
                            IconButton(onClick = { navController.navigate(Screen.NewSession.build(p.id)) }) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = "Start")
                            }
                            IconButton(onClick = { deleteConfirm = p.id }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNew) {
        AlertDialog(
            onDismissRequest = { showNew = false },
            title = { Text("New routine") },
            text = {
                OutlinedTextField(
                    value = newName, onValueChange = { newName = it },
                    label = { Text("Name") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = newName.trim()
                        if (name.isNotEmpty()) {
                            scope.launch {
                                val id = vm.addPreset(name)
                                showNew = false
                                newName = ""
                                navController.navigate(Screen.PresetEdit.build(id))
                            }
                        }
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showNew = false; newName = "" }) { Text("Cancel") }
            }
        )
    }

    deleteConfirm?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteConfirm = null },
            title = { Text("Delete routine?") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val p = vm.getPreset(id)
                            if (p != null) vm.deletePreset(p)
                            deleteConfirm = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { OutlinedButton(onClick = { deleteConfirm = null }) { Text("Cancel") } }
        )
    }
}
