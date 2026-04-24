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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mbosse.gymloga.data.DataLogic
import com.mbosse.gymloga.data.Session
import com.mbosse.gymloga.ui.GymLogaViewModel
import com.mbosse.gymloga.ui.GymView
import com.mbosse.gymloga.ui.components.formatDate
import com.mbosse.gymloga.ui.theme.*

@Composable
fun PRsView(viewModel: GymLogaViewModel, sessions: List<Session>) {
    val prs = DataLogic.getAllPRs(sessions)

    LazyColumn(modifier = Modifier.fillMaxSize().padding(vertical = 12.dp)) {
        item {
            Text(
                "PERSONAL RECORDS · ${prs.size} EXERCISE${if (prs.size != 1) "S" else ""}",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(bottom = 10.dp)
            )
        }

        if (prs.isEmpty()) {
            item {
                Text(
                    "Log some sessions to see your PRs here.",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 13.sp, color = TextDim),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        items(prs) { pr ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .background(Surface, RoundedCornerShape(8.dp))
                    .border(1.dp, Border, RoundedCornerShape(8.dp))
                    .clickable {
                        viewModel.selectedExerciseName = pr.name
                        viewModel.exerciseHistorySource = GymView.PRS
                        viewModel.currentView = GymView.EXERCISE_HISTORY
                    }
                    .padding(12.dp, 14.dp)
            ) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(pr.name.uppercase(), style = MaterialTheme.typography.bodyLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold))
                        Text(
                            "${pr.totalSessions} session${if (pr.totalSessions != 1) "s" else ""} · ${pr.totalSets} set${if (pr.totalSets != 1) "s" else ""}",
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 10.sp, color = TextDim)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(modifier = Modifier.width(100.dp)) {
                            Text("HEAVIEST", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text("${pr.bestW}", style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Accent))
                                Text("×${pr.bestWR}", style = MaterialTheme.typography.bodyLarge.copy(fontSize = 12.sp, color = TextDim))
                            }
                            Text(formatDate(pr.bestWDate), style = MaterialTheme.typography.bodyLarge.copy(fontSize = 10.sp, color = TextDim))
                        }
                        Column(modifier = Modifier.width(100.dp)) {
                            Text("EST 1RM", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                            Text("${kotlin.math.round(pr.bestE1rm).toLong()}", style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Green))
                            Text(
                                "${pr.bestE1rmW}×${pr.bestE1rmR} · ${formatDate(pr.bestE1rmDate)}",
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 10.sp, color = TextDim)
                            )
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}
