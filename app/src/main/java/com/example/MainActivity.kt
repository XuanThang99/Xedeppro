package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CarRental
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Customer
import com.example.data.model.ServiceDefinition
import com.example.data.model.ServiceRegistry
import com.example.data.model.ServiceUsage
import com.example.data.model.SubscriptionPackage
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}

enum class AppTab {
    DASHBOARD, CUSTOMERS, REVENUE, PRICING
}

@Composable
fun MainScreen(viewModel: AppViewModel) {
    var currentTab by remember { mutableStateOf(AppTab.DASHBOARD) }
    val context = LocalContext.current
    val toastMsg by viewModel.statusMessage.collectAsState()

    // Trigger platform sync upon first launch to populate standard mock data from xedep.pro
    LaunchedEffect(Unit) {
        viewModel.syncWithWebsite()
    }

    LaunchedEffect(toastMsg) {
        toastMsg?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.testTag("bottom_nav")
            ) {
                val items = listOf(
                    Triple(AppTab.DASHBOARD, Icons.Default.Dashboard, "Bảng điều khiển"),
                    Triple(AppTab.CUSTOMERS, Icons.Default.People, "Khách hàng"),
                    Triple(AppTab.REVENUE, Icons.Default.Assessment, "Doanh thu"),
                    Triple(AppTab.PRICING, Icons.Default.ListAlt, "Bảng giá")
                )
                items.forEach { (tab, icon, label) ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            indicatorColor = MaterialTheme.colorScheme.secondary
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppTab.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                AppTab.CUSTOMERS -> CustomersScreen(viewModel = viewModel)
                AppTab.REVENUE -> RevenueScreen(viewModel = viewModel)
                AppTab.PRICING -> PricingScreen()
            }
        }
    }
}

