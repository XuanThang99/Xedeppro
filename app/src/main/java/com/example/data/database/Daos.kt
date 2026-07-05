package com.example.data.database

import androidx.room.*
import com.example.data.model.Customer
import com.example.data.model.SubscriptionPackage
import com.example.data.model.ServiceUsage
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY createdAt DESC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: Long): Customer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Delete
    suspend fun deleteCustomer(customer: Customer)
}

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions")
    fun getAllSubscriptions(): Flow<List<SubscriptionPackage>>

    @Query("SELECT * FROM subscriptions WHERE customerId = :customerId AND isActive = 1 LIMIT 1")
    suspend fun getActiveSubscriptionForCustomer(customerId: Long): SubscriptionPackage?

    @Query("SELECT * FROM subscriptions WHERE customerId = :customerId")
    fun getSubscriptionsForCustomerFlow(customerId: Long): Flow<List<SubscriptionPackage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(subscription: SubscriptionPackage): Long

    @Update
    suspend fun updateSubscription(subscription: SubscriptionPackage)

    @Delete
    suspend fun deleteSubscription(subscription: SubscriptionPackage)
}

@Dao
interface ServiceUsageDao {
    @Query("SELECT * FROM service_usages ORDER BY date DESC")
    fun getAllServiceUsages(): Flow<List<ServiceUsage>>

    @Query("SELECT * FROM service_usages WHERE customerId = :customerId ORDER BY date DESC")
    fun getServiceUsagesForCustomer(customerId: Long): Flow<List<ServiceUsage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServiceUsage(usage: ServiceUsage): Long

    @Update
    suspend fun updateServiceUsage(usage: ServiceUsage)

    @Delete
    suspend fun deleteServiceUsage(usage: ServiceUsage)
}
