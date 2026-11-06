package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val _selectedTask = mutableStateOf(1)
    private val _taskResult = mutableStateOf("Select a task and tap 'Run Selected Task'")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MasterclassRunnerScreen(
                        selectedTaskId = _selectedTask.value,
                        taskResult = _taskResult.value,
                        onTaskSelected = { id -> _selectedTask.value = id },
                        onRunTask = { id -> runTask(id) },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    private fun runTask(id: Int) {
        _taskResult.value = "Running Task $id..."
        lifecycleScope.launch {
            try {
                val result = CoroutinesMasterclassRunner.runTask(id)
                _taskResult.value = "--- Task $id Result ---\n$result"
            } catch (e: Exception) {
                _taskResult.value = "Error in Task $id: ${e.localizedMessage}"
            }
        }
    }
}

@Composable
fun MasterclassRunnerScreen(
    selectedTaskId: Int,
    taskResult: String,
    onTaskSelected: (Int) -> Unit,
    onRunTask: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tasks = (1..15).map { "Task $it" } + (1..5).map { "Extra $it" }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Coroutines Masterclass Runner",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                items(tasks.indices.toList()) { index ->
                    val taskId = index + 1
                    val taskName = tasks[index]
                    val isSelected = selectedTaskId == taskId
                    Surface(
                        selected = isSelected,
                        onClick = { onTaskSelected(taskId) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        shape = MaterialTheme.shapes.small,
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = taskName,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onRunTask(selectedTaskId) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Run Selected Task ($selectedTaskId)")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.TopStart
            ) {
                Text(
                    text = taskResult,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
