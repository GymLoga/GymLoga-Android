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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mbosse.gymloga.data.DataLogic
import com.mbosse.gymloga.data.Session
import com.mbosse.gymloga.ui.GymLogaViewModel
import com.mbosse.gymloga.ui.GymView
import com.mbosse.gymloga.ui.components.FlowRow
import com.mbosse.gymloga.ui.components.SetBadge
import com.mbosse.gymloga.ui.components.formatDate
import com.mbosse.gymloga.ui.theme.*

@Composable
fun ExerciseHistoryView(viewModel: GymLogaViewModel, sessions: List<Session>) {
    val name = viewModel.selectedExerciseName ?: return
    val history = DataLogic.getExerciseHistory(sessions, name)

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

        Text(name.uppercase(), style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Accent, letterSpacing = 1.sp))

        if (history.isEmpty()) {
            Text("No data.", style = MaterialTheme.typography.bodyLarge.copy(color = TextDim), modifier = Modifier.padding(top = 8.dp))
        } else {
            val bestEntry = history.maxByOrNull { it.bestW }
            if (bestEntry != null && bestEntry.bestW > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .background(Surface, RoundedCornerShape(6.dp))
                        .border(1.dp, Green.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(10.dp, 8.dp)
                ) {
                    Text(
                        "BEST: ${bestEntry.bestW}×${bestEntry.bestR} · est 1RM: ${kotlin.math.round(bestEntry.bestW * (1 + bestEntry.bestR.toDouble() / 30)).toLong()}",
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 12.sp, color = Green, fontWeight = FontWeight.Bold)
                    )
                }
            }

            history.forEach { entry ->
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(formatDate(entry.date), style = MaterialTheme.typography.bodyLarge.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Accent))
                        if (entry.label.isNotEmpty()) {
                            Text(entry.label, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 11.sp, color = TextDim))
                        }
                    }
                    FlowRow(modifier = Modifier.padding(top = 4.dp)) {
                        entry.sets.forEach { SetBadge(it) }
                    }
                    if (entry.note.isNotEmpty()) {
                        Text(
                            entry.note,
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 11.sp, color = TextDim, fontStyle = FontStyle.Italic),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Divider(modifier = Modifier.padding(top = 10.dp), color = Border.copy(alpha = 0.1f))
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}
