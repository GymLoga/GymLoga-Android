package com.ironlog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ironlog.data.*
import com.ironlog.ui.*
import com.ironlog.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = SessionRepository(applicationContext)
        val viewModel = IronLogViewModel(repository)

        setContent {
            IronLogApp(viewModel)
        }
    }
}

@Composable
fun IronLogApp(viewModel: IronLogViewModel) {
    val sessions by viewModel.sessions.collectAsState()
    
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Bg,
            surface = Surface,
            onBackground = Text,
            onSurface = Text
        ),
        typography = IronTypography
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Bg) {
            Column {
                Header(sessions.size)
                Tabs(viewModel.currentView, viewModel.editSessionId != null) { viewModel.currentView = it }
                
                Box(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                    when (viewModel.currentView) {
                        IronView.LOG -> LogView(viewModel, sessions)
                        IronView.HISTORY -> HistoryView(viewModel, sessions)
                        IronView.PRS -> PRsView(viewModel, sessions)
                        IronView.SESSION_DETAIL -> SessionDetailView(viewModel)
                        IronView.EXERCISE_HISTORY -> ExerciseHistoryView(viewModel, sessions)
                    }
                }
            }
        }
    }
}

@Composable
fun Header(sessionCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row {
            Text("IRON", style = MaterialTheme.typography.titleLarge)
            Text("LOG", style = MaterialTheme.typography.titleLarge, color = Text)
        }
        Text(
            "$sessionCount SESSION${if (sessionCount != 1) "S" else ""}",
            style = MaterialTheme.typography.labelSmall
        )
    }
    Divider(color = Border)
}

@Composable
fun Tabs(currentView: IronView, isEditing: Boolean, onTabClick: (IronView) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth()) {
        listOf(
            IronView.LOG to if (isEditing) "EDIT" else "LOG",
            IronView.HISTORY to "HISTORY",
            IronView.PRS to "PRs"
        ).forEach { (view, label) ->
            val isSelected = currentView == view || 
                (view == IronView.HISTORY && (currentView == IronView.SESSION_DETAIL || currentView == IronView.EXERCISE_HISTORY))
            
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
fun IronInput(
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

@Composable
fun LogView(viewModel: IronLogViewModel, sessions: List<Session>) {
    val scrollState = rememberScrollState()
    val allNames = DataLogic.getAllExerciseNames(sessions)

    Column(modifier = Modifier.verticalScroll(scrollState).padding(vertical = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IronInput(
                value = viewModel.aDate,
                onValueChange = { viewModel.aDate = it },
                placeholder = "YYYY-MM-DD",
                modifier = Modifier.width(120.dp)
            )
            IronInput(
                value = viewModel.aLabel,
                onValueChange = { viewModel.aLabel = it },
                placeholder = "Label (recovery, pull...)",
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        IronInput(
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
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 11.sp, color = TextDim, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
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
                // build suggestion list based on existing exercises
                val suggestions = remember(viewModel.curName, allNames) {
                    if (viewModel.curName.isBlank()) emptyList()
                    else allNames.filter {
                        it.lowercase().contains(viewModel.curName.lowercase()) &&
                                it.lowercase() != viewModel.curName.lowercase()
                    }.take(5)
                }

                IronInput(
                    value = viewModel.curName,
                    onValueChange = { viewModel.curName = it },
                    placeholder = "Exercise name",
                    modifier = Modifier.fillMaxWidth()
                )

                // use suggestions
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
                    IronInput(
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
                        IronInput(
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
                enabled = viewModel.aExercises.isNotEmpty(),
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryView(viewModel: IronLogViewModel, sessions: List<Session>) {
    val allNames = DataLogic.getAllExerciseNames(sessions)
    
    LazyColumn(modifier = Modifier.fillMaxSize().padding(vertical = 12.dp)) {
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
                                    viewModel.currentView = IronView.EXERCISE_HISTORY
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
                        viewModel.currentView = IronView.SESSION_DETAIL
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
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 11.sp, color = TextDim, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        )
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun PRsView(viewModel: IronLogViewModel, sessions: List<Session>) {
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
                        viewModel.currentView = IronView.EXERCISE_HISTORY
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
                            Text("${Math.round(pr.bestE1rm)}", style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Green))
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

@Composable
fun SessionDetailView(viewModel: IronLogViewModel) {
    val session = viewModel.selectedSession ?: return
    val context = LocalContext.current
    
    Column(modifier = Modifier.fillMaxSize().padding(vertical = 12.dp).verticalScroll(rememberScrollState())) {
        OutlinedButton(
            onClick = { viewModel.currentView = IronView.HISTORY },
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
                    onClick = { 
                        // In a real app we'd show a confirmation dialog
                        viewModel.deleteSession(session.id)
                    },
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
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 12.sp, color = TextDim, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
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
                        viewModel.currentView = IronView.EXERCISE_HISTORY
                    }
                )
                FlowRow(modifier = Modifier.padding(top = 4.dp)) {
                    ex.sets.forEach { SetBadge(it) }
                }
                if (ex.note.isNotEmpty()) {
                    Text(
                        ex.note,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 11.sp, color = TextDim, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Divider(modifier = Modifier.padding(top = 10.dp), color = Border.copy(alpha = 0.1f))
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun ExerciseHistoryView(viewModel: IronLogViewModel, sessions: List<Session>) {
    val name = viewModel.selectedExerciseName ?: return
    val history = DataLogic.getExerciseHistory(sessions, name)
    
    Column(modifier = Modifier.fillMaxSize().padding(vertical = 12.dp).verticalScroll(rememberScrollState())) {
        OutlinedButton(
            onClick = { viewModel.currentView = IronView.HISTORY },
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
                        "BEST: ${bestEntry.bestW}×${bestEntry.bestR} · est 1RM: ${Math.round(bestEntry.bestW * (1 + bestEntry.bestR.toDouble() / 30))}",
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
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 11.sp, color = TextDim, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
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

fun formatDate(iso: String): String {
    return try {
        val date = LocalDate.parse(iso)
        val formatter = DateTimeFormatter.ofPattern("EEE, MMM d")
        date.format(formatter)
    } catch (e: Exception) {
        iso
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
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
