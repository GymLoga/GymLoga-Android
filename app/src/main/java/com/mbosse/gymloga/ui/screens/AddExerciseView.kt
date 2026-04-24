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

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mbosse.gymloga.ui.GymLogaViewModel
import com.mbosse.gymloga.ui.GymView
import com.mbosse.gymloga.ui.components.GymInput
import com.mbosse.gymloga.ui.theme.*

@Composable
fun AddExerciseView(viewModel: GymLogaViewModel) {
    val defId = viewModel.editDefinitionId
    val isEdit = defId != null
    val existingDef = remember(defId) {
        if (defId != null) viewModel.exerciseDefinitions.value.find { it.id == defId } else null
    }

    var name by remember(defId) { mutableStateOf(existingDef?.name ?: "") }
    var category by remember(defId) { mutableStateOf(existingDef?.category ?: "") }

    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                if (isEdit) "EDIT EXERCISE" else "DEFINE EXERCISE",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified)
            )
            TextButton(onClick = {
                viewModel.editDefinitionId = null
                viewModel.currentView = if (isEdit) GymView.MANAGE_EXERCISES else GymView.LOG
            }) {
                Text("← BACK", style = MaterialTheme.typography.labelSmall.copy(color = Accent))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Exercise name",
            style = MaterialTheme.typography.labelSmall.copy(color = TextDim),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        GymInput(
            value = name,
            onValueChange = { name = it },
            placeholder = "e.g. Bench Press",
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            "Category (optional)",
            style = MaterialTheme.typography.labelSmall.copy(color = TextDim),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        GymInput(
            value = category,
            onValueChange = { category = it },
            placeholder = "push, pull, legs, cardio…",
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (isEdit && defId != null) {
                    viewModel.updateExerciseDefinition(defId, name, category)
                    viewModel.editDefinitionId = null
                    viewModel.currentView = GymView.MANAGE_EXERCISES
                } else {
                    viewModel.addExerciseDefinition(name, category)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = name.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Accent,
                disabledContainerColor = TextDim.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text("SAVE", style = MaterialTheme.typography.labelSmall.copy(color = Bg, fontWeight = FontWeight.ExtraBold))
        }
    }
}
