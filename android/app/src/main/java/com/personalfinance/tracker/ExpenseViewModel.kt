package com.personalfinance.tracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.personalfinance.tracker.data.ApiException
import com.personalfinance.tracker.data.Category
import com.personalfinance.tracker.data.Expense
import com.personalfinance.tracker.data.NewCategory
import com.personalfinance.tracker.data.NewExpense
import com.personalfinance.tracker.data.SupabaseApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class UiState(
    val signedIn: Boolean = false,
    val email: String = "",
    val month: YearMonth = YearMonth.now(),
    val categories: List<Category> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
)

/** What the add-expense form collects. */
data class ExpenseDraft(
    val amount: Double,
    val categoryId: String,
    val note: String,
    val date: LocalDate,
    val store: String,
    /** "cash", "card" or null. */
    val paymentMethod: String?,
    val cardName: String,
)

class ExpenseViewModel(app: Application) : AndroidViewModel(app) {
    val api = SupabaseApi(app)

    private val _state = MutableStateFlow(
        UiState(signedIn = api.session != null, email = api.session?.user?.email.orEmpty()),
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun clearError() = _state.update { it.copy(error = null) }

    private fun fail(e: Throwable) {
        val signedIn = api.session != null
        _state.update { it.copy(error = e.message ?: "Something went wrong", loading = false, signedIn = signedIn) }
    }

    // ------------------------------------------------------------ auth

    /** Calls [onDone] with null on success, or a message to show. */
    fun signIn(email: String, password: String, onDone: (String?) -> Unit) = viewModelScope.launch {
        try {
            api.signIn(email.trim(), password)
            _state.update { it.copy(signedIn = true, email = api.session?.user?.email ?: email.trim()) }
            refresh()
            onDone(null)
        } catch (e: ApiException) {
            onDone(e.message)
        }
    }

    fun signUp(email: String, password: String, onDone: (String?) -> Unit) = viewModelScope.launch {
        try {
            if (api.signUp(email.trim(), password)) {
                _state.update { it.copy(signedIn = true, email = email.trim()) }
                refresh()
                onDone(null)
            } else {
                onDone("Check your email to confirm your account, then sign in.")
            }
        } catch (e: ApiException) {
            onDone(e.message)
        }
    }

    fun signOut() {
        api.signOut()
        _state.value = UiState()
    }

    // ------------------------------------------------------------ data

    fun refresh() = viewModelScope.launch {
        val month = _state.value.month
        _state.update { it.copy(loading = true, error = null) }
        try {
            val cats = api.categories()
            val exps = api.expenses(month.atDay(1).toString(), month.plusMonths(1).atDay(1).toString())
            _state.update { it.copy(categories = cats, expenses = exps, loading = false) }
        } catch (e: ApiException) {
            fail(e)
        }
    }

    fun changeMonth(delta: Long) {
        val next = _state.value.month.plusMonths(delta)
        if (next.isAfter(YearMonth.now())) return
        _state.update { it.copy(month = next, expenses = emptyList()) }
        refresh()
    }

    fun addExpense(draft: ExpenseDraft, onDone: (Boolean) -> Unit) =
        viewModelScope.launch {
            try {
                val created = api.addExpense(
                    NewExpense(
                        amount = draft.amount,
                        categoryId = draft.categoryId,
                        note = draft.note.trim().ifEmpty { null },
                        spentOn = draft.date.toString(),
                        store = draft.store.trim().ifEmpty { null },
                        paymentMethod = draft.paymentMethod,
                        cardName = if (draft.paymentMethod == "card") draft.cardName.trim().ifEmpty { null } else null,
                    ),
                )
                val date = draft.date
                val month = _state.value.month
                if (YearMonth.from(date) == month) {
                    _state.update { s ->
                        s.copy(
                            expenses = (s.expenses + created).sortedWith(
                                compareByDescending<Expense> { it.spentOn }.thenByDescending { it.createdAt },
                            ),
                        )
                    }
                }
                onDone(true)
            } catch (e: ApiException) {
                fail(e)
                onDone(false)
            }
        }

    fun deleteExpense(e: Expense) = viewModelScope.launch {
        try {
            api.deleteExpense(e.id)
            _state.update { s -> s.copy(expenses = s.expenses.filterNot { it.id == e.id }) }
        } catch (ex: ApiException) {
            fail(ex)
        }
    }

    fun addCategory(name: String, icon: String, color: String, onDone: (String?) -> Unit) = viewModelScope.launch {
        try {
            val created = api.addCategory(NewCategory(name.trim(), icon, color))
            _state.update { s ->
                s.copy(categories = (s.categories + created).sortedWith(compareBy({ it.sortOrder }, { it.name })))
            }
            onDone(null)
        } catch (e: ApiException) {
            onDone(e.message)
        }
    }

    fun deleteCategory(c: Category, onDone: (String?) -> Unit) = viewModelScope.launch {
        try {
            api.deleteCategory(c.id)
            _state.update { s -> s.copy(categories = s.categories.filterNot { it.id == c.id }) }
            onDone(null)
        } catch (e: ApiException) {
            onDone(e.message)
        }
    }
}
