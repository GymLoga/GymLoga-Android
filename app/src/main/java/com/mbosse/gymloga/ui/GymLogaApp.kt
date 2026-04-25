/*
* Copyright (C) 2026 Michael Bosse
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Affero General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU Affero General Public License for more details.
*
* You should have received a copy of the GNU Affero General Public License
* along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
package com.mbosse.gymloga.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.mbosse.gymloga.ui.components.Header
import com.mbosse.gymloga.ui.components.Tabs
import com.mbosse.gymloga.ui.screens.*
import com.mbosse.gymloga.ui.theme.*

@Composable
fun GymLogaApp(viewModel: GymLogaViewModel) {
    val sessions by viewModel.sessions.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Bg,
            surface = Surface,
            onBackground = Text,
            onSurface = Text
        ),
        typography = GymTypography
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Bg
        ) { paddingValues ->
            val focusManager = LocalFocusManager.current
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }
            ) {
                Header(sessions.size)
                Tabs(viewModel.currentView, viewModel.editSessionId != null) { viewModel.currentView = it }

                Box(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                    when (viewModel.currentView) {
                        GymView.LOG -> LogView(viewModel)
                        GymView.HISTORY -> HistoryView(viewModel, sessions, snackbarHostState)
                        GymView.PRS -> PRsView(viewModel)
                        GymView.SESSION_DETAIL -> SessionDetailView(viewModel)
                        GymView.EXERCISE_HISTORY -> ExerciseHistoryView(viewModel, sessions)
                        GymView.ADD_EXERCISE -> AddExerciseView(viewModel)
                        GymView.MANAGE_EXERCISES -> ManageExercisesView(viewModel)
                    }
                }
            }
        }
    }
}
