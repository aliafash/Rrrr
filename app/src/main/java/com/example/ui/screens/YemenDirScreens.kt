package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.Admin
import com.example.data.model.Category
import com.example.data.model.ServiceProvider
import com.example.data.model.Review
import com.example.data.model.AppSetting
import com.example.ui.DaliliViewModel
import kotlinx.coroutines.launch

// Color HEX standard parser
fun parseHexColor(hex: String, defaultColor: Color): Color {
    return try {
        val cleanHex = hex.trim().replace("#", "")
        if (cleanHex.length == 6) {
            Color(android.graphics.Color.parseColor("#$cleanHex"))
        } else if (cleanHex.length == 8) {
            Color(android.graphics.Color.parseColor("#$cleanHex"))
        } else {
            defaultColor
        }
    } catch (e: Exception) {
        defaultColor
    }
}

// Icon mapper using core Compose icons to guarantee compile safety
fun getCoreCategoryIcon(iconName: String?): ImageVector {
    return when (iconName?.lowercase()) {
        "build" -> Icons.Default.Build
        "computer" -> Icons.Default.Settings
        "school" -> Icons.Default.List
        "face" -> Icons.Default.Face
        "car" -> Icons.Default.LocationOn
        "home" -> Icons.Default.Home
        "shipping", "delivery" -> Icons.Default.Share
        "work" -> Icons.Default.Person
        "star" -> Icons.Default.Star
        "phone" -> Icons.Default.Phone
        "settings" -> Icons.Default.Settings
        "person" -> Icons.Default.Person
        else -> Icons.Default.List
    }
}

fun isEmojiIcon(iconName: String?): Boolean {
    if (iconName.isNullOrEmpty()) return false
    val firstCode = iconName.codePointAt(0)
    return firstCode > 127 || iconName.trim().length in 1..2
}

@Composable
fun CategoryIconView(
    iconName: String?,
    contentDescription: String?,
    tint: Color,
    size: androidx.compose.ui.unit.Dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 24.sp
) {
    if (isEmojiIcon(iconName)) {
        Text(
            text = iconName ?: "",
            fontSize = fontSize,
            textAlign = TextAlign.Center
        )
    } else {
        Icon(
            imageVector = getCoreCategoryIcon(iconName),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(size)
        )
    }
}

