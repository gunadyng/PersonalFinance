package com.personalfinance.tracker.data

import android.content.Context
import com.personalfinance.tracker.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiException(message: String) : Exception(message)

/**
 * A small client for Supabase's REST (PostgREST) and Auth (GoTrue) endpoints.
 * Talks to the same database as the web app, so data stays in sync.
 */
class SupabaseApi(context: Context) {
    private val baseUrl = BuildConfig.SUPABASE_URL
    private val anonKey = BuildConfig.SUPABASE_ANON_KEY
    private val prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = true }
    private val jsonType = "application/json".toMediaType()
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
    private val refreshLock = Mutex()

    val isConfigured: Boolean get() = baseUrl.isNotBlank() && anonKey.isNotBlank()

    var session: Session? = prefs.getString(KEY, null)?.let {
        runCatching { json.decodeFromString<Session>(it) }.getOrNull()
    }
        private set(value) {
            field = value
            prefs.edit().apply {
                if (value == null) remove(KEY) else putString(KEY, json.encodeToString(value))
            }.apply()
        }

    // ---------------------------------------------------------------- auth

    suspend fun signIn(email: String, password: String) {
        val body = buildJsonObject { put("email", email); put("password", password) }.toString()
        session = withExpiry(json.decodeFromString(authPost("token?grant_type=password", body)))
    }

    /** Returns true when signed in straight away, false when the account needs email confirmation. */
    suspend fun signUp(email: String, password: String): Boolean {
        val body = buildJsonObject { put("email", email); put("password", password) }.toString()
        val raw = authPost("signup", body)
        val parsed = runCatching { json.decodeFromString<Session>(raw) }.getOrNull()
        if (parsed != null) session = withExpiry(parsed)
        return parsed != null
    }

    fun signOut() {
        session = null
    }

    private suspend fun authPost(path: String, body: String): String = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("$baseUrl/auth/v1/$path")
            .header("apikey", anonKey)
            .post(body.toRequestBody(jsonType))
            .build()
        execute(req)
    }

    private fun withExpiry(s: Session): Session =
        if (s.expiresAt > 0) s else s.copy(expiresAt = nowSeconds() + s.expiresIn)

    private suspend fun accessToken(): String = refreshLock.withLock {
        val current = session ?: throw ApiException("Signed out")
        if (current.expiresAt - 60 > nowSeconds()) return current.accessToken
        val body = buildJsonObject { put("refresh_token", current.refreshToken) }.toString()
        try {
            val refreshed = withExpiry(json.decodeFromString(authPost("token?grant_type=refresh_token", body)))
            session = refreshed
            refreshed.accessToken
        } catch (e: ApiException) {
            session = null // refresh token revoked or expired: sign in again
            throw ApiException("Your session expired. Please sign in again.")
        }
    }

    // ---------------------------------------------------------------- data

    suspend fun categories(): List<Category> {
        val url = rest("categories")
            .addQueryParameter("select", "id,name,icon,color,is_default,sort_order")
            .addQueryParameter("order", "sort_order.asc,name.asc")
            .build()
        var list = json.decodeFromString<List<Category>>(send(url, "GET"))
        if (list.isEmpty()) {
            send(rest("rpc/ensure_default_categories").build(), "POST", "{}")
            list = json.decodeFromString(send(url, "GET"))
        }
        return list
    }

    suspend fun addCategory(c: NewCategory): Category =
        json.decodeFromString<List<Category>>(
            send(rest("categories").build(), "POST", json.encodeToString(c), returnRow = true),
        ).first()

    suspend fun deleteCategory(id: String) {
        send(rest("categories").addQueryParameter("id", "eq.$id").build(), "DELETE")
    }

    /** Expenses with spent_on in [from, to), both YYYY-MM-DD. */
    suspend fun expenses(from: String, to: String): List<Expense> {
        val url = rest("expenses")
            .addQueryParameter("select", "id,amount,category_id,note,spent_on,created_at,store,payment_method,card_name")
            .addQueryParameter("spent_on", "gte.$from")
            .addQueryParameter("spent_on", "lt.$to")
            .addQueryParameter("order", "spent_on.desc,created_at.desc")
            .build()
        return json.decodeFromString(send(url, "GET"))
    }

    suspend fun addExpense(e: NewExpense): Expense =
        json.decodeFromString<List<Expense>>(
            send(rest("expenses").build(), "POST", json.encodeToString(e), returnRow = true),
        ).first()

    suspend fun deleteExpense(id: String) {
        send(rest("expenses").addQueryParameter("id", "eq.$id").build(), "DELETE")
    }

    // ---------------------------------------------------------------- plumbing

    private fun rest(path: String): HttpUrl.Builder = "$baseUrl/rest/v1/$path".toHttpUrl().newBuilder()

    private suspend fun send(url: HttpUrl, method: String, body: String? = null, returnRow: Boolean = false): String {
        val token = accessToken()
        return withContext(Dispatchers.IO) {
            val req = Request.Builder()
                .url(url)
                .header("apikey", anonKey)
                .header("Authorization", "Bearer $token")
                .apply { if (returnRow) header("Prefer", "return=representation") }
                .method(method, body?.toRequestBody(jsonType))
                .build()
            execute(req)
        }
    }

    private fun execute(req: Request): String {
        val response = try {
            http.newCall(req).execute()
        } catch (e: IOException) {
            throw ApiException("Can't reach the server. Check your connection.")
        }
        response.use {
            val text = it.body?.string().orEmpty()
            if (it.isSuccessful) return text
            val err = runCatching { json.decodeFromString<ApiError>(text) }.getOrNull()
            throw ApiException(
                when (err?.code) {
                    "23505" -> "A category with that name already exists."
                    "23503" -> "That category still has expenses. Delete them first."
                    else -> err?.errorDescription ?: err?.msg ?: err?.message ?: "Request failed (${it.code})"
                },
            )
        }
    }

    private fun nowSeconds() = System.currentTimeMillis() / 1000

    private companion object {
        const val KEY = "session"
    }
}
