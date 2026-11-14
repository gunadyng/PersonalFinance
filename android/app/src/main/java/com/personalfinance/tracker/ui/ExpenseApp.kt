package com.personalfinance.tracker.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personalfinance.tracker.ExpenseViewModel

@Composable
fun ExpenseApp(vm: ExpenseViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    var showCategories by rememberSaveable { mutableStateOf(false) }

    when {
        !vm.api.isConfigured -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("This build has no Supabase settings. Add SUPABASE_URL and SUPABASE_ANON_KEY (see README) and rebuild.")
        }
        !state.signedIn -> LoginScreen(vm)
        showCategories -> {
            BackHandler { showCategories = false }
            CategoriesScreen(vm, state, onBack = { showCategories = false })
        }
        else -> HomeScreen(vm, state, onManageCategories = { showCategories = true })
    }
}
