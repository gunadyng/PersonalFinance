package com.personalfinance.tracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.personalfinance.tracker.ExpenseViewModel
import com.personalfinance.tracker.UiState
import com.personalfinance.tracker.data.Category
import com.personalfinance.tracker.data.Expense
import java.time.YearMonth
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: ExpenseViewModel, state: UiState, onManageCategories: () -> Unit) {
    var adding by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<Expense?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val byId = remember(state.categories) { state.categories.associateBy { it.id } }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbar.showSnackbar(it)
            vm.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("💰 Expenses") },
                actions = {
                    IconButton(onClick = { vm.refresh() }) { Icon(Icons.Filled.Refresh, "Refresh") }
                    Box {
                        IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, "More") }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Manage categories") },
                                onClick = { menuOpen = false; onManageCategories() },
                            )
                            DropdownMenuItem(
                                text = { Text("Sign out (${state.email})", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                onClick = { menuOpen = false; vm.signOut() },
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { adding = true },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Add expense") },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth()) else Spacer(Modifier.height(4.dp))
            LazyColumn(
                contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { SummaryCard(state, byId, vm::changeMonth) }
                val days = state.expenses.groupBy { it.spentOn }.toList()
                if (days.isEmpty() && !state.loading) {
                    item {
                        Text(
                            "No expenses this month yet. Tap “Add expense” to log one.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                }
                items(days, key = { it.first }) { (day, items) ->
                    DayCard(day, items, byId, onDelete = { toDelete = it })
                }
            }
        }
    }

    if (adding) {
        AddExpenseSheet(
            categories = state.categories,
            onDismiss = { adding = false },
            onSave = { amount, categoryId, note, date, done -> vm.addExpense(amount, categoryId, note, date, done) },
            onManageCategories = { adding = false; onManageCategories() },
        )
    }

    toDelete?.let { e ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Delete ${money(e.amount)}?") },
            text = { Text(byId[e.categoryId]?.name ?: "") },
            confirmButton = {
                TextButton(onClick = { vm.deleteExpense(e); toDelete = null }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SummaryCard(state: UiState, byId: Map<String, Category>, changeMonth: (Long) -> Unit) {
    val total = state.expenses.sumOf { it.amount }
    val breakdown = state.expenses.groupBy { it.categoryId }
        .mapValues { (_, v) -> v.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { changeMonth(-1) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous month")
                }
                Text(
                    monthLabel(state.month),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                IconButton(onClick = { changeMonth(1) }, enabled = state.month < YearMonth.now()) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next month")
                }
            }
            Text(money(total), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Text(
                "${state.expenses.size} expense${if (state.expenses.size == 1) "" else "s"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (total > 0) {
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    breakdown.forEach { (id, sum) ->
                        Box(
                            Modifier.weight((sum / total).toFloat().coerceAtLeast(0.005f)).height(10.dp)
                                .background(parseColor(byId[id]?.color ?: "#64748b")),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                breakdown.forEach { (id, sum) ->
                    val c = byId[id]
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(parseColor(c?.color ?: "#64748b")))
                        Spacer(Modifier.width(10.dp))
                        Text("${c?.icon ?: ""} ${c?.name ?: "Unknown"}", Modifier.weight(1f))
                        Text(
                            "${(sum / total * 100).roundToInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(money(sum), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCard(day: String, items: List<Expense>, byId: Map<String, Category>, onDelete: (Expense) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Text(dayLabel(day), style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                Text(money(items.sumOf { it.amount }), style = MaterialTheme.typography.labelLarge)
            }
            HorizontalDivider()
            items.forEach { e ->
                val c = byId[e.categoryId]
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                            .background(parseColor(c?.color ?: "#64748b")),
                        contentAlignment = Alignment.Center,
                    ) { Text(c?.icon ?: "") }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c?.name ?: "Unknown")
                        e.note?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Text(money(e.amount), fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = { onDelete(e) }) {
                        Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
