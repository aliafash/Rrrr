package com.example.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Admin(
    val id: String? = null,
    val username: String,
    val password_hash: String,
    val role: String, // "super_admin" or "admin"
    val created_at: String? = null
)

@Serializable
data class Category(
    val id: String? = null,
    val name_ar: String,
    val icon: String? = null,
    val order_index: Int? = 0,
    val created_at: String? = null
)

@Serializable
data class ServiceProvider(
    val id: String? = null,
    val name: String,
    val phone: String,
    val category_id: String,
    val rating: Double? = 0.0,
    val image_url: String? = null,
    val is_active: Boolean = true,
    val created_at: String? = null
)

@Serializable
data class Review(
    val id: String? = null,
    val provider_id: String,
    val user_phone: String,
    val rating: Int, // 1 to 5
    val comment: String? = null,
    val created_at: String? = null
)

@Serializable
data class AppSetting(
    val key: String,
    val value: String
)
