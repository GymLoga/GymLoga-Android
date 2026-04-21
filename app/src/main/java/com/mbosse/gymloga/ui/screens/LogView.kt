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

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mbosse.gymloga.data.DataLogic
import com.mbosse.gymloga.data.Session
import com.mbosse.gymloga.ui.GymLogaViewModel
import com.mbosse.gymloga.ui.components.FlowRow
import com.mbosse.gymloga.ui.components.GymInput
import com.mbosse.gymloga.ui.components.SetBadge
import com.mbosse.gymloga.ui.theme.*

@Composable
fun LogView(viewModel: GymLogaViewModel, sessions: List<Session>) {
    val scrollState = rememberScrollState()
    val allNames = DataLogic.getAllExerciseNames(sessions)

    Column(modifier = Modifier.verticalScroll(scrollState).padding(vertical = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column {
                GymInput(
                    value = viewModel.aDate,
                    onValueChange = { viewModel.aDate = it },
                    placeholder = "YYYY-MM-DD",
                    modifier = Modifier.width(120.dp)
                )
                if (viewModel.aDate.isNotEmpty() && !viewModel.isDateValid) {
                    Text(
                        "Invalid date",
                        style = MaterialTheme.typography.labelSmall.copy(color = Red),
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }
            }
            GymInput(
                value = viewModel.aLabel,
                onValueChange = { viewModel.aLabel = it },
                placeholder = "Label (recovery, pull...)",
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        GymInput(
            value = viewModel.aNote,
            onValueChange = { viewModel.aNote = it },
            placeholder = "Session notes",
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        viewModel.aExercises.forEach { ex ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .background(Surface, RoundedCornerShape(8.dp))
                    .border(1.dp, Border, RoundedCornerShape(8.dp))
                    .padding(10.dp, 12.dp)
            ) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            ex.name.uppercase(),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                            modifier = Modifier.clickable { viewModel.curName = ex.name }
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "UNDO",
                                style = MaterialTheme.typography.labelSmall.copy(color = Red),
                                modifier = Modifier.clickable { viewModel.removeLastSet(ex.id) }
                            )
                            Text(
                                "DEL",
                                style = MaterialTheme.typography.labelSmall.copy(color = Red),
                                modifier = Modifier.clickable { viewModel.deleteExercise(ex.id) }
                            )
                        }
                    }
                    FlowRow(modifier = Modifier.padding(top = 4.dp)) {
                        ex.sets.forEach { SetBadge(it) }
                    }
                    if (ex.note.isNotEmpty()) {
                        Text(
                            ex.note,
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 11.sp, color = TextDim, fontStyle = FontStyle.Italic),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .background(Surface, RoundedCornerShape(8.dp))
                .border(1.dp, Accent.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Column {
                val suggestions = remember(viewModel.curName, allNames) {
                    if (viewModel.curName.isBlank()) emptyList()
                    else allNames.filter {
                        it.lowercase().contains(viewModel.curName.lowercase()) &&
                                it.lowercase() != viewModel.curName.lowercase()
                    }.take(5)
                }

                GymInput(
                    value = viewModel.curName,
                    onValueChange = { viewModel.curName = it },
                    placeholder = "Exercise name",
                    modifier = Modifier.fillMaxWidth()
                )

                if (suggestions.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        suggestions.forEach { suggestion ->
                            Box(
                                modifier = Modifier
                                    .background(SurfaceHi, RoundedCornerShape(4.dp))
                                    .border(1.dp, Border, RoundedCornerShape(4.dp))
                                    .clickable { viewModel.curName = suggestion }
                                    .padding(vertical = 4.dp, horizontal = 8.dp)
                            ) {
                                Text(suggestion, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 11.sp, color = Accent))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GymInput(
                        value = viewModel.curSet,
                        onValueChange = { viewModel.curSet = it },
                        placeholder = "135x5 or 20x10x2",
                        modifier = Modifier.weight(1f),
                        onDone = { viewModel.addSet() }
                    )
                    Button(
                        onClick = { viewModel.addSet() },
                        colors = ButtonDefaults.buttonColors(containerColor = Accent),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(10.dp, 14.dp)
                    ) {
                        Text("ADD", style = MaterialTheme.typography.labelSmall.copy(color = Bg, fontWeight = FontWeight.ExtraBold))
                    }
                }

                if (!viewModel.showNoteInput) {
                    Text(
                        "+ note",
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 11.sp, color = TextDim),
                        modifier = Modifier.clickable { viewModel.showNoteInput = true }.padding(top = 4.dp)
                    )
                } else {
                    Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GymInput(
                            value = viewModel.curExNote,
                            onValueChange = { viewModel.curExNote = it },
                            placeholder = "ezpz, slow bar, etc",
                            modifier = Modifier.weight(1f),
                            onDone = { viewModel.addExNote() }
                        )
                        OutlinedButton(
                            onClick = { viewModel.addExNote() },
                            border = BorderStroke(1.dp, Accent),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("OK", style = MaterialTheme.typography.labelSmall.copy(color = Accent))
                        }
                    }
                }
            }
        }

        Text(
            "135x5 one set · 20x10x2 two sets · 30s freeform",
            style = MaterialTheme.typography.labelSmall.copy(color = TextDim),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { viewModel.saveSession() },
                modifier = Modifier.weight(1f),
                enabled = viewModel.aExercises.isNotEmpty() && viewModel.isDateValid,
                colors = ButtonDefaults.buttonColors(containerColor = Accent, disabledContainerColor = TextDim.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    if (viewModel.editSessionId != null) "UPDATE" else "SAVE SESSION",
                    style = MaterialTheme.typography.labelSmall.copy(color = Bg, fontWeight = FontWeight.ExtraBold)
                )
            }
            OutlinedButton(
                onClick = { viewModel.clearLog() },
                border = BorderStroke(1.dp, Red),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("CLEAR", style = MaterialTheme.typography.labelSmall.copy(color = Red))
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}