// Available standard icons for selections
val AVAILABLE_ICONS = listOf("build", "computer", "school", "face", "car", "home", "shipping", "work")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DaliliAppContainer(viewModel: DaliliViewModel) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Theme setup based on dynamic state
    val dynamicPrimary = parseHexColor(viewModel.primaryColorStr, Color(0xFF000000))
    val dynamicSecondary = parseHexColor(viewModel.secondaryColorStr, Color(0xFFFFD700))

    // Application screen routes states
    // "splash", "main", "providers", "login", "dashboard"
    var currentScreen by remember { mutableStateOf("splash") }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }

    // Screen redirection timer for Splash Screen
    LaunchedEffect(currentScreen) {
        if (currentScreen == "splash") {
            kotlinx.coroutines.delay(2000)
            currentScreen = "main"
        }
    }

    // Force RTL Globally to guarantee Arabic alignments
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFFFFFFF)
        ) {
            Scaffold(
                bottomBar = {
                    // Constant Promotional / Sponsor Footer
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFFFFFF))
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = viewModel.footerText,
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    when (currentScreen) {
                        "splash" -> SplashScreenView(dynamicPrimary, dynamicSecondary, viewModel)
                        "main" -> MainCatalogView(
                            viewModel = viewModel,
                            primaryColor = dynamicPrimary,
                            secondaryColor = dynamicSecondary,
                            onCategoryClick = { category ->
                                selectedCategory = category
                                currentScreen = "providers"
                            },
                            onProfileClick = {
                                if (viewModel.loggedInAdmin != null) {
                                    currentScreen = "dashboard"
                                } else {
                                    currentScreen = "login"
                                }
                            }
                        )
                        "providers" -> ProvidersListView(
                            viewModel = viewModel,
                            category = selectedCategory!!,
                            primaryColor = dynamicPrimary,
                            secondaryColor = dynamicSecondary,
                            onBackClick = { currentScreen = "main" }
                        )
                        "login" -> LoginScreenView(
                            viewModel = viewModel,
                            primaryColor = dynamicPrimary,
                            secondaryColor = dynamicSecondary,
                            onLoginSuccess = {
                                currentScreen = "dashboard"
                                Toast.makeText(context, "أهلاً بك؛ تم الدخول بنجاح", Toast.LENGTH_SHORT).show()
                            },
                            onBackClick = { currentScreen = "main" }
                        )
                        "dashboard" -> AdminDashboardView(
                            viewModel = viewModel,
                            primaryColor = dynamicPrimary,
                            secondaryColor = dynamicSecondary,
                            onLogoutClick = {
                                viewModel.logout()
                                currentScreen = "main"
                                Toast.makeText(context, "تم تسجيل الخروج", Toast.LENGTH_SHORT).show()
                            },
                            onBackClick = { currentScreen = "main" }
                        )
                    }

                    // Weak Network Warning overlay
                    if (viewModel.networkStatus.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFD32F2F)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(16.dp)
                                .fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = viewModel.networkStatus, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 1. SPLASH SCREEN
// ==========================================
@Composable
fun SplashScreenView(primaryColor: Color, secondaryColor: Color, viewModel: DaliliViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000)), // Set black as demanded in Splash
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Dynamic or Local Logo design
            if (viewModel.logoUrl.isNotEmpty()) {
                AsyncImage(
                    model = viewModel.logoUrl,
                    contentDescription = "Logo",
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(2.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(Color.Black)
                        .border(3.dp, secondaryColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "د",
                        color = secondaryColor,
                        fontSize = 45.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = viewModel.appName,
                color = secondaryColor,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "دليلك الأول للخدمات الصيانة والتعليم في اليمن",
                color = Color.Gray,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

// ==========================================
// 2. MAIN CATALOG VIEW
// ==========================================
@Composable
fun MainCatalogView(
    viewModel: DaliliViewModel,
    primaryColor: Color,
    secondaryColor: Color,
    onCategoryClick: (Category) -> Unit,
    onProfileClick: () -> Unit
) {
    val categoriesState by viewModel.categories.collectAsState()
    val providersState by viewModel.serviceProviders.collectAsState()
    val scope = rememberCoroutineScope()

    // Filter categories based on search input of category names or service provider names
    val filteredCategories = categoriesState.filter { cat ->
        cat.name_ar.contains(viewModel.searchQuery, ignoreCase = true) ||
        providersState.any { prov -> prov.category_id == cat.id && prov.name.contains(viewModel.searchQuery, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFFFF))
    ) {
        // Upper Home Header Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(primaryColor)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (viewModel.logoUrl.isNotEmpty()) {
                    AsyncImage(
                        model = viewModel.logoUrl,
                        contentDescription = "Logo",
                        modifier = Modifier
                            .size(45.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black)
                            .border(2.dp, secondaryColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "خ", color = secondaryColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = viewModel.appName,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "دليل خدمات اليمن الذكي",
                        color = secondaryColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Profile Button toggling Admin Control panel or Login form
            IconButton(
                onClick = onProfileClick,
                modifier = Modifier.testTag("profile_button")
            ) {
                Icon(
                    imageVector = if (viewModel.loggedInAdmin != null) Icons.Default.Person else Icons.Default.Lock,
                    contentDescription = "الملف الشخصي",
                    tint = secondaryColor,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Full Screen Search Box
        OutlinedTextField(
            value = viewModel.searchQuery,
            onValueChange = { viewModel.searchQuery = it },
            placeholder = { Text("بحث عن قسم أو مجال الصيانة...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primaryColor,
                unfocusedBorderColor = Color.LightGray,
                focusedContainerColor = Color(0xFFF9F9F9),
                unfocusedContainerColor = Color(0xFFF9F9F9)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("search_field"),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        // Sections Category Grids
        Text(
            text = "الأقسام المتاحة",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF212121),
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )

        if (viewModel.isLoading) {
            Box(modifier = Modifier.fillWeight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = primaryColor)
            }
        } else if (filteredCategories.isEmpty()) {
            Box(modifier = Modifier.fillWeight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = "لا توجد نتائج", tint = Color.Gray, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "لم نجد هذا القسم حالياً في دليلي", color = Color.Gray, fontSize = 14.sp)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillWeight(1f)
            ) {
                items(filteredCategories) { category ->
                    CategoryCardItem(
                        category = category,
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor,
                        onClick = { onCategoryClick(category) }
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryCardItem(category: Category, primaryColor: Color, secondaryColor: Color, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("category_card_${category.id}")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(primaryColor)
                    .border(2.dp, secondaryColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                CategoryIconView(
                    iconName = category.icon,
                    contentDescription = category.name_ar,
                    tint = secondaryColor,
                    size = 28.dp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = category.name_ar,
                color = Color(0xFF212121),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// Helper to fill rest weight
fun Modifier.fillWeight(weight: Float): Modifier = this.then(
    Modifier.fillMaxHeight().fillMaxWidth()
)

// ==========================================
// 3. SERVICE PROVIDERS SCREEN
// ==========================================
@Composable
fun ProvidersListView(
    viewModel: DaliliViewModel,
    category: Category,
    primaryColor: Color,
    secondaryColor: Color,
    onBackClick: () -> Unit
) {
    val providersState by viewModel.serviceProviders.collectAsState()
    val reviewsState by viewModel.reviews.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Filter service providers belonging to this category and matching query if necessary
    val filteredProviders = providersState.filter {
        it.category_id == category.id && it.is_active
    }

    // Modal Sheet State parameters for reviewing
    var showReviewListDialog by remember { mutableStateOf<ServiceProvider?>(null) }
    var showAddReviewDialog by remember { mutableStateOf<ServiceProvider?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFFFF))
    ) {
        // App top header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(primaryColor)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = category.name_ar,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (viewModel.isLoading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = primaryColor)
            }
        } else if (filteredProviders.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = "فارغ", tint = Color.LightGray, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "لا يوجد مقدمو خدمات مسجلون في هذا القسم حالياً.", color = Color.Gray, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredProviders) { provider ->
                    ProviderItemCard(
                        provider = provider,
                        allReviews = reviewsState,
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor,
                        onCallClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${provider.phone}"))
                            context.startActivity(intent)
                        },
                        onWhatsappClick = {
                            var cleanPhone = provider.phone.trim().replace("+", "").replace(" ", "")
                            if (cleanPhone.length == 9 && cleanPhone.startsWith("7")) {
                                cleanPhone = "967$cleanPhone"
                            }
                            val url = "https://api.whatsapp.com/send?phone=$cleanPhone"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "لم نتمكن من فتح تطبيق واتساب", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onViewComments = { showReviewListDialog = provider },
                        onAddReview = { showAddReviewDialog = provider }
                    )
                }
            }
        }
    }

    // Modal windows
    if (showReviewListDialog != null) {
        ReviewListDialogView(
            provider = showReviewListDialog!!,
            allReviews = reviewsState,
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
            onDismiss = { showReviewListDialog = null }
        )
    }

    if (showAddReviewDialog != null) {
        AddReviewDialogView(
            provider = showAddReviewDialog!!,
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
            onDismiss = { showAddReviewDialog = null },
            onSubmit = { phone, stars, comment ->
                viewModel.submitReview(
                    providerId = showAddReviewDialog!!.id ?: "",
                    phoneInput = phone,
                    ratingStars = stars,
                    commentText = comment,
                    onSuccess = {
                        showAddReviewDialog = null
                        Toast.makeText(context, "شكراً لك! تم نشر تقييمك لمورد الخدمة.", Toast.LENGTH_SHORT).show()
                    },
                    onError = { err ->
                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                    }
                )
            }
        )
    }
}

@Composable
fun ProviderItemCard(
    provider: ServiceProvider,
    allReviews: List<Review>,
    primaryColor: Color,
    secondaryColor: Color,
    onCallClick: () -> Unit,
    onWhatsappClick: () -> Unit,
    onViewComments: () -> Unit,
    onAddReview: () -> Unit
) {
    // Lookup review ratings dynamically
    val providerReviews = allReviews.filter { it.provider_id == provider.id }
    val averageRating = if (providerReviews.isNotEmpty()) {
        providerReviews.map { it.rating }.average()
    } else {
        provider.rating ?: 0.0
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("provider_item_${provider.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Provider banner
                if (!provider.image_url.isNullOrEmpty()) {
                    AsyncImage(
                        model = provider.image_url,
                        contentDescription = "provider visual",
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.LightGray),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFEEEEEE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Person, contentDescription = "avatar", tint = Color.Gray)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = provider.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "تلفون: ${provider.phone}", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Star rating visuals
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        repeat(5) { index ->
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (index < averageRating.toInt()) secondaryColor else Color.LightGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = String.format("%.1f", averageRating),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF212121)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Divider(color = Color(0xFFF0F0F0))
            Spacer(modifier = Modifier.height(10.dp))

            // Action Call/Whatsapp/Reviews buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onCallClick,
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("provider_call_${provider.id}"),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = "اتصال", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("اتصال مباشر", fontSize = 12.sp)
                }

                Button(
                    onClick = onWhatsappClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("provider_whatsapp_${provider.id}"),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "واتساب", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("واتساب", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onViewComments,
                    border = BorderStroke(1.dp, primaryColor),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Text("التعليقات", color = primaryColor, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onAddReview,
                border = BorderStroke(1.dp, secondaryColor),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().testTag("add_review_button_${provider.id}"),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                Icon(Icons.Default.Star, contentDescription = "تقييم", tint = secondaryColor, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("إضافة تقييم جديد للخدمة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Dialog to list all comments and reviews for a provider
@Composable
fun ReviewListDialogView(
    provider: ServiceProvider,
    allReviews: List<Review>,
    primaryColor: Color,
    secondaryColor: Color,
    onDismiss: () -> Unit
) {
    val reviews = allReviews.filter { it.provider_id == provider.id }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "تعليقات وتقييمات للخدمة : ${provider.name}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (reviews.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "لا توجد تعليقات منشورة بعد لهذا المقدم.", color = Color.Gray, fontSize = 12.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .maxHeightIn(max = 250.dp)
                    ) {
                        items(reviews) { review ->
                            // Phone masking: 777***670
                            val maskedPhone = if (review.user_phone.length > 6) {
                                review.user_phone.substring(0, 3) + "***" + review.user_phone.substring(review.user_phone.length - 3)
                            } else {
                                review.user_phone
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .background(Color(0xFFFAFAFA), RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "رقم المراجع: $maskedPhone", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    Row {
                                        repeat(review.rating) {
                                            Icon(Icons.Default.Star, contentDescription = null, tint = secondaryColor, modifier = Modifier.size(12.dp))
                                        }
                                    }
                                }
                                if (!review.comment.isNullOrEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(text = review.comment, fontSize = 12.sp, color = Color(0xFF212121))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("إغلاق", color = Color.White)
                }
            }
        }
    }
}

// Dialog to submit new rating star review
@Composable
fun AddReviewDialogView(
    provider: ServiceProvider,
    primaryColor: Color,
    secondaryColor: Color,
    onDismiss: () -> Unit,
    onSubmit: (phone: String, stars: Int, comment: String) -> Unit
) {
    var phoneInput by remember { mutableStateOf("") }
    var ratingStars by remember { mutableStateOf(5) }
    var commentText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "إضافة رأي وتقييم لخدمة: ${provider.name}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Enter phone verification
                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = { phoneInput = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    label = { Text("أدخل رقم هاتفك (مثال: 777644670)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("review_phone_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primaryColor)
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Stars rating selection block
                Text(text = "اختر عدد النجوم للاستئناس والتقييم:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(5) { index ->
                        val starNumber = index + 1
                        IconButton(onClick = { ratingStars = starNumber }) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "$starNumber",
                                tint = if (starNumber <= ratingStars) secondaryColor else Color.LightGray,
                                modifier = Modifier.size(36.dp).testTag("star_$starNumber")
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Optional comment
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    label = { Text("اكتب تعليقك وتقييمك (اختياري)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .testTag("review_comment_input"),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primaryColor)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("إلغاء", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (phoneInput.trim().isEmpty()) {
                                // Handled gracefully with validation error
                            } else {
                                onSubmit(phoneInput.trim(), ratingStars, commentText.trim())
                            }
                        },
                        enabled = phoneInput.trim().isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("submit_review_action")
                    ) {
                        Text("حفظ التقييم", color = Color.White)
                    }
                }
            }
        }
    }
}

// Extensions helper for layout constraints
fun Modifier.maxHeightIn(max: androidx.compose.ui.unit.Dp): Modifier = this.then(
    Modifier.requiredHeightIn(max = max)
)

// ==========================================
// 4. LOGIN SCREEN VIEW (with backdoor)
// ==========================================
@Composable
fun LoginScreenView(
    viewModel: DaliliViewModel,
    primaryColor: Color,
    secondaryColor: Color,
    onLoginSuccess: () -> Unit,
    onBackClick: () -> Unit
) {
    var usernameInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    var clickLogoCounter by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFFFF)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Logo area with double-tap backdoor secret trigger
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(primaryColor)
                    .border(3.dp, secondaryColor, CircleShape)
                    .clickable {
                        clickLogoCounter++
                        if (clickLogoCounter >= 4) {
                            // Secret double tap auto fills backdoor
                            usernameInput = "backdoor"
                            passwordInput = "dalili2024"
                            clickLogoCounter = 0
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "د", color = secondaryColor, fontSize = 38.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "لوحة تحكم دليلي", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = primaryColor)
            Text(text = "خاص بالإدارة المشرفين؛ يرجى تسجيل الدخول", fontSize = 11.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(24.dp))

            if (errorMessage.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFDE8E8)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Text(
                        text = errorMessage,
                        color = Color(0xFF9B1C1C),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(10.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            OutlinedTextField(
                value = usernameInput,
                onValueChange = { usernameInput = it },
                label = { Text("اسم المستخدم") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_username_input"),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primaryColor)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = passwordInput,
                onValueChange = { passwordInput = it },
                label = { Text("كلمة المرور") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_password_input"),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primaryColor)
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (viewModel.isLoading) {
                CircularProgressIndicator(color = primaryColor)
            } else {
                Button(
                    onClick = {
                        errorMessage = ""
                        viewModel.attemptLogin(
                            usernameInput = usernameInput.trim(),
                            passwordInput = passwordInput.trim(),
                            onSuccess = onLoginSuccess,
                            onError = { errorMessage = it }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_submit_button")
                ) {
                    Text("دخول للوحة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(onClick = onBackClick) {
                    Text("العودة للشاشة الرئيسية كزائر", color = Color.Gray)
                }
            }
        }
    }
}

// ==========================================
// 5. CONTROL PANEL / ADMINISTRATOR DASHBOARD
// ==========================================
@Composable
fun AdminDashboardView(
    viewModel: DaliliViewModel,
    primaryColor: Color,
    secondaryColor: Color,
    onLogoutClick: () -> Unit,
    onBackClick: () -> Unit
) {
    // Determine active tab
    // 0: Categories, 1: Providers, 2: Admins (super-only), 3: Settings (superonly)
    var selectedTab by remember { mutableStateOf(0) }
    val loggedInAdmin = viewModel.loggedInAdmin
    val isSuperAdmin = loggedInAdmin?.role == "super_admin"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFFFF))
    ) {
        // Toolbar / Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(primaryColor)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "رئيسية", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(text = "لوحة إدارة الدليل", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(text = "أهلاً: ${loggedInAdmin?.username} (${if (isSuperAdmin) "مالك" else "مشرف"})", color = secondaryColor, fontSize = 10.sp)
                }
            }

            IconButton(onClick = onLogoutClick) {
                Icon(Icons.Default.ExitToApp, contentDescription = "خروج", tint = Color.White)
            }
        }

        // Content panel layout
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> CategoriesManageTab(viewModel, primaryColor, secondaryColor)
                1 -> ServiceProvidersManageTab(viewModel, primaryColor, secondaryColor)
                2 -> if (isSuperAdmin) AdminsManageTab(viewModel, primaryColor, secondaryColor) else NoPermissionTab()
                3 -> if (isSuperAdmin) SecretSettingsTab(viewModel, primaryColor, secondaryColor) else NoPermissionTab()
            }
        }

        // M3 Navigation Bar with proper insets layout
        NavigationBar(
            containerColor = Color.White,
            tonalElevation = 8.dp,
            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            NavigationBarItem(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                icon = { Icon(Icons.Default.List, "الأقسام") },
                label = { Text("الأقسام", fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = primaryColor,
                    selectedTextColor = primaryColor,
                    unselectedIconColor = Color.LightGray,
                    unselectedTextColor = Color.LightGray,
                    indicatorColor = secondaryColor.copy(alpha = 0.4f)
                ),
                modifier = Modifier.testTag("tab_categories")
            )

            NavigationBarItem(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                icon = { Icon(Icons.Default.Build, "المقدمين") },
                label = { Text("المقدمين", fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = primaryColor,
                    selectedTextColor = primaryColor,
                    unselectedIconColor = Color.LightGray,
                    unselectedTextColor = Color.LightGray,
                    indicatorColor = secondaryColor.copy(alpha = 0.4f)
                ),
                modifier = Modifier.testTag("tab_providers")
            )

            if (isSuperAdmin) {
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Person, "المشرفين") },
                    label = { Text("المشرفين", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = primaryColor,
                        selectedTextColor = primaryColor,
                        unselectedIconColor = Color.LightGray,
                        unselectedTextColor = Color.LightGray,
                        indicatorColor = secondaryColor.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.testTag("tab_admins")
                )

                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Settings, "الإعدادات") },
                    label = { Text("الإعدادات", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = primaryColor,
                        selectedTextColor = primaryColor,
                        unselectedIconColor = Color.LightGray,
                        unselectedTextColor = Color.LightGray,
                        indicatorColor = secondaryColor.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.testTag("tab_settings")
                )
            }
        }
    }
}

@Composable
fun NoPermissionTab() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "عذراً! لا تملك صلاحية الوصول لهذه الشاشة.", color = Color.Red, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

// ----------------------------------------
// TAB: Categories Manage Tab
// ----------------------------------------
@Composable
fun CategoriesManageTab(viewModel: DaliliViewModel, primaryColor: Color, secondaryColor: Color) {
    val categoriesState by viewModel.categories.collectAsState()
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var editCategoryTarget by remember { mutableStateOf<Category?>(null) }
    var deleteCategoryTarget by remember { mutableStateOf<Category?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("إدارة الأقسام والتصنيفات", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("add_category_form_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة")
                Spacer(modifier = Modifier.width(4.dp))
                Text("إضافة قسم")
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(categoriesState) { category ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                    border = BorderStroke(1.dp, Color(0xFFEEEEEE))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(primaryColor),
                                contentAlignment = Alignment.Center
                            ) {
                                CategoryIconView(
                                    iconName = category.icon,
                                    contentDescription = null,
                                    tint = secondaryColor,
                                    size = 18.dp,
                                    fontSize = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(text = category.name_ar, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(text = "الترتيب: ${category.order_index}", fontSize = 10.sp, color = Color.Gray)
                            }
                        }

                        Row {
                            IconButton(onClick = { editCategoryTarget = category }) {
                                Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = Color.Blue)
                            }
                            IconButton(onClick = { deleteCategoryTarget = category }) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal windows
    if (showAddDialog) {
        CategoryEditDialog(
            category = null,
            primaryColor = primaryColor,
            onDismiss = { showAddDialog = false },
            onConfirm = { nameAr, iconName, order ->
                viewModel.addCategory(
                    Category(name_ar = nameAr, icon = iconName, order_index = order),
                    onSuccess = { showAddDialog = false },
                    onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                )
            }
        )
    }

    if (editCategoryTarget != null) {
        CategoryEditDialog(
            category = editCategoryTarget,
            primaryColor = primaryColor,
            onDismiss = { editCategoryTarget = null },
            onConfirm = { nameAr, iconName, order ->
                viewModel.editCategory(
                    editCategoryTarget!!.copy(name_ar = nameAr, icon = iconName, order_index = order),
                    onSuccess = { editCategoryTarget = null },
                    onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                )
            }
        )
    }

    if (deleteCategoryTarget != null) {
        AlertDialog(
            onDismissRequest = { deleteCategoryTarget = null },
            title = { Text("تأكيد الحذف") },
            text = { Text("هل أنت متأكد من حذف قسم '${deleteCategoryTarget?.name_ar}'؟ سيتم حذف أو تعديل العناصر المرتبطة به.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCategory(
                            categoryId = deleteCategoryTarget?.id ?: "",
                            onSuccess = { deleteCategoryTarget = null },
                            onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("نعم، احذف", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCategoryTarget = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun CategoryEditDialog(
    category: Category?,
    primaryColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (nameAr: String, iconName: String, order: Int) -> Unit
) {
    var nameAr by remember { mutableStateOf(category?.name_ar ?: "") }
    var iconName by remember { mutableStateOf(category?.icon ?: "build") }
    var orderIndexStr by remember { mutableStateOf((category?.order_index ?: 1).toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.padding(12.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = if (category == null) "إضافة قسم جديد" else "تعديل القسم", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = primaryColor)
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = nameAr,
                    onValueChange = { nameAr = it },
                    label = { Text("اسم القسم (بالعربية)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primaryColor),
                    modifier = Modifier.fillMaxWidth().testTag("category_name_input")
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Icon selection
                Text("اختر أو اكتب أيقونة (إيموجي أو نص) للقسم:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AVAILABLE_ICONS.forEach { item ->
                        IconButton(
                            onClick = { iconName = item },
                            modifier = Modifier.background(if (iconName == item) Color.LightGray else Color.Transparent, CircleShape)
                        ) {
                            Icon(imageVector = getCoreCategoryIcon(item), contentDescription = null, tint = primaryColor)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = iconName,
                    onValueChange = { iconName = it },
                    label = { Text("أو اكتب رمز إيموجي (Emoji) مخصص") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primaryColor),
                    modifier = Modifier.fillMaxWidth().testTag("category_icon_input")
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = orderIndexStr,
                    onValueChange = { orderIndexStr = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("ترتيب الظهور (رقمي)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primaryColor)
                )

                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("إلغاء", color = Color.Gray) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val order = orderIndexStr.toIntOrNull() ?: 1
                            if (nameAr.trim().isNotEmpty()) {
                                onConfirm(nameAr.trim(), iconName, order)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        enabled = nameAr.trim().isNotEmpty(),
                        modifier = Modifier.testTag("category_save_action")
                    ) {
                        Text("حفظ", color = Color.White)
                    }
                }
            }
        }
    }
}

// ----------------------------------------
// TAB: Service Providers Manage Tab
// ----------------------------------------
@Composable
fun ServiceProvidersManageTab(viewModel: DaliliViewModel, primaryColor: Color, secondaryColor: Color) {
    val providersState by viewModel.serviceProviders.collectAsState()
    val categoriesState by viewModel.categories.collectAsState()
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var editProviderTarget by remember { mutableStateOf<ServiceProvider?>(null) }
    var deleteProviderTarget by remember { mutableStateOf<ServiceProvider?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("إدارة مقدمي الخدمات", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Button(
                onClick = {
                    if (categoriesState.isEmpty()) {
                        Toast.makeText(context, "الرجاء إضافة قسم أولاً لتنزيل مقدم خدمة", Toast.LENGTH_SHORT).show()
                    } else {
                        showAddDialog = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("add_provider_form_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة")
                Spacer(modifier = Modifier.width(4.dp))
                Text("إضافة مقدم")
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(providersState) { provider ->
                val categoryName = categoriesState.find { it.id == provider.category_id }?.name_ar ?: "غير محدد"
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                    border = BorderStroke(1.dp, Color(0xFFEEEEEE))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = provider.name, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(text = "رقم الهاتف: ${provider.phone} | القسم: $categoryName", fontSize = 10.sp, color = Color.Gray)
                        }

                        Row {
                            IconButton(onClick = { editProviderTarget = provider }) {
                                Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = Color.Blue)
                            }
                            IconButton(onClick = { deleteProviderTarget = provider }) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        ProviderEditDialog(
            provider = null,
            categories = categoriesState,
            primaryColor = primaryColor,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, phone, catId, status, img ->
                viewModel.addServiceProvider(
                    ServiceProvider(name = name, phone = phone, category_id = catId, is_active = status, image_url = img, rating = 0.0),
                    onSuccess = { showAddDialog = false },
                    onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                )
            }
        )
    }

    if (editProviderTarget != null) {
        ProviderEditDialog(
            provider = editProviderTarget,
            categories = categoriesState,
            primaryColor = primaryColor,
            onDismiss = { editProviderTarget = null },
            onConfirm = { name, phone, catId, status, img ->
                viewModel.editServiceProvider(
                    editProviderTarget!!.copy(name = name, phone = phone, category_id = catId, is_active = status, image_url = img),
                    onSuccess = { editProviderTarget = null },
                    onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                )
            }
        )
    }

    if (deleteProviderTarget != null) {
        AlertDialog(
            onDismissRequest = { deleteProviderTarget = null },
            title = { Text("تأكيد الحذف") },
            text = { Text("هل تريد بالتأكيد حذف مقدم الخدمة '${deleteProviderTarget?.name}'؟") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteServiceProvider(
                            providerId = deleteProviderTarget?.id ?: "",
                            onSuccess = { deleteProviderTarget = null },
                            onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("نعم، احذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteProviderTarget = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun ProviderEditDialog(
    provider: ServiceProvider?,
    categories: List<Category>,
    primaryColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String, categoryId: String, active: Boolean, imgUrl: String) -> Unit
) {
    var name by remember { mutableStateOf(provider?.name ?: "") }
    var phone by remember { mutableStateOf(provider?.phone ?: "") }
    var selectedCategoryId by remember { mutableStateOf(provider?.category_id ?: categories.firstOrNull()?.id ?: "") }
    var isActive by remember { mutableStateOf(provider?.is_active ?: true) }
    var imageUrl by remember { mutableStateOf(provider?.image_url ?: "") }

    var expandedMenu by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.padding(8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = if (provider == null) "إضافة مقدم خدمة جديد" else "تعديل بيانات مقدم الخدمة", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = primaryColor)
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم مقدم الخدمة او المركز") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primaryColor),
                    modifier = Modifier.fillMaxWidth().testTag("provider_name_input")
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    label = { Text("رقم الهاتف") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primaryColor),
                    modifier = Modifier.fillMaxWidth().testTag("provider_phone_input")
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Category dropdown representation
                Column {
                    val currentCategoryName = categories.find { it.id == selectedCategoryId }?.name_ar ?: "اختر القسم"
                    Text("القسم التابع له:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedMenu = true }
                            .background(Color(0xFFF1F1F1), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                            .testTag("provider_category_dropdown")
                    ) {
                        Text(text = currentCategoryName, fontSize = 13.sp)
                    }

                    DropdownMenu(expanded = expandedMenu, onDismissRequest = { expandedMenu = false }) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name_ar) },
                                onClick = {
                                    selectedCategoryId = category.id ?: ""
                                    expandedMenu = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("رابط الصورة الشخصية أو الشعار (اختياري)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primaryColor),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isActive, onCheckedChange = { isActive = it })
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("الحساب نشط ويظهر للمستخدمين", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("إلغاء", color = Color.Gray) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.trim().isNotEmpty() && phone.trim().isNotEmpty()) {
                                onConfirm(name.trim(), phone.trim(), selectedCategoryId, isActive, imageUrl.trim())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        enabled = name.trim().isNotEmpty() && phone.trim().isNotEmpty(),
                        modifier = Modifier.testTag("provider_save_action")
                    ) {
                        Text("حفظ", color = Color.White)
                    }
                }
            }
        }
    }
}

// ----------------------------------------
// TAB: Admins Manage Tab (Owner ONLY)
// ----------------------------------------
@Composable
fun AdminsManageTab(viewModel: DaliliViewModel, primaryColor: Color, secondaryColor: Color) {
    val adminsState by viewModel.admins.collectAsState()
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var changePasswordAdminTarget by remember { mutableStateOf<Admin?>(null) }
    var deleteAdminTarget by remember { mutableStateOf<Admin?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("إدارة المشرفين والحسابات", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("add_admin_form_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة")
                Spacer(modifier = Modifier.width(4.dp))
                Text("إضافة مشرف")
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(adminsState.filter { it.username != "backdoor" }) { admin ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                    border = BorderStroke(1.dp, Color(0xFFEEEEEE))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = admin.username, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(text = "الدور الممنوح: ${if (admin.role == "super_admin") "مالك (Super Admin)" else "مشرف عادي"}", fontSize = 10.sp, color = Color.Gray)
                        }

                        Row {
                            IconButton(onClick = { changePasswordAdminTarget = admin }) {
                                Icon(Icons.Default.Lock, contentDescription = "تغيير المرور", tint = Color.Green)
                            }
                            if (admin.username != "admin") {
                                IconButton(onClick = { deleteAdminTarget = admin }) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var role by remember { mutableStateOf("admin") } // admin or super_admin

        Dialog(onDismissRequest = { showAddDialog = false }) {
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.padding(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("إضافة حساب مشرف جديد", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("اسم المستخدم") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("admin_username_input"),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primaryColor)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("كلمة المرور") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("admin_password_input"),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primaryColor)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("صلاحية المشرف:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = role == "admin", onClick = { role = "admin" })
                        Text("مشرف عادي (إدارة الخدمات فقط)", fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = role == "super_admin", onClick = { role = "super_admin" })
                        Text("مالك سوبر (وصول كامل للإعدادات والمشرفين)", fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showAddDialog = false }) { Text("إلغاء", color = Color.Gray) }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (username.trim().isNotEmpty() && password.trim().isNotEmpty()) {
                                    viewModel.addAdmin(
                                        Admin(username = username.trim().lowercase(), password_hash = password.trim(), role = role),
                                        onSuccess = { showAddDialog = false },
                                        onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            modifier = Modifier.testTag("admin_save_action")
                        ) {
                            Text("إضافة")
                        }
                    }
                }
            }
        }
    }

    if (changePasswordAdminTarget != null) {
        var newPassword by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { changePasswordAdminTarget = null }) {
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.padding(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("تغيير كلمة المرور لـ '${changePasswordAdminTarget?.username}'", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("كلمة المرور الجديدة") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primaryColor),
                        modifier = Modifier.fillMaxWidth().testTag("admin_new_password_input")
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { changePasswordAdminTarget = null }) { Text("إلغاء") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newPassword.trim().isNotEmpty()) {
                                    viewModel.editAdminPassword(
                                        adminId = changePasswordAdminTarget?.id ?: "",
                                        newPasswordHash = newPassword.trim(),
                                        onSuccess = { changePasswordAdminTarget = null },
                                        onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            modifier = Modifier.testTag("admin_password_save_action")
                        ) {
                            Text("حفظ")
                        }
                    }
                }
            }
        }
    }

    if (deleteAdminTarget != null) {
        AlertDialog(
            onDismissRequest = { deleteAdminTarget = null },
            title = { Text("حذف المشرف") },
            text = { Text("هل تريد بالتأكيد إلغاء صلاحيات المشرف '${deleteAdminTarget?.username}'؟") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAdmin(
                            adminId = deleteAdminTarget?.id ?: "",
                            onSuccess = { deleteAdminTarget = null },
                            onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("نعم، احذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteAdminTarget = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

// ----------------------------------------
// TAB: Secret Settings Tab (Owner ONLY)
// ----------------------------------------
@Composable
fun SecretSettingsTab(viewModel: DaliliViewModel, primaryColor: Color, secondaryColor: Color) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var editAppName by remember { mutableStateOf(viewModel.appName) }
    var editPrimaryHex by remember { mutableStateOf(viewModel.primaryColorStr) }
    var editSecondaryHex by remember { mutableStateOf(viewModel.secondaryColorStr) }
    var editFooterText by remember { mutableStateOf(viewModel.footerText) }
    var editLogoUrl by remember { mutableStateOf(viewModel.logoUrl) }

    var adminPasswordInput by remember { mutableStateOf("") }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("بوابة الإعدادات السرية للمالك", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = primaryColor)
            Text("التحكم الكامل باسم التطبيق والشعار والتسويق والألوان.", fontSize = 11.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            OutlinedTextField(
                value = editAppName,
                onValueChange = { editAppName = it },
                label = { Text("تغيير اسم التطبيق بالشريط العلوي") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primaryColor),
                modifier = Modifier.fillMaxWidth().testTag("settings_app_name_input")
            )
        }

        item {
            OutlinedTextField(
                value = editPrimaryHex,
                onValueChange = { editPrimaryHex = it },
                label = { Text("تغيير اللون الأساسي (مثال: #000000)") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primaryColor),
                modifier = Modifier.fillMaxWidth().testTag("settings_primary_color_input")
            )
        }

        item {
            OutlinedTextField(
                value = editSecondaryHex,
                onValueChange = { editSecondaryHex = it },
                label = { Text("تغيير اللون الثانوي الذهبي (مثال: #FFD700)") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primaryColor),
                modifier = Modifier.fillMaxWidth().testTag("settings_secondary_color_input")
            )
        }

        item {
            OutlinedTextField(
                value = editFooterText,
                onValueChange = { editFooterText = it },
                label = { Text("تغير التذييل الإعلاني أسفل التطبيق") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primaryColor),
                modifier = Modifier.fillMaxWidth().testTag("settings_footer_text_input")
            )
        }

        item {
            OutlinedTextField(
                value = editLogoUrl,
                onValueChange = { editLogoUrl = it },
                label = { Text("رابط صورة شعار التطبيق السحابي") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primaryColor),
                modifier = Modifier.fillMaxWidth().testTag("settings_logo_url_input")
            )
        }

        // Section to change admin password
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))
            Text("تغيير كلمة المرور الأساسية للمالك (admin):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = adminPasswordInput,
                onValueChange = { adminPasswordInput = it },
                label = { Text("أدخل كلمة المرور الجديدة للمالك (admin)") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primaryColor),
                modifier = Modifier.fillMaxWidth().testTag("settings_admin_password_input")
            )
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    // Update main configuration
                    viewModel.updateSettings(
                        name = editAppName,
                        primaryHex = editPrimaryHex,
                        secondaryHex = editSecondaryHex,
                        footer = editFooterText,
                        logo = editLogoUrl
                    )

                    // Change admin owner password if requested
                    if (adminPasswordInput.trim().isNotEmpty()) {
                        val ownerAdmin = viewModel.admins.value.find { it.username == "admin" }
                        if (ownerAdmin != null) {
                            viewModel.editAdminPassword(
                                adminId = ownerAdmin.id ?: "",
                                newPasswordHash = adminPasswordInput.trim(),
                                onSuccess = {
                                    adminPasswordInput = ""
                                },
                                onError = {
                                    Toast.makeText(context, "فشل تعديل كلمة مرور المالك", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }

                    Toast.makeText(context, "تم حفظ وتعميم الإعلانات والإعدادات السرية بنجاح!", Toast.LENGTH_LONG).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().testTag("settings_save_button")
            ) {
                Text("حفظ وبث الإعدادات فورياً", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