@Composable
fun DashboardScreen(viewModel: AppViewModel) {
    val stats by viewModel.dashboardStats.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val reports by viewModel.monthlyRevenueReports.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Hero Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.secondary)
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "XE ĐẸP PRO",
                        color = MaterialTheme.colorScheme.onSecondary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "Chăm Sóc Tiêu Chuẩn & Premium",
                        color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Sync button
                IconButton(
                    onClick = { viewModel.syncWithWebsite() },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .testTag("sync_button")
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sync",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Stats Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Khách hàng",
                value = stats.totalCustomers.toString(),
                icon = Icons.Default.People,
                color = Color(0xFF00E5FF),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Gói hoạt động",
                value = stats.totalActiveSubscriptions.toString(),
                icon = Icons.Default.DirectionsCar,
                color = Color(0xFFFFB300),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        StatCard(
            title = "Tổng doanh thu hệ thống",
            value = formatCurrency(stats.totalRevenue),
            icon = Icons.Default.TrendingUp,
            color = Color(0xFF4CAF50),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Subscription Breakdown Section
        Text(
            text = "Phân Bố Gói Năm Premium",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PackageDistributionCard(name = "SILVER (15M)", count = stats.silverCount, color = Color(0xFF90A4AE), modifier = Modifier.weight(1f))
            PackageDistributionCard(name = "GOLD (17M)", count = stats.goldCount, color = Color(0xFFFFD54F), modifier = Modifier.weight(1f))
            PackageDistributionCard(name = "DIAMOND (20M)", count = stats.diamondCount, color = Color(0xFF4FC3F7), modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Simple Custom Compose Chart
        if (reports.isNotEmpty()) {
            Text(
                text = "Biểu Đồ Doanh Thu Theo Tháng",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            RevenueMiniChart(reports = reports.take(4).reversed())
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Expiry Alerts Section (Cảnh báo hết hạn)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Cảnh Báo Hết Hạn Gói Năm (< 30 Ngày)",
                color = MaterialTheme.colorScheme.error,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${stats.activeExpiringSoon.size} xe",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (stats.activeExpiringSoon.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "OK",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Không có gói dịch vụ nào sắp hết hạn.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                stats.activeExpiringSoon.forEach { info ->
                    ExpiringCarItem(info = info, viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun PackageDistributionCard(
    name: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = name,
                color = if (color == Color(0xFF90A4AE)) Color(0xFF49454F) else color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$count xe",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
fun RevenueMiniChart(reports: List<AppViewModel.MonthlyRevenueReport>) {
    val maxVal = remember(reports) {
        val max = reports.maxOfOrNull { it.totalRevenue } ?: 1.0
        if (max == 0.0) 1.0 else max
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Bars Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                val barCount = reports.size
                if (barCount == 0) return@Canvas

                val canvasWidth = size.width
                val canvasHeight = size.height
                val barSpacing = 24f
                val totalSpacing = barSpacing * (barCount + 1)
                val barWidth = (canvasWidth - totalSpacing) / barCount

                // Get theme primary color
                val primaryColor = Color(0xFF6750A4)

                reports.forEachIndexed { index, report ->
                    val x = barSpacing + index * (barWidth + barSpacing)
                    val barHeightFraction = (report.totalRevenue / maxVal).toFloat()
                    val barHeight = canvasHeight * barHeightFraction * 0.85f // leave room for text
                    val y = canvasHeight - barHeight

                    // Draw rounded/smooth gradient bar in primary purple
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(primaryColor, primaryColor.copy(alpha = 0.3f))
                        ),
                        topLeft = Offset(x, y),
                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
                    )
                }

                // Draw baseline
                drawLine(
                    color = Color(0xFFCAC4D0),
                    start = Offset(0f, canvasHeight),
                    end = Offset(canvasWidth, canvasHeight),
                    strokeWidth = 2f
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // X-Axis Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                reports.forEach { report ->
                    val shortMonth = report.monthKey // e.g. "07/2026"
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = shortMonth,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formatShortCurrency(report.totalRevenue),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExpiringCarItem(
    info: AppViewModel.ExpiringPackageInfo,
    viewModel: AppViewModel
) {
    var showNotifyDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular icon like in the HTML (bg-[#FFD8E4] and text-[#31111D])
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFD8E4)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = Color(0xFF31111D),
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = info.customer.name,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.secondary)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = info.subscription.packageType,
                                color = MaterialTheme.colorScheme.onSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Biển số: ${info.customer.licensePlate} | ĐT: ${info.customer.phone}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Còn lại ${info.daysRemaining} ngày (Hết hạn: ${formatDate(info.subscription.endDate)})",
                        color = if (info.daysRemaining <= 7) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Beautiful modern button bg-[#6750A4]/10 text-[#6750A4]
            IconButton(
                onClick = { showNotifyDialog = true },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .testTag("notify_alert_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notify",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    if (showNotifyDialog) {
        NotificationDraftDialog(
            customer = info.customer,
            sub = info.subscription,
            viewModel = viewModel,
            onDismiss = { showNotifyDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(viewModel: AppViewModel) {
    val customerList by viewModel.customers.collectAsState()
    val subList by viewModel.subscriptions.collectAsState()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedCustomerForDetails by remember { mutableStateOf<Customer?>(null) }

    val filteredCustomers = remember(customerList, searchQuery) {
        if (searchQuery.isBlank()) {
            customerList
        } else {
            customerList.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.phone.contains(searchQuery) ||
                it.licensePlate.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search & Head
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Tìm tên, SĐT, biển số...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Tìm", tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search_customer_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            if (filteredCustomers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = "Empty",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Chưa có khách hàng nào trong hệ thống.\nĐồng bộ hoặc bấm '+' để thêm mới.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredCustomers) { cust ->
                        val sub = subList.find { it.customerId == cust.id && it.isActive && it.endDate > System.currentTimeMillis() }
                        CustomerListItem(customer = cust, activeSub = sub, onClick = {
                            selectedCustomerForDetails = cust
                        })
                    }
                }
            }
        }

        // Add Customer FAB
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_customer_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Thêm khách hàng")
        }
    }

    if (showAddDialog) {
        AddCustomerDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, phone, plate, carType ->
                viewModel.insertCustomer(name, phone, plate, carType)
                showAddDialog = false
            }
        )
    }

    selectedCustomerForDetails?.let { customer ->
        val activeSub = subList.find { it.customerId == customer.id && it.isActive && it.endDate > System.currentTimeMillis() }
        CustomerDetailDialog(
            customer = customer,
            activeSub = activeSub,
            viewModel = viewModel,
            onDismiss = { selectedCustomerForDetails = null }
        )
    }
}

@Composable
fun CustomerListItem(
    customer: Customer,
    activeSub: SubscriptionPackage?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = customer.name,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (customer.syncSource == "xedep.pro") {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Web Sync",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Biển số: ${customer.licensePlate} • SĐT: ${customer.phone}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Loại xe: ${customer.carType}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    fontSize = 11.sp
                )
            }

            // Subscription Tag
            if (activeSub != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when (activeSub.packageType) {
                                "SILVER" -> Color(0xFF90A4AE).copy(alpha = 0.15f)
                                "GOLD" -> Color(0xFFFFD54F).copy(alpha = 0.15f)
                                "DIAMOND" -> Color(0xFF4FC3F7).copy(alpha = 0.15f)
                                else -> Color.Transparent
                            }
                        )
                        .border(
                            width = 1.dp,
                            color = when (activeSub.packageType) {
                                "SILVER" -> Color(0xFF78909C)
                                "GOLD" -> Color(0xFFFFB300)
                                "DIAMOND" -> Color(0xFF0288D1)
                                else -> Color.Transparent
                            },
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = activeSub.packageType,
                        color = when (activeSub.packageType) {
                            "SILVER" -> Color(0xFF455A64)
                            "GOLD" -> Color(0xFFB47A00)
                            "DIAMOND" -> Color(0xFF01579B)
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Text(
                    text = "Khách Lẻ",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomerDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var plate by remember { mutableStateOf("") }
    var carType by remember { mutableStateOf("SEDAN") } // MINI, SEDAN, SUV
    var expanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF161622),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFFFB300).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Thêm Khách Hàng Mới",
                    color = Color(0xFFFFB300),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Họ và Tên") },
                    modifier = Modifier.fillMaxWidth().testTag("cust_name_field"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFFB300),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Số điện thoại") },
                    modifier = Modifier.fillMaxWidth().testTag("cust_phone_field"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFFB300),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = plate,
                    onValueChange = { plate = it },
                    label = { Text("Biển số xe (e.g. 30A-123.45)") },
                    modifier = Modifier.fillMaxWidth().testTag("cust_plate_field"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFFB300),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Car Type Dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = carType,
                        onValueChange = {},
                        label = { Text("Phân khúc xe (Size)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFFB300),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(Color(0xFF161622))
                    ) {
                        listOf("MINI", "SEDAN", "SUV").forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type, color = Color.White) },
                                onClick = {
                                    carType = type
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.6f))
                    ) {
                        Text("Hủy")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank() && phone.isNotBlank() && plate.isNotBlank()) {
                                onConfirm(name, phone, plate, carType)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                        modifier = Modifier.testTag("dialog_confirm_button")
                    ) {
                        Text("Thêm mới", color = Color(0xFF0C0C10))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailDialog(
    customer: Customer,
    activeSub: SubscriptionPackage?,
    viewModel: AppViewModel,
    onDismiss: () -> Unit
) {
    var showRegisterSub by remember { mutableStateOf(false) }
    var showLogService by remember { mutableStateOf(false) }
    var showNotifyPicker by remember { mutableStateOf(false) }
    val usagesList by viewModel.getCustomerServiceUsages(customer.id).collectAsState(initial = emptyList())

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF14141E),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .border(1.dp, Color(0xFFFFB300).copy(alpha = 0.2f), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = customer.name,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Biển số: ${customer.licensePlate} | Loại: ${customer.carType}",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Active Subscription Status
                if (activeSub != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1D1D2C))
                            .border(1.dp, Color(0xFFFFB300).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Gói Năm Premium: ${activeSub.packageType}",
                                    color = Color(0xFFFFB300),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "Đang hoạt động",
                                    color = Color(0xFF4CAF50),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Thời hạn: ${formatDate(activeSub.startDate)} - ${formatDate(activeSub.endDate)}",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Lượt Dịch Vụ Miễn Phí Còn Lại:",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            RemainingServicesGrid(activeSub = activeSub)
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1D1D2C))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Khách hàng chưa đăng ký gói chăm sóc năm",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { showRegisterSub = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("reg_package_button")
                            ) {
                                Text("Đăng Ký Gói Chăm Sóc Năm", color = Color(0xFF0C0C10), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action controls row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showLogService = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5200)),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("log_service_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.DirectionsCar, contentDescription = "Log", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Lên Dịch Vụ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { showNotifyPicker = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("share_alert_button"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFB300))
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = "Remind", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Gửi Nhắc Nhở", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Service usage history
                Text(
                    text = "Lịch Sử Sử Dụng Dịch Vụ",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                if (usagesList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Chưa có lượt dọn rửa nào được ghi lại.",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 12.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(usagesList) { usage ->
                            UsageListItem(usage = usage)
                        }
                    }
                }
            }
        }
    }

    if (showRegisterSub) {
        RegisterSubscriptionDialog(
            customer = customer,
            onDismiss = { showRegisterSub = false },
            onConfirm = { packageType ->
                viewModel.registerPackageForCustomer(customer.id, packageType)
                showRegisterSub = false
            }
        )
    }

    if (showLogService) {
        LogServiceDialog(
            customer = customer,
            onDismiss = { showLogService = false },
            onConfirm = { srvName, notes ->
                viewModel.logServiceUse(customer.id, srvName, customer.carType, notes)
                showLogService = false
            }
        )
    }

    if (showNotifyPicker) {
        NotificationOptionsDialog(
            customer = customer,
            sub = activeSub,
            viewModel = viewModel,
            onDismiss = { showNotifyPicker = false }
        )
    }
}

@Composable
fun RemainingServicesGrid(activeSub: SubscriptionPackage) {
    val items = listOf(
        Triple("Rửa nâng cao / Wax", activeSub.remainingWash, Color(0xFFFFB300)),
        Triple("Vệ sinh nội thất VIP", activeSub.remainingVipInterior, Color(0xFF00E5FF)),
        Triple("Đánh bóng / Khoang máy", activeSub.remainingEnginePolish, Color(0xFFFF5200)),
        Triple("Tẩy ố kính & Khử mùi", activeSub.remainingGlassDeodorize, Color(0xFF4CAF50)),
    )

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowItems.forEach { (label, count, color) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF14141E))
                            .border(0.5.dp, color.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 10.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "$count",
                                color = color,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }

        if (activeSub.packageType == "DIAMOND") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF14141E))
                    .border(0.5.dp, Color(0xFF4FC3F7).copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Phủ Ceramic 9H miễn phí",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                    Text(
                        text = "${activeSub.remainingCeramic}",
                        color = Color(0xFF4FC3F7),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
fun UsageListItem(usage: ServiceUsage) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1D1D2C)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = usage.serviceName,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (usage.isPackageDeducted) "Miễn phí (Gói)" else formatCurrency(usage.cost),
                    color = if (usage.isPackageDeducted) Color(0xFF4CAF50) else Color(0xFFFFB300),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Xe: ${usage.vehicleSize}",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
                Text(
                    text = formatDate(usage.date),
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
            }
            if (usage.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Ghi chú: ${usage.notes}",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterSubscriptionDialog(
    customer: Customer,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var packageType by remember { mutableStateOf("SILVER") }
    var expanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF161622),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFFFB300).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Đăng Ký Gói Chăm Sóc Năm",
                    color = Color(0xFFFFB300),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Xe đăng ký: ${customer.name} - ${customer.licensePlate} (${customer.carType})",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = packageType,
                        onValueChange = {},
                        label = { Text("Chọn gói dịch vụ") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFFB300),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(Color(0xFF161622))
                    ) {
                        listOf("SILVER", "GOLD", "DIAMOND").forEach { type ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "$type - ${when(type){"SILVER" -> "15 TR"; "GOLD" -> "17 TR"; else -> "20 TR"}}/năm",
                                        color = Color.White
                                    )
                                },
                                onClick = {
                                    packageType = type
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Detail card of selected package
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1D1D2C))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "Thông tin đặc quyền Gói $packageType:",
                            color = Color(0xFFFFB300),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when(packageType) {
                                "SILVER" -> "• 20 lần Rửa nâng cao\n• 3 lần Vệ sinh nội thất VIP\n• 3 lần Đánh bóng 3 bước & VS khoang máy\n• 3 lần Tẩy ố kính & Khử mùi\n• Giảm 10% dịch vụ khác, ưu tiên lịch"
                                "GOLD" -> "• 30 lần Rửa nâng cao\n• 6 lần Vệ sinh nội thất VIP\n• 6 lần Đánh bóng 3 bước & VS khoang máy\n• 6 lần Tẩy ố kính & Khử mùi\n• Giảm 15% dịch vụ khác, ưu tiên lịch"
                                "DIAMOND" -> "• 40 lần Rửa nâng cao + Wax\n• 12 lần Vệ sinh nội thất VIP\n• 6 lần Vệ sinh khoang máy\n• 12 lần Tẩy ố kính & Khử mùi\n• 1 lần Đánh bóng Phủ Ceramic\n• Giảm 15% dịch vụ khác, ưu tiên lịch"
                                else -> ""
                            },
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Hủy", color = Color.White.copy(alpha = 0.6f))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = { onConfirm(packageType) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300))
                    ) {
                        Text("Kích hoạt ngay", color = Color(0xFF0C0C10))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogServiceDialog(
    customer: Customer,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var selectedService by remember { mutableStateOf(ServiceRegistry.services[0].name) }
    var notes by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF161622),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFFFB300).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Ghi Nhận Lượt Dịch Vụ Mới",
                    color = Color(0xFFFFB300),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Khách hàng: ${customer.name} (${customer.carType})",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Service Dropdown Selector
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = selectedService,
                        onValueChange = {},
                        label = { Text("Chọn dịch vụ chăm sóc") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFFB300),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier
                            .background(Color(0xFF161622))
                            .fillMaxHeight(0.4f) // Prevent overflowing
                    ) {
                        ServiceRegistry.services.forEach { service ->
                            val normalPrice = when(customer.carType.uppercase()) {
                                "MINI" -> service.priceMini
                                "SEDAN" -> service.priceSedan
                                else -> service.priceSuv
                            }
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "${service.name} (${formatCurrency(normalPrice)})",
                                        color = Color.White
                                    )
                                },
                                onClick = {
                                    selectedService = service.name
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Ghi chú thêm (Nếu có)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFFB300),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Hủy", color = Color.White.copy(alpha = 0.6f))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = { onConfirm(selectedService, notes) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5200))
                    ) {
                        Text("Ghi nhận & Xử lý", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationOptionsDialog(
    customer: Customer,
    sub: SubscriptionPackage?,
    viewModel: AppViewModel,
    onDismiss: () -> Unit
) {
    var selectedNotifyType by remember { mutableStateOf<String?>(null) }
    var serviceNameForNotification by remember { mutableStateOf(ServiceRegistry.services[0].name) }
    var serviceExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF161622),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFFFB300).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Chọn Loại Thông Báo Cần Gửi",
                    color = Color(0xFFFFB300),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (sub != null) {
                        NotificationOptionItem(
                            title = "Cảnh báo gói sắp hết hạn",
                            desc = "Nhắc nhở xe ${customer.licensePlate} sắp hết hạn gói năm, liệt kê số lượt còn lại.",
                            onClick = { selectedNotifyType = "EXPIRY" }
                        )

                        NotificationOptionItem(
                            title = "Tổng hợp số dư lượt dọn rửa định kỳ",
                            desc = "Gửi danh sách số lần rửa xe, dọn nội thất VIP còn lại trong gói năm.",
                            onClick = { selectedNotifyType = "SUMMARY" }
                        )
                    }

                    NotificationOptionItem(
                        title = "Cảm ơn sau khi rửa dọn xong",
                        desc = "Gửi lời chúc thượng lộ bình an sau khi bàn giao xe bóng đẹp.",
                        onClick = { selectedNotifyType = "THANKYOU" }
                    )

                    NotificationOptionItem(
                        title = "Thông báo hoàn tất dịch vụ",
                        desc = "Báo xe đã rửa sạch, đánh bóng chỉn chu, sẵn sàng bàn giao.",
                        onClick = { selectedNotifyType = "SERVICE_DONE" }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Quay lại", color = Color.White.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }

    selectedNotifyType?.let { type ->
        NotificationDraftDialog(
            customer = customer,
            sub = sub,
            type = type,
            selectedServiceName = serviceNameForNotification,
            viewModel = viewModel,
            onDismiss = {
                selectedNotifyType = null
                onDismiss()
            }
        )
    }
}

@Composable
fun NotificationOptionItem(
    title: String,
    desc: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1D1D2C))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column {
            Text(text = title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = desc, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, lineHeight = 15.sp)
        }
    }
}

