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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.gymlog.app.data.ExerciseCatalog
import com.gymlog.app.data.ExerciseCategory
import com.gymlog.app.ui.Screen
import com.gymlog.app.ui.components.ScreenTopBar

/**
 * Read-only exercise encyclopedia. Each entry shows one common exercise in a category
 * with a one-line "how to" cue.
 *
 * This screen is intentionally content-only — there is no Add button here. New custom
 * exercises are added on-the-fly from the New Session / Routine edit flows. This
 * screen is the reference for technique.
 */
@Composable
fun ExercisesScreen(navController: NavHostController, padding: PaddingValues) {
    Scaffold(
        topBar = {
            ScreenTopBar(
                title = "Exercise Library",
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.ExerciseNew.route) }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add custom exercise")
                    }
                }
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
                "Common exercises with one-line cues for proper form. Tap and hold a future version " +
                    "to see images / animations.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                ExerciseCategory.values().forEach { cat ->
                    val entries = ExerciseCatalog.LIBRARY_BY_CATEGORY[cat].orEmpty()
                    if (entries.isNotEmpty()) {
                        item(key = "header-${cat.name}") {
                            Text(
                                cat.label,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                            )
                        }
                        items(entries, key = { "lib-${cat.name}-${it.name}" }) { entry ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.outlinedCardColors()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            entry.name,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            entry.howTo,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    // Reserve space for a future thumbnail — keeps layout stable
                                    // when images are added later.
                                    androidx.compose.foundation.layout.Spacer(
                                        modifier = Modifier.padding(start = 12.dp)
                                    )
                                    /* Filler reserved for image. Uncomment when ready:
                                    Box(modifier = Modifier.size(64.dp).clip(...)) { AsyncImage(...) }
                                    */
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
