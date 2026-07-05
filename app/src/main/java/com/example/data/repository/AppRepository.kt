package com.example.data.repository

import com.example.data.database.CustomerDao
import com.example.data.database.SubscriptionDao
import com.example.data.database.ServiceUsageDao
import com.example.data.model.Customer
import com.example.data.model.SubscriptionPackage
import com.example.data.model.ServiceUsage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Calendar

class AppRepository(
    private val customerDao: CustomerDao,
    private val subscriptionDao: SubscriptionDao,
    private val serviceUsageDao: ServiceUsageDao
) {
    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()
    val allSubscriptions: Flow<List<SubscriptionPackage>> = subscriptionDao.getAllSubscriptions()
    val allServiceUsages: Flow<List<ServiceUsage>> = serviceUsageDao.getAllServiceUsages()

    suspend fun getCustomerById(id: Long): Customer? = customerDao.getCustomerById(id)
    
    suspend fun getActiveSubscription(customerId: Long): SubscriptionPackage? =
        subscriptionDao.getActiveSubscriptionForCustomer(customerId)

    fun getCustomerServiceUsages(customerId: Long): Flow<List<ServiceUsage>> =
        serviceUsageDao.getServiceUsagesForCustomer(customerId)

    suspend fun insertCustomer(customer: Customer): Long = customerDao.insertCustomer(customer)
    
    suspend fun updateCustomer(customer: Customer) = customerDao.updateCustomer(customer)
    
    suspend fun deleteCustomer(customer: Customer) = customerDao.deleteCustomer(customer)

    suspend fun insertSubscription(subscription: SubscriptionPackage): Long =
        subscriptionDao.insertSubscription(subscription)
        
    suspend fun updateSubscription(subscription: SubscriptionPackage) =
        subscriptionDao.updateSubscription(subscription)
        
    suspend fun deleteSubscription(subscription: SubscriptionPackage) =
        subscriptionDao.deleteSubscription(subscription)

    suspend fun insertServiceUsage(usage: ServiceUsage): Long =
        serviceUsageDao.insertServiceUsage(usage)
        
    suspend fun deleteServiceUsage(usage: ServiceUsage) =
        serviceUsageDao.deleteServiceUsage(usage)

    /**
     * Simulates fetching customer and subscription registrations from the Xe Đẹp Pro server.
     * Inserts fresh sync rows into the local database.
     */
    suspend fun syncFromXeDepProWebsite(): Int {
        // Simulate network latency
        delay(2000)

        val existing = allCustomers.first()
        var addedCount = 0

        // Mock synchronized items
        val webCustomers = listOf(
            Triple(
                Customer(name = "Nguyễn Hoàng Long", phone = "0912345678", licensePlate = "30A-999.99", carType = "SEDAN", syncSource = "xedep.pro"),
                "GOLD",
                listOf(
                    Triple("Rửa nâng cao", "SEDAN", true),
                    Triple("VS nội thất VIP", "SEDAN", true)
                )
            ),
            Triple(
                Customer(name = "Phạm Quỳnh Anh", phone = "0987654321", licensePlate = "29C-111.22", carType = "MINI", syncSource = "xedep.pro"),
                "SILVER",
                listOf(
                    Triple("Rửa nâng cao", "MINI", true)
                )
            ),
            Triple(
                Customer(name = "Trần Minh Quân", phone = "0905556667", licensePlate = "51H-888.88", carType = "SUV", syncSource = "xedep.pro"),
                "DIAMOND",
                listOf(
                    Triple("Rửa NC + Wax", "SUV", true),
                    Triple("Phủ Ceramic 9H", "SUV", false) // normal service with standard price or custom
                )
            ),
            Triple(
                Customer(name = "Lê Thị Hồng", phone = "0934112233", licensePlate = "43A-444.55", carType = "SEDAN", syncSource = "xedep.pro"),
                "NONE",
                listOf(
                    Triple("Rửa cơ bản", "SEDAN", false),
                    Triple("Tẩy ố kính", "SEDAN", false)
                )
            ),
            Triple(
                Customer(name = "Vũ Quang Đăng", phone = "0977889900", licensePlate = "30E-666.88", carType = "SUV", syncSource = "xedep.pro"),
                "GOLD",
                emptyList()
            )
        )

        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        // 1 year subscription duration
        calendar.add(Calendar.YEAR, 1)
        val oneYearLater = calendar.timeInMillis

        for ((cust, packageType, usages) in webCustomers) {
            // Check if customer with the same phone or license plate already exists
            if (existing.any { it.phone == cust.phone || it.licensePlate == cust.licensePlate }) {
                continue
            }

            // Insert customer
            val custId = insertCustomer(cust)
            addedCount++

            // Insert subscription if any
            if (packageType != "NONE") {
                val (price, wash, vip, eng, glass, ceramic) = when (packageType) {
                    "SILVER" -> Sextuple(15000000.0, 20, 3, 3, 3, 0)
                    "GOLD" -> Sextuple(17000000.0, 30, 6, 6, 6, 0)
                    "DIAMOND" -> Sextuple(20000000.0, 40, 12, 6, 12, 1)
                    else -> Sextuple(0.0, 0, 0, 0, 0, 0)
                }

                // DEDUCT already used items if mock logs are packages
                val actualWash = wash - usages.count { it.first.contains("Rửa") && it.third }
                val actualVip = vip - usages.count { it.first.contains("nội thất VIP") && it.third }
                val actualEng = eng - usages.count { it.first.contains("Đánh bóng") && it.third }
                val actualGlass = glass - usages.count { it.first.contains("Tẩy ố") && it.third }

                val sub = SubscriptionPackage(
                    customerId = custId,
                    packageType = packageType,
                    price = price,
                    startDate = now,
                    endDate = oneYearLater,
                    remainingWash = actualWash,
                    remainingVipInterior = actualVip,
                    remainingEnginePolish = actualEng,
                    remainingGlassDeodorize = actualGlass,
                    remainingCeramic = ceramic
                )
                insertSubscription(sub)
            }

            // Insert mock past usages
            for ((srvName, vehicleSize, isDeducted) in usages) {
                val cost = if (isDeducted) 0.0 else com.example.data.model.ServiceRegistry.getPrice(srvName, vehicleSize)
                val serviceUsage = ServiceUsage(
                    customerId = custId,
                    serviceName = srvName,
                    vehicleSize = vehicleSize,
                    date = now - (86400000L * (1..10).random()), // 1 to 10 days ago
                    cost = cost,
                    isPackageDeducted = isDeducted,
                    notes = "Đồng bộ tự động từ website xedep.pro"
                )
                insertServiceUsage(serviceUsage)
            }
        }

        return addedCount
    }
}

// Simple wrapper class for 6 elements
data class Sextuple<A, B, C, D, E, F>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
    val sixth: F
)
