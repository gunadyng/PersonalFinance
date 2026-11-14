package com.personalfinance.tracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.personalfinance.tracker.ui.ExpenseApp
import com.personalfinance.tracker.ui.theme.ExpenseTheme

class MainActivity : ComponentActivity() {
    private val viewModel: ExpenseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExpenseTheme {
                ExpenseApp(viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Pick up anything added from the browser while the app was in the background.
        if (viewModel.state.value.signedIn) viewModel.refresh()
    }
}
