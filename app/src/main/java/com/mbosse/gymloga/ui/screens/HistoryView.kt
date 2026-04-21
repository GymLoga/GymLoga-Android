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
package com.mbosse.gymloga.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mbosse.gymloga.data.DataLogic
import com.mbosse.gymloga.data.Session
import com.mbosse.gymloga.ui.ExportImportEvent
import com.mbosse.gymloga.ui.GymLogaViewModel
import com.mbosse.gymloga.ui.GymView
import com.mbosse.gymloga.ui.components.FlowRow
import com.mbosse.gymloga.ui.components.formatDate
import com.mbosse.gymloga.ui.theme.*

@Composable
fun HistoryView(viewModel: GymLogaViewModel, sessions: List<Session>, snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val allNames = DataLogic.getAllExerciseNames(sessions)

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { viewModel.exportToUri(it, context.contentResolver) } }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.importFromUri(it, context.contentResolver) } }

    LaunchedEffect(Unit) {
        viewModel.exportImportEvents.collect { event ->
            val message = when (event) {
                is ExportImportEvent.ExportSuccess -> "Exported successfully"
                is ExportImportEvent.ExportFailure -> "Export failed: ${event.message}"
                is ExportImportEvent.ImportSuccess -> "Imported ${event.added} new session(s)"
                is ExportImportEvent.ImportFailure -> "Import failed: ${event.message}"
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(vertical = 12.dp)) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { exportLauncher.launch("gymloga-export.json") },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, Accent),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(8.dp, 5.dp)
                ) {
                    Text("EXPORT", style = MaterialTheme.typography.labelSmall.copy(color = Accent))
                }
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/json")) },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, TextDim),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(8.dp, 5.dp)
                ) {
                    Text("IMPORT", style = MaterialTheme.typography.labelSmall.copy(color = TextDim))
                }
            }
        }

        if (allNames.isNotEmpty()) {
            item {
                Text("EXERCISES", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(bottom = 6.dp))
                FlowRow(modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
                    allNames.forEach { name ->
                        Box(
                            modifier = Modifier
                                .padding(2.dp)
                                .background(SurfaceHi, RoundedCornerShape(4.dp))
                                .border(1.dp, Border, RoundedCornerShape(4.dp))
                                .clickable {
                                    viewModel.selectedExerciseName = name
                                    viewModel.currentView = GymView.EXERCISE_HISTORY
                                }
                                .padding(vertical = 4.dp, horizontal = 10.dp)
                        ) {
                            Text(name, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 11.sp))
                        }
                    }
                }
            }
        }

        item {
            Text("SESSIONS", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(bottom = 8.dp))
        }

        if (sessions.isEmpty()) {
            item {
                Text(
                    "No sessions yet.",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 13.sp, color = TextDim),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        items(sessions.sortedByDescending { it.date }) { session ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .background(Surface, RoundedCornerShape(8.dp))
                    .border(1.dp, Border, RoundedCornerShape(8.dp))
                    .clickable {
                        viewModel.selectedSession = session
                        viewModel.currentView = GymView.SESSION_DETAIL
                    }
                    .padding(12.dp, 14.dp)
            ) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(formatDate(session.date), style = MaterialTheme.typography.bodyLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Accent))
                        Text("${session.exercises.size} ex", style = MaterialTheme.typography.bodyLarge.copy(fontSize = 10.sp, color = TextDim))
                    }
                    if (session.label.isNotEmpty()) {
                        Text(session.label, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold))
                    }
                    Text(
                        session.exercises.joinToString(" · ") { it.name },
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 11.sp, color = TextDim)
                    )
                    if (session.note.isNotEmpty()) {
                        Divider(modifier = Modifier.padding(vertical = 4.dp), color = Border.copy(alpha = 0.2f))
                        Text(
                            session.note,
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 11.sp, color = TextDim, fontStyle = FontStyle.Italic)
                        )
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}
