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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mbosse.gymloga.data.Exercise
import com.mbosse.gymloga.data.ExerciseDefinition
import com.mbosse.gymloga.ui.GymLogaViewModel
import com.mbosse.gymloga.ui.GymView
import com.mbosse.gymloga.ui.components.FlowRow
import com.mbosse.gymloga.ui.components.GymInput
import com.mbosse.gymloga.ui.components.SetBadge
import com.mbosse.gymloga.ui.theme.*

@Composable
fun LogView(viewModel: GymLogaViewModel) {
    val scrollState = rememberScrollState()
    val exerciseDefinitions by viewModel.exerciseDefinitions.collectAsState()
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .verticalScroll(scrollState)
            .imePadding()
            .padding(vertical = 12.dp)
    ) {
        // Session header
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

        // Exercise picker — only for selecting/adding new exercises
        ExercisePicker(
            definitions = exerciseDefinitions,
            onSelect = { name, defId -> viewModel.selectExercise(name, defId) },
            onDefineNew = { viewModel.currentView = GymView.ADD_EXERCISE },
            onManage = { viewModel.currentView = GymView.MANAGE_EXERCISES }
        )

        Text(
            "135x5 one set · 20x10x2 two sets · 30s freeform",
            style = MaterialTheme.typography.labelSmall.copy(color = TextDim),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Exercise cards — insertion order, edit controls inline on the active one
        viewModel.aExercises.forEach { ex ->
            val isActive = ex.name.lowercase() == viewModel.curName.lowercase()
            ExerciseCard(
                ex = ex,
                isActive = isActive,
                onActivate = { viewModel.selectExercise(ex.name) },
                onUndo = { viewModel.removeLastSet(ex.id) },
                onDelete = { viewModel.deleteExercise(ex.id) },
                curSet = viewModel.curSet,
                onCurSetChange = { viewModel.curSet = it },
                onAddSet = { viewModel.addSet(); focusManager.clearFocus() },
                showNoteInput = viewModel.showNoteInput,
                onShowNote = { viewModel.showNoteInput = true },
                curExNote = viewModel.curExNote,
                onCurExNoteChange = { viewModel.curExNote = it },
                onAddNote = { viewModel.addExNote() }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { viewModel.saveSession() },
                modifier = Modifier.weight(1f),
                enabled = viewModel.aExercises.any { it.sets.isNotEmpty() } && viewModel.isDateValid,
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

@Composable
private fun ExerciseCard(
    ex: Exercise,
    isActive: Boolean,
    onActivate: () -> Unit,
    onUndo: () -> Unit,
    onDelete: () -> Unit,
    curSet: String,
    onCurSetChange: (String) -> Unit,
    onAddSet: () -> Unit,
    showNoteInput: Boolean,
    onShowNote: () -> Unit,
    curExNote: String,
    onCurExNoteChange: (String) -> Unit,
    onAddNote: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .background(Surface, RoundedCornerShape(8.dp))
            .border(1.dp, if (isActive) Accent.copy(alpha = 0.5f) else Border, RoundedCornerShape(8.dp))
            .clickable { onActivate() }
            .padding(10.dp, 12.dp)
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    ex.name.uppercase(),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = if (isActive) Accent else MaterialTheme.colorScheme.onSurface
                    )
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "UNDO",
                        style = MaterialTheme.typography.labelSmall.copy(color = Red),
                        modifier = Modifier.clickable { onUndo() }
                    )
                    Text(
                        "DEL",
                        style = MaterialTheme.typography.labelSmall.copy(color = Red),
                        modifier = Modifier.clickable { onDelete() }
                    )
                }
            }

            if (ex.sets.isNotEmpty()) {
                FlowRow(modifier = Modifier.padding(top = 4.dp)) {
                    ex.sets.forEach { SetBadge(it) }
                }
            }

            if (ex.note.isNotEmpty()) {
                Text(
                    ex.note,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 11.sp, color = TextDim, fontStyle = FontStyle.Italic),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Inline edit controls — only on the active card
            if (isActive) {
                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = Border.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GymInput(
                        value = curSet,
                        onValueChange = onCurSetChange,
                        placeholder = "135x5 or 20x10x2",
                        modifier = Modifier.weight(1f),
                        onDone = { onAddSet() }
                    )
                    Button(
                        onClick = { onAddSet() },
                        enabled = curSet.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, disabledContainerColor = TextDim.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(10.dp, 14.dp)
                    ) {
                        Text("ADD", style = MaterialTheme.typography.labelSmall.copy(color = Bg, fontWeight = FontWeight.ExtraBold))
                    }
                }

                if (!showNoteInput) {
                    Text(
                        "+ note",
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 11.sp, color = TextDim),
                        modifier = Modifier.clickable { onShowNote() }.padding(top = 4.dp)
                    )
                } else {
                    Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GymInput(
                            value = curExNote,
                            onValueChange = onCurExNoteChange,
                            placeholder = "ezpz, slow bar, etc",
                            modifier = Modifier.weight(1f),
                            onDone = { onAddNote() }
                        )
                        OutlinedButton(
                            onClick = { onAddNote() },
                            border = BorderStroke(1.dp, Accent),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("OK", style = MaterialTheme.typography.labelSmall.copy(color = Accent))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExercisePicker(
    definitions: List<ExerciseDefinition>,
    onSelect: (name: String, defId: String) -> Unit,
    onDefineNew: () -> Unit,
    onManage: () -> Unit
) {
    var filter by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GymInput(
            value = filter,
            onValueChange = { filter = it; expanded = true },
            placeholder = "Add exercise…",
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { if (it.isFocused) expanded = true }
        )
        Box(
            modifier = Modifier
                .background(SurfaceHi, RoundedCornerShape(6.dp))
                .border(1.dp, Border, RoundedCornerShape(6.dp))
                .clickable { expanded = !expanded }
                .padding(10.dp, 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (expanded) "▲" else "▼",
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 12.sp, color = TextDim)
            )
        }
    }

    if (expanded) {
        val active = definitions.filter { it.active }
        val matches = remember(filter, active) {
            if (filter.isBlank()) active
            else active.filter { it.name.lowercase().contains(filter.lowercase()) }
        }.take(8)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .background(SurfaceHi, RoundedCornerShape(6.dp))
                .border(1.dp, Border, RoundedCornerShape(6.dp))
        ) {
            matches.forEach { def ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(def.name, def.id); filter = ""; expanded = false }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(def.name, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp))
                        if (def.category.isNotEmpty()) {
                            Text(def.category, style = MaterialTheme.typography.labelSmall.copy(color = TextDim))
                        }
                    }
                }
                Divider(color = Border, thickness = 0.5.dp)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDefineNew(); filter = ""; expanded = false }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    "+ Define new exercise",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp, color = Accent)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onManage(); filter = ""; expanded = false }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    "Manage exercises",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp, color = TextDim)
                )
            }
        }
    }
}
