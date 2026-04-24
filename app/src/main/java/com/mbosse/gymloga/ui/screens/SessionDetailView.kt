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
import com.mbosse.gymloga.ui.GymLogaViewModel
import com.mbosse.gymloga.ui.GymView
import com.mbosse.gymloga.ui.components.FlowRow
import com.mbosse.gymloga.ui.components.SetBadge
import com.mbosse.gymloga.ui.components.formatDate
import com.mbosse.gymloga.ui.components.formatVolume
import com.mbosse.gymloga.ui.theme.*

@Composable
fun SessionDetailView(viewModel: GymLogaViewModel) {
    val session = viewModel.selectedSession ?: return
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete session?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.deleteSession(session.id)
                }) { Text("DELETE", color = Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("CANCEL") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(vertical = 12.dp).verticalScroll(rememberScrollState())) {
        OutlinedButton(
            onClick = { viewModel.currentView = GymView.HISTORY },
            border = BorderStroke(1.dp, Accent),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.padding(bottom = 10.dp),
            contentPadding = PaddingValues(10.dp, 5.dp)
        ) {
            Text("← BACK", style = MaterialTheme.typography.labelSmall.copy(color = Accent))
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(formatDate(session.date), style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Accent))
                if (session.label.isNotEmpty()) {
                    Text(session.label, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold))
                }
                val vol = DataLogic.getSessionVolume(session)
                if (vol > 0) {
                    Text(formatVolume(vol) + " total volume", style = MaterialTheme.typography.bodyLarge.copy(fontSize = 11.sp, color = TextDim))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = { viewModel.editSession(session) },
                    border = BorderStroke(1.dp, Blue),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(8.dp, 5.dp)
                ) {
                    Text("EDIT", style = MaterialTheme.typography.labelSmall.copy(color = Blue))
                }
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    border = BorderStroke(1.dp, Red),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(8.dp, 5.dp)
                ) {
                    Text("DEL", style = MaterialTheme.typography.labelSmall.copy(color = Red))
                }
            }
        }

        if (session.note.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .background(Surface, RoundedCornerShape(6.dp))
                    .border(1.dp, Border, RoundedCornerShape(6.dp))
                    .padding(10.dp, 8.dp)
            ) {
                Text(
                    session.note,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 12.sp, color = TextDim, fontStyle = FontStyle.Italic)
                )
            }
        }

        session.exercises.forEach { ex ->
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                Text(
                    ex.name.uppercase(),
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                    modifier = Modifier.clickable {
                        viewModel.selectedExerciseName = ex.name
                        viewModel.currentView = GymView.EXERCISE_HISTORY
                    }
                )
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
                Divider(modifier = Modifier.padding(top = 10.dp), color = Border.copy(alpha = 0.1f))
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}
