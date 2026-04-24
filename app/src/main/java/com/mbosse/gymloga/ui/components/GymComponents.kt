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
package com.mbosse.gymloga.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mbosse.gymloga.data.WorkoutSet
import com.mbosse.gymloga.ui.GymView
import com.mbosse.gymloga.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun Header(sessionCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row {
            Text("GYM", style = MaterialTheme.typography.titleLarge)
            Text("LOGA", style = MaterialTheme.typography.titleLarge, color = Text)
        }
        Text(
            "$sessionCount SESSION${if (sessionCount != 1) "S" else ""}",
            style = MaterialTheme.typography.labelSmall
        )
    }
    Divider(color = Border)
}

@Composable
fun Tabs(currentView: GymView, isEditing: Boolean, onTabClick: (GymView) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth()) {
        listOf(
            GymView.LOG to if (isEditing) "EDIT" else "LOG",
            GymView.HISTORY to "HISTORY",
            GymView.PRS to "PRs"
        ).forEach { (view, label) ->
            val isSelected = currentView == view ||
                (view == GymView.HISTORY && (currentView == GymView.SESSION_DETAIL || currentView == GymView.EXERCISE_HISTORY))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTabClick(view) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) Accent else TextDim
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .height(2.dp)
                            .width(40.dp)
                            .background(if (isSelected) Accent else Color.Transparent)
                    )
                }
            }
        }
    }
    Divider(color = Border)
}

@Composable
fun GymInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    onDone: (() -> Unit)? = null
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .background(SurfaceHi, RoundedCornerShape(6.dp))
            .border(1.dp, Border, RoundedCornerShape(6.dp))
            .padding(10.dp, 12.dp),
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
        cursorBrush = SolidColor(Accent),
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(imeAction = if (onDone != null) ImeAction.Done else ImeAction.Default),
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
        decorationBox = { innerTextField ->
            if (value.isEmpty()) {
                Text(placeholder, style = MaterialTheme.typography.bodyLarge.copy(color = TextDim, fontSize = 14.sp))
            }
            innerTextField()
        }
    )
}

@Composable
fun SetBadge(set: WorkoutSet) {
    if (set.note != null && set.w == null && set.r == null) {
        Box(
            modifier = Modifier
                .padding(2.dp)
                .background(SurfaceHi, RoundedCornerShape(4.dp))
                .border(1.dp, Border, RoundedCornerShape(4.dp))
                .padding(vertical = 3.dp, horizontal = 8.dp)
        ) {
            Text(set.note, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 12.sp, color = TextDim))
        }
    } else {
        Box(
            modifier = Modifier
                .padding(2.dp)
                .background(SurfaceHi, RoundedCornerShape(4.dp))
                .border(1.dp, Border, RoundedCornerShape(4.dp))
                .padding(vertical = 3.dp, horizontal = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${set.w}", style = MaterialTheme.typography.bodyLarge.copy(fontSize = 12.sp, color = Accent, fontWeight = FontWeight.Bold))
                Text("×", style = MaterialTheme.typography.bodyLarge.copy(fontSize = 12.sp, color = TextDim))
                Text("${set.r}", style = MaterialTheme.typography.bodyLarge.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold))
                if (set.note != null) {
                    Text(" (${set.note})", style = MaterialTheme.typography.bodyLarge.copy(fontSize = 11.sp, color = TextDim))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    maxItemsInEachRow: Int = Int.MAX_VALUE,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = Arrangement.Top,
        maxItemsInEachRow = maxItemsInEachRow,
        content = { content() }
    )
}

fun formatDate(iso: String): String {
    return try {
        val date = LocalDate.parse(iso)
        val pattern = if (date.year != LocalDate.now().year) "EEE, MMM d, yyyy" else "EEE, MMM d"
        date.format(DateTimeFormatter.ofPattern(pattern))
    } catch (e: Exception) {
        iso
    }
}

fun formatVolume(lbs: Long): String =
    if (lbs >= 1000) "${"%.1f".format(lbs / 1000.0)}k lbs" else "$lbs lbs"
