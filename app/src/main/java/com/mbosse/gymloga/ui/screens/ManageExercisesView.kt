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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mbosse.gymloga.data.ExerciseDefinition
import com.mbosse.gymloga.ui.GymLogaViewModel
import com.mbosse.gymloga.ui.GymView
import com.mbosse.gymloga.ui.theme.*

@Composable
fun ManageExercisesView(viewModel: GymLogaViewModel) {
    val definitions by viewModel.exerciseDefinitions.collectAsState()
    val sorted = remember(definitions) {
        definitions.sortedWith(compareBy({ !it.active }, { it.name.lowercase() }))
    }

    Column(modifier = Modifier.fillMaxSize().padding(vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("MANAGE EXERCISES", style = MaterialTheme.typography.labelSmall)
            TextButton(onClick = { viewModel.currentView = GymView.LOG }) {
                Text("← BACK", style = MaterialTheme.typography.labelSmall.copy(color = Accent))
            }
        }

        if (definitions.isEmpty()) {
            Text(
                "No exercises defined yet.",
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 13.sp, color = TextDim),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(sorted, key = { it.id }) { def ->
                ExerciseRow(
                    def = def,
                    onEdit = {
                        viewModel.editDefinitionId = def.id
                        viewModel.currentView = GymView.ADD_EXERCISE
                    },
                    onToggleActive = { viewModel.setExerciseActive(def.id, !def.active) }
                )
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun ExerciseRow(
    def: ExerciseDefinition,
    onEdit: () -> Unit,
    onToggleActive: () -> Unit
) {
    val alpha = if (def.active) 1f else 0.4f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .background(Surface.copy(alpha = alpha), RoundedCornerShape(8.dp))
            .border(1.dp, Border.copy(alpha = alpha), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                def.name,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (def.active) MaterialTheme.colorScheme.onSurface else TextDim
                )
            )
            if (def.category.isNotEmpty()) {
                Text(
                    def.category,
                    style = MaterialTheme.typography.labelSmall.copy(color = TextDim.copy(alpha = alpha))
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "EDIT",
                style = MaterialTheme.typography.labelSmall.copy(color = Accent.copy(alpha = alpha)),
                modifier = Modifier.clickable { onEdit() }
            )
            Text(
                if (def.active) "HIDE" else "SHOW",
                style = MaterialTheme.typography.labelSmall.copy(color = if (def.active) Red else TextDim),
                modifier = Modifier.clickable { onToggleActive() }
            )
        }
    }
}
