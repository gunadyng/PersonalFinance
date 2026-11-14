package com.personalfinance.tracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.personalfinance.tracker.ExpenseViewModel
import com.personalfinance.tracker.UiState
import com.personalfinance.tracker.data.Category

private val COLORS = listOf("#f97316", "#22c55e", "#3b82f6", "#eab308", "#8b5cf6", "#ec4899", "#ef4444", "#06b6d4", "#64748b")
private val ICONS = listOf("🏷️", "🐶", "☕", "🍺", "🎮", "👶", "🏋️", "💼", "🔧", "📱", "🚕", "⛽", "🎓", "💐")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CategoriesScreen(vm: ExpenseViewModel, state: UiState, onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf(ICONS.first()) }
    var color by remember { mutableStateOf(COLORS.first()) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var toDelete by remember { mutableStateOf<Category?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categories") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("New category", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = name,
                            onValueChange = { if (it.length <= 40) name = it },
                            label = { Text("Name") },
                            leadingIcon = { Text(icon) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ICONS.forEach { i ->
                                AssistChip(
                                    onClick = { icon = i },
                                    label = { Text(i) },
                                    border = if (i == icon) {
                                        androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                    } else {
                                        null
                                    },
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            COLORS.forEach { c ->
                                Box(
                                    Modifier.size(30.dp).clip(CircleShape).background(parseColor(c))
                                        .then(
                                            if (c == color) {
                                                Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                            } else {
                                                Modifier
                                            },
                                        )
                                        .clickable { color = c },
                                )
                            }
                        }
                        message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        Button(
                            enabled = !busy && name.isNotBlank(),
                            onClick = {
                                busy = true
                                message = null
                                vm.addCategory(name, icon, color) { err ->
                                    busy = false
                                    message = err
                                    if (err == null) name = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Add category") }
                    }
                }
            }
            items(state.categories, key = { it.id }) { c ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(parseColor(c.color)))
                    Spacer(Modifier.width(12.dp))
                    Text("${c.icon}  ${c.name}", Modifier.weight(1f))
                    if (c.isDefault) {
                        Text(
                            "default",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { toDelete = c }) {
                        Icon(Icons.Filled.Delete, "Delete ${c.name}", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    toDelete?.let { c ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Delete “${c.name}”?") },
            text = { Text("Categories that already have expenses can't be deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    toDelete = null
                    vm.deleteCategory(c) { err -> message = err }
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancel") } },
        )
    }
}
