package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.Customer
import com.example.data.model.ServiceRegistry
import com.example.data.model.ServiceUsage
import com.example.data.model.SubscriptionPackage
import com.example.data.repository.AppRepository
import com.example.data.repository.Sextuple
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = AppRepository(
        database.customerDao(),
        database.subscriptionDao(),
        database.serviceUsageDao()
    )

    // Raw Room flows
    val customers: StateFlow<List<Customer>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subscriptions: StateFlow<List<SubscriptionPackage>> = repository.allSubscriptions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val serviceUsages: StateFlow<List<ServiceUsage>> = repository.allServiceUsages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Syncing state
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    // Status Message / Toast
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage = _statusMessage.asStateFlow()

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    // Dashboard Statistics State
    data class DashboardStats(
        val totalCustomers: Int = 0,
        val totalActiveSubscriptions: Int = 0,
        val totalRevenue: Double = 0.0,
        val silverCount: Int = 0,
        val goldCount: Int = 0,
        val diamondCount: Int = 0,
        val activeExpiringSoon: List<ExpiringPackageInfo> = emptyList()
    )

    data class ExpiringPackageInfo(
        val customer: Customer,
        val subscription: SubscriptionPackage,
        val daysRemaining: Long
    )

    val dashboardStats: StateFlow<DashboardStats> = combine(
        customers,
        subscriptions,
        serviceUsages
    ) { custs, subs, usages ->
        val activeSubs = subs.filter { it.isActive && it.endDate > System.currentTimeMillis() }
        val now = System.currentTimeMillis()
        val expiringSoon = activeSubs.mapNotNull { sub ->
            val cust = custs.find { it.id == sub.customerId } ?: return@mapNotNull null
            val diffMs = sub.endDate - now
            val diffDays = diffMs / (1000 * 60 * 60 * 24)
            if (diffDays in 0..30) {
                ExpiringPackageInfo(cust, sub, diffDays)
            } else null
        }.sortedBy { it.daysRemaining }

        val totalPackageRev = subs.sumOf { it.price }
        val totalServiceRev = usages.sumOf { it.cost }

        DashboardStats(
            totalCustomers = custs.size,
            totalActiveSubscriptions = activeSubs.size,
            totalRevenue = totalPackageRev + totalServiceRev,
            silverCount = activeSubs.count { it.packageType == "SILVER" },
            goldCount = activeSubs.count { it.packageType == "GOLD" },
            diamondCount = activeSubs.count { it.packageType == "DIAMOND" },
            activeExpiringSoon = expiringSoon
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    // Monthly Revenue Breakdown (for monthly reports)
    data class MonthlyRevenueReport(
        val monthKey: String, // e.g. "07/2026"
        val displayMonth: String, // e.g. "Tháng 07, 2026"
        val packageSales: Double,
        val serviceSales: Double,
        val totalRevenue: Double,
        val itemizedRecords: List<RevenueRecord>
    )

    sealed interface RevenueRecord {
        val date: Long
        val customerName: String
        val licensePlate: String
        val amount: Double
        val description: String

        data class PackageSale(
            override val date: Long,
            override val customerName: String,
            override val licensePlate: String,
            override val amount: Double,
            val packageType: String
        ) : RevenueRecord {
            override val description: String = "Đăng ký Gói Chăm Sóc Năm $packageType"
        }

        data class StandaloneService(
            override val date: Long,
            override val customerName: String,
            override val licensePlate: String,
            override val amount: Double,
            val serviceName: String,
            val isPackageDeducted: Boolean
        ) : RevenueRecord {
            override val description: String = if (isPackageDeducted) {
                "Sử dụng $serviceName (Trừ gói năm)"
            } else {
                "Dịch vụ phát sinh ngoài: $serviceName"
            }
        }
    }

    val monthlyRevenueReports: StateFlow<List<MonthlyRevenueReport>> = combine(
        customers,
        subscriptions,
        serviceUsages
    ) { custs, subs, usages ->
        val sdfMonth = SimpleDateFormat("MM/yyyy", Locale.getDefault())
        val sdfDisplay = SimpleDateFormat("'Tháng' MM, yyyy", Locale.getDefault())

        val records = mutableListOf<RevenueRecord>()

        // Add packages
        for (sub in subs) {
            val cust = custs.find { it.id == sub.customerId } ?: continue
            records.add(
                RevenueRecord.PackageSale(
                    date = sub.startDate,
                    customerName = cust.name,
                    licensePlate = cust.licensePlate,
                    amount = sub.price,
                    packageType = sub.packageType
                )
            )
        }

        // Add service usages
        for (usage in usages) {
            val cust = custs.find { it.id == usage.customerId } ?: continue
            records.add(
                RevenueRecord.StandaloneService(
                    date = usage.date,
                    customerName = cust.name,
                    licensePlate = cust.licensePlate,
                    amount = usage.cost,
                    serviceName = usage.serviceName,
                    isPackageDeducted = usage.isPackageDeducted
                )
            )
        }

        // Group by month
        val grouped = records.groupBy { sdfMonth.format(Date(it.date)) }

        grouped.map { (monthKey, list) ->
            val packagesSum = list.filterIsInstance<RevenueRecord.PackageSale>().sumOf { it.amount }
            val servicesSum = list.filterIsInstance<RevenueRecord.StandaloneService>().sumOf { it.amount }
            val total = packagesSum + servicesSum
            
            // Generate friendly display title
            val sampleDate = list.first().date
            val displayTitle = sdfDisplay.format(Date(sampleDate))

            MonthlyRevenueReport(
                monthKey = monthKey,
                displayMonth = displayTitle,
                packageSales = packagesSum,
                serviceSales = servicesSum,
                totalRevenue = total,
                itemizedRecords = list.sortedByDescending { it.date }
            )
        }.sortedByDescending {
            // Sort by month parsed chronologically
            val parts = it.monthKey.split("/")
            if (parts.size == 2) {
                val month = parts[0].toIntOrNull() ?: 0
                val year = parts[1].toIntOrNull() ?: 0
                year * 100 + month
            } else 0
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getCustomerServiceUsages(customerId: Long): Flow<List<ServiceUsage>> =
        repository.getCustomerServiceUsages(customerId)

    // --- Actions & Database Mutations ---

    fun insertCustomer(name: String, phone: String, licensePlate: String, carType: String) {
        viewModelScope.launch {
            val customer = Customer(
                name = name.trim(),
                phone = phone.trim(),
                licensePlate = licensePlate.trim().uppercase(),
                carType = carType
            )
            repository.insertCustomer(customer)
            _statusMessage.value = "Đã thêm khách hàng ${customer.name} thành công!"
        }
    }

    fun updateCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.updateCustomer(customer)
            _statusMessage.value = "Đã cập nhật thông tin khách hàng ${customer.name}!"
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
            _statusMessage.value = "Đã xóa khách hàng ${customer.name}!"
        }
    }

    fun registerPackageForCustomer(customerId: Long, packageType: String, customStartDate: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            val (price, wash, vip, eng, glass, ceramic) = when (packageType) {
                "SILVER" -> Sextuple(15000000.0, 20, 3, 3, 3, 0)
                "GOLD" -> Sextuple(17000000.0, 30, 6, 6, 6, 0)
                "DIAMOND" -> Sextuple(20000000.0, 40, 12, 6, 12, 1)
                else -> Sextuple(0.0, 0, 0, 0, 0, 0)
            }

            val calendar = Calendar.getInstance()
            calendar.timeInMillis = customStartDate
            calendar.add(Calendar.YEAR, 1)
            val endDate = calendar.timeInMillis

            // Cancel any existing active subscriptions first
            val currentActive = repository.getActiveSubscription(customerId)
            if (currentActive != null) {
                repository.updateSubscription(currentActive.copy(isActive = false))
            }

            val sub = SubscriptionPackage(
                customerId = customerId,
                packageType = packageType,
                price = price,
                startDate = customStartDate,
                endDate = endDate,
                remainingWash = wash,
                remainingVipInterior = vip,
                remainingEnginePolish = eng,
                remainingGlassDeodorize = glass,
                remainingCeramic = ceramic
            )
            repository.insertSubscription(sub)
            
            val cust = repository.getCustomerById(customerId)
            _statusMessage.value = "Đăng ký thành công Gói ${packageType} cho khách hàng ${cust?.name ?: ""}!"
        }
    }

    fun cancelActiveSubscription(subscription: SubscriptionPackage) {
        viewModelScope.launch {
            repository.updateSubscription(subscription.copy(isActive = false))
            _statusMessage.value = "Đã dừng kích hoạt gói dịch vụ!"
        }
    }

    /**
     * Records a new service use log.
     * Decides whether to deduct from the active annual package, or charge standard pay-as-you-go.
     */
    fun logServiceUse(
        customerId: Long,
        serviceName: String,
        vehicleSize: String,
        customNotes: String = ""
    ) {
        viewModelScope.launch {
            val activeSub = repository.getActiveSubscription(customerId)
            var deductSuccessful = false
            var finalCost = ServiceRegistry.getPrice(serviceName, vehicleSize)

            // Attempt package deduction based on package specs if subscription is active
            if (activeSub != null && activeSub.endDate > System.currentTimeMillis()) {
                val packageType = activeSub.packageType

                when {
                    // 1. Rửa nâng cao / Rửa NC + Wax
                    (serviceName == "Rửa nâng cao" || (serviceName == "Rửa NC + Wax" && packageType == "DIAMOND")) && activeSub.remainingWash > 0 -> {
                        repository.updateSubscription(activeSub.copy(remainingWash = activeSub.remainingWash - 1))
                        deductSuccessful = true
                        finalCost = 0.0
                    }
                    // 2. Vệ sinh nội thất VIP
                    serviceName == "VS nội thất VIP" && activeSub.remainingVipInterior > 0 -> {
                        repository.updateSubscription(activeSub.copy(remainingVipInterior = activeSub.remainingVipInterior - 1))
                        deductSuccessful = true
                        finalCost = 0.0
                    }
                    // 3. Đánh bóng 3 bước & Vệ sinh khoang máy / Vệ sinh khoang máy
                    (serviceName == "Đánh bóng 3 bước" || serviceName == "Rửa khoang máy") && activeSub.remainingEnginePolish > 0 -> {
                        repository.updateSubscription(activeSub.copy(remainingEnginePolish = activeSub.remainingEnginePolish - 1))
                        deductSuccessful = true
                        finalCost = 0.0
                    }
                    // 4. Tẩy ố kính & Khử mùi nội thất
                    (serviceName == "Tẩy ố kính" || serviceName == "Khử khuẩn nội thất" || serviceName == "Tẩy ố + Phủ kính") && activeSub.remainingGlassDeodorize > 0 -> {
                        repository.updateSubscription(activeSub.copy(remainingGlassDeodorize = activeSub.remainingGlassDeodorize - 1))
                        deductSuccessful = true
                        finalCost = 0.0
                    }
                    // 5. Ceramic (Diamond only)
                    serviceName == "Phủ Ceramic 9H" && activeSub.remainingCeramic > 0 -> {
                        repository.updateSubscription(activeSub.copy(remainingCeramic = activeSub.remainingCeramic - 1))
                        deductSuccessful = true
                        finalCost = 0.0
                    }
                }

                // If no direct coupon, apply standard package discount (10% Silver, 15% Gold/Diamond)
                if (!deductSuccessful) {
                    val discountPercent = if (packageType == "SILVER") 0.10 else 0.15
                    finalCost = finalCost * (1.0 - discountPercent)
                }
            }

            // Create service usage log
            val usageLog = ServiceUsage(
                customerId = customerId,
                serviceName = serviceName,
                vehicleSize = vehicleSize,
                cost = finalCost,
                isPackageDeducted = deductSuccessful,
                notes = if (deductSuccessful) {
                    "Khấu trừ gói năm. $customNotes"
                } else {
                    if (activeSub != null) "Áp dụng giảm giá gói ${activeSub.packageType}. $customNotes" else customNotes
                }
            )

            repository.insertServiceUsage(usageLog)
            val cust = repository.getCustomerById(customerId)
            _statusMessage.value = "Ghi nhận dịch vụ '$serviceName' cho xe ${cust?.licensePlate ?: ""} thành công!"
        }
    }

    // Trigger website synchronisation
    fun syncWithWebsite() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val syncedCount = repository.syncFromXeDepProWebsite()
                if (syncedCount > 0) {
                    _statusMessage.value = "Đồng bộ thành công! Thêm mới $syncedCount khách hàng đăng ký từ xedep.pro"
                } else {
                    _statusMessage.value = "Dữ liệu trên xedep.pro đã được đồng bộ đồng nhất với hệ thống quản lý."
                }
            } catch (e: Exception) {
                _statusMessage.value = "Lỗi đồng bộ dữ liệu: ${e.localizedMessage}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    // Generate messaging templates for SMS/Zalo/Messenger copy/paste
    fun generateNotificationText(
        customer: Customer,
        sub: SubscriptionPackage?,
        type: String, // "EXPIRY", "SUMMARY", "THANKYOU", "SERVICE_DONE"
        selectedServiceName: String = ""
    ): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val formattedDate = sdf.format(Date(sub?.endDate ?: System.currentTimeMillis()))
        val today = sdf.format(Date())

        return when (type) {
            "EXPIRY" -> {
                "Kính gửi Quý khách ${customer.name}, Xe Đẹp Pro (https://xedep.pro) xin thông báo gói dịch vụ chăm sóc năm ${sub?.packageType ?: ""} của xe ${customer.licensePlate} sẽ hết hạn vào ngày $formattedDate.\n\n" +
                "Hiện tại Quý khách vẫn còn:\n" +
                "- Rửa xe nâng cao: ${sub?.remainingWash ?: 0} lượt\n" +
                "- Vệ sinh nội thất VIP: ${sub?.remainingVipInterior ?: 0} lượt\n" +
                "- Đánh bóng/Vệ sinh khoang máy: ${sub?.remainingEnginePolish ?: 0} lượt\n" +
                "- Tẩy ố kính/Khử mùi: ${sub?.remainingGlassDeodorize ?: 0} lượt\n" +
                "chưa sử dụng.\n\n" +
                "Kính mời Quý khách đặt lịch chăm sóc tuần này qua hotline hoặc website xedep.pro để tận dụng tối đa quyền lợi trước khi hết hạn. Xin trân trọng cảm ơn!"
            }
            "SUMMARY" -> {
                "Xin chào anh/chị ${customer.name},\n" +
                "Xe Đẹp Pro xin gửi thông tin số dư lượt chăm sóc định kỳ cho xe ${customer.licensePlate} (Gói năm ${sub?.packageType ?: ""}):\n" +
                "- Rửa xe nâng cao: ${sub?.remainingWash ?: 0} lần\n" +
                "- Dọn nội thất VIP: ${sub?.remainingVipInterior ?: 0} lần\n" +
                "- Vệ sinh khoang máy: ${sub?.remainingEnginePolish ?: 0} lần\n" +
                "- Tẩy ố kính: ${sub?.remainingGlassDeodorize ?: 0} lần\n" +
                "Ưu đãi giảm giá 10% - 15% tất cả dịch vụ phát sinh ngoài.\n" +
                "Đặt lịch ngay hôm nay để nhận ưu tiên phục vụ. Trân trọng cảm ơn!"
            }
            "THANKYOU" -> {
                "Xe Đẹp Pro chân thành cảm ơn Quý khách ${customer.name} đã mang xế yêu ${customer.licensePlate} đến chăm sóc định kỳ ngày $today.\n\n" +
                "Hy vọng Quý khách hài lòng với chất lượng dịch vụ của chúng tôi. Chúc Quý khách luôn có những hành trình vạn dặm bình an và tràn đầy may mắn trên chiếc xế yêu sáng bóng! Hotline hỗ trợ kỹ thuật: 09x.xxx.xxx"
            }
            "SERVICE_DONE" -> {
                "Chào anh/chị ${customer.name},\n" +
                "Xe Đẹp Pro xin báo cáo dịch vụ '$selectedServiceName' cho xe ${customer.licensePlate} đã hoàn tất chỉn chu và sẵn sàng bàn giao.\n" +
                "Kính mời anh/chị đến trung tâm nhận xe hoặc kiểm tra hình ảnh qua zalo. Chúc anh/chị một ngày vui vẻ!"
            }
            else -> ""
        }
    }
}
