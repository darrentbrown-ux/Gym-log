package com.gymlog.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.gymlog.app.ui.GymLogViewModel
import com.gymlog.app.ui.Screen
import com.gymlog.app.ui.components.ScreenTopBar

@Composable
fun SessionsScreen(navController: NavHostController, padding: PaddingValues) {
    val vm: GymLogViewModel = viewModel()
    val sessions by vm.sessions.collectAsState(initial = emptyList())

    Scaffold(topBar = { ScreenTopBar("History") }) { inner ->
        Column(modifier = Modifier.fillMaxSize().padding(inner).padding(horizontal = 16.dp)) {
            if (sessions.isEmpty()) {
                EmptyHint("No sessions yet.")
            }
            LazyColumn {
                items(sessions, key = { it.id }) { s ->
                    SessionListRow(
                        dateMillis = s.date,
                        name = s.name,
                        onClick = { navController.navigate(Screen.SessionDetail.build(s.id)) }
                    )
                }
            }
        }
    }
}
