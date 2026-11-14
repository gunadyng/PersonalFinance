package com.personalfinance.tracker.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val id: String,
    val name: String,
    val icon: String,
    val color: String,
    @SerialName("is_default") val isDefault: Boolean = false,
    @SerialName("sort_order") val sortOrder: Int = 100,
)

@Serializable
data class Expense(
    val id: String,
    val amount: Double,
    @SerialName("category_id") val categoryId: String,
    val note: String? = null,
    /** ISO date, YYYY-MM-DD. */
    @SerialName("spent_on") val spentOn: String,
    @SerialName("created_at") val createdAt: String,
    val store: String? = null,
    /** "cash", "card" or null when not recorded. */
    @SerialName("payment_method") val paymentMethod: String? = null,
    @SerialName("card_name") val cardName: String? = null,
)

@Serializable
data class NewExpense(
    val amount: Double,
    @SerialName("category_id") val categoryId: String,
    val note: String?,
    @SerialName("spent_on") val spentOn: String,
    val store: String?,
    @SerialName("payment_method") val paymentMethod: String?,
    @SerialName("card_name") val cardName: String?,
)

@Serializable
data class NewCategory(val name: String, val icon: String, val color: String)

@Serializable
data class Session(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    /** Unix seconds. */
    @SerialName("expires_at") val expiresAt: Long = 0,
    @SerialName("expires_in") val expiresIn: Long = 3600,
    val user: User? = null,
)

@Serializable
data class User(val id: String, val email: String? = null)

@Serializable
internal data class ApiError(
    val code: String? = null,
    val message: String? = null,
    val msg: String? = null,
    @SerialName("error_description") val errorDescription: String? = null,
)