@Composable
fun NotificationDraftDialog(
    customer: Customer,
    sub: SubscriptionPackage?,
    type: String = "EXPIRY",
    selectedServiceName: String = "",
    viewModel: AppViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val messageText = remember {
        viewModel.generateNotificationText(customer, sub, type, selectedServiceName)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF161622),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFFFB300).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Mẫu Tin Nhắn Gửi Khách Hàng",
                    color = Color(0xFFFFB300),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Message Text Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0C0C12))
                        .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = messageText,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.testTag("notification_text_preview")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Hỗ trợ chia sẻ hoặc sao chép nhanh sang ứng dụng khác (Zalo, SMS).",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Đóng", color = Color.White.copy(alpha = 0.6f))
                    }

                    // Copy button
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Xe Dep Pro Notification", messageText)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Đã sao chép tin nhắn vào khay nhớ tạm!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF222230))
                            .size(40.dp)
                            .testTag("copy_notification_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color(0xFFFFB300))
                    }

                    // Share button
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                setType("text/plain")
                                putExtra(Intent.EXTRA_TEXT, messageText)
                            }
                            context.startActivity(Intent.createChooser(intent, "Gửi thông báo qua:"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("share_intent_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Color(0xFF0C0C10), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Gửi Đi", color = Color(0xFF0C0C10), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevenueScreen(viewModel: AppViewModel) {
    val reports by viewModel.monthlyRevenueReports.collectAsState()
    val context = LocalContext.current

    var selectedMonthKey by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val activeReport = remember(reports, selectedMonthKey) {
        if (selectedMonthKey.isEmpty() && reports.isNotEmpty()) {
            reports[0]
        } else {
            reports.find { it.monthKey == selectedMonthKey }
        }
    }

    // Auto-select latest month key if none selected
    LaunchedEffect(reports) {
        if (selectedMonthKey.isEmpty() && reports.isNotEmpty()) {
            selectedMonthKey = reports[0].monthKey
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Dropdown Month Selector
        if (reports.isNotEmpty()) {
            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = !dropdownExpanded }
            ) {
                OutlinedTextField(
                    readOnly = true,
                    value = activeReport?.displayMonth ?: "",
                    onValueChange = {},
                    label = { Text("Chọn Tháng Báo Cáo") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .testTag("month_report_selector")
                )
                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    reports.forEach { report ->
                        DropdownMenuItem(
                            text = { Text(report.displayMonth, color = MaterialTheme.colorScheme.onSurface) },
                            onClick = {
                                selectedMonthKey = report.monthKey
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Không có dữ liệu hóa đơn nào trong tháng.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (activeReport != null) {
            // Stats summary card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "TỔNG DOANH THU THÁNG",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatCurrency(activeReport.totalRevenue),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Từ Gói Premium", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 11.sp)
                            Text(text = formatCurrency(activeReport.packageSales), color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Từ Dịch Vụ Phát Sinh ngoài", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 11.sp)
                            Text(text = formatCurrency(activeReport.serviceSales), color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Export option
            Button(
                onClick = {
                    Toast.makeText(context, "Báo cáo doanh thu ${activeReport.displayMonth} đã được xuất thành công dưới dạng PDF/Excel vào thư mục Downloads!", Toast.LENGTH_LONG).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .testTag("export_report_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.TrendingUp, contentDescription = "Export", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Xuất Báo Cáo Tài Chính", color = MaterialTheme.colorScheme.onPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Chi Tiết Giao Dịch Trong Tháng",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(activeReport.itemizedRecords) { record ->
                    RevenueRecordItem(record = record)
                }
            }
        }
    }
}

@Composable
fun RevenueRecordItem(record: AppViewModel.RevenueRecord) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.customerName,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = record.description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Biển số: ${record.licensePlate} • Ngày: ${formatDate(record.date)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    fontSize = 10.sp
                )
            }

            Text(
                text = if (record.amount == 0.0) "0đ (Trừ gói)" else "+${formatCurrency(record.amount)}",
                color = if (record.amount == 0.0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
fun PricingScreen() {
    var isShowingPackageDetails by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Tab Headers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { isShowingPackageDetails = false },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!isShowingPackageDetails) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (!isShowingPackageDetails) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
            ) {
                Text(
                    "Đơn Giá Lẻ",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = { isShowingPackageDetails = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isShowingPackageDetails) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isShowingPackageDetails) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
            ) {
                Text(
                    "Gói Chăm Sóc Năm",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (!isShowingPackageDetails) {
            // Detailing price list (Image 1)
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ServiceRegistry.services) { service ->
                    ServicePriceItem(service = service)
                }
            }
        } else {
            // Premium package list (Image 2)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PremiumPackageDetailCard(
                    title = "SILVER PACKAGE",
                    price = "15.000.000đ / Năm",
                    color = Color(0xFF90A4AE),
                    specs = listOf(
                        "20 lần MIỄN PHÍ rửa xe nâng cao chuyên sâu",
                        "3 lần MIỄN PHÍ dọn dẹp vệ sinh nội thất VIP tháo ghế",
                        "3 lần MIỄN PHÍ đánh bóng sơn 3 bước & rửa khoang máy",
                        "3 lần MIỄN PHÍ tẩy mốc ố kính và khử khuẩn nội thất",
                        "GIẢM NGAY 10% tất cả dịch vụ phát sinh ngoài gói",
                        "Ưu tiên đặt lịch, hỗ trợ đưa đón xe tận nơi"
                    )
                )

                PremiumPackageDetailCard(
                    title = "GOLD PACKAGE",
                    price = "17.000.000đ / Năm",
                    color = Color(0xFFFFD54F),
                    specs = listOf(
                        "30 lần MIỄN PHÍ rửa xe nâng cao chuyên sâu",
                        "6 lần MIỄN PHÍ dọn dẹp vệ sinh nội thất VIP tháo ghế",
                        "6 lần MIỄN PHÍ đánh bóng sơn 3 bước & rửa khoang máy",
                        "6 lần MIỄN PHÍ tẩy mốc ố kính và khử khuẩn nội thất",
                        "GIẢM NGAY 15% tất cả dịch vụ phát sinh ngoài gói",
                        "Ưu tiên đặt lịch, hỗ trợ đưa đón xe tận nơi"
                    )
                )

                PremiumPackageDetailCard(
                    title = "DIAMOND PACKAGE",
                    price = "20.000.000đ / Năm",
                    color = Color(0xFF4FC3F7),
                    specs = listOf(
                        "40 lần MIỄN PHÍ rửa xe nâng cao kết hợp phủ Wax bảo vệ",
                        "12 lần MIỄN PHÍ dọn dẹp vệ sinh nội thất VIP tháo ghế",
                        "6 lần MIỄN PHÍ vệ sinh khoang máy chuyên sâu chống chuột",
                        "12 lần MIỄN PHÍ tẩy mốc ố kính và ozone khử mùi nội thất",
                        "1 lần MIỄN PHÍ đánh bóng hiệu chỉnh sơn và PHỦ CERAMIC 9H",
                        "GIẢM NGAY 15% tất cả dịch vụ phát sinh ngoài gói"
                    )
                )
            }
        }
    }
}

@Composable
fun ServicePriceItem(service: ServiceDefinition) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = service.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = service.durationText,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (service.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = service.description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PriceLabel(size = "MINI", price = service.priceMini)
                PriceLabel(size = "SEDAN", price = service.priceSedan)
                PriceLabel(size = "SUV", price = service.priceSuv)
            }
        }
    }
}

@Composable
fun PriceLabel(size: String, price: Double) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = size, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(
            text = if (price >= 1000000.0) "${(price/1000000.0).toString().removeSuffix(".0")}tr" else "${(price/1000.0).toInt()}k",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
fun PremiumPackageDetailCard(
    title: String,
    price: String,
    color: Color,
    specs: List<String>
) {
    val displayTitleColor = remember(title) {
        if (title.contains("SILVER")) Color(0xFF455A64)
        else if (title.contains("GOLD")) Color(0xFFB47A00)
        else Color(0xFF01579B)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = displayTitleColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = price,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                specs.forEach { spec ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(text = "✦", color = displayTitleColor, fontSize = 11.sp, modifier = Modifier.padding(end = 6.dp))
                        Text(
                            text = spec,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

// Formatting helpers
fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
    return format.format(amount).replace("₫", "đ")
}

fun formatShortCurrency(amount: Double): String {
    return if (amount >= 1000000.0) {
        String.format(Locale.getDefault(), "%.1ftr", amount / 1000000.0).replace(".0", "")
    } else {
        String.format(Locale.getDefault(), "%.0fk", amount / 1000.0)
    }
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
