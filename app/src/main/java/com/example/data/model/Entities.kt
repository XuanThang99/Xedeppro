package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val licensePlate: String,
    val carType: String, // "MINI", "SEDAN", "SUV"
    val syncSource: String = "Manual", // "xedep.pro" or "Manual"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "subscriptions")
data class SubscriptionPackage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val packageType: String, // "SILVER", "GOLD", "DIAMOND"
    val price: Double, // 15000000.0, 17000000.0, 20000000.0
    val startDate: Long,
    val endDate: Long,
    
    // Remaining standard services
    val remainingWash: Int, // SILVER: 20 Premium Wash | GOLD: 30 Premium Wash | DIAMOND: 40 Premium Wash + Wax
    val remainingVipInterior: Int, // SILVER: 3 | GOLD: 6 | DIAMOND: 12
    val remainingEnginePolish: Int, // SILVER: 3 (Polish + Engine) | GOLD: 6 (Polish + Engine) | DIAMOND: 6 (Engine Wash only)
    val remainingGlassDeodorize: Int, // SILVER: 3 | GOLD: 6 | DIAMOND: 12 (Glass stain + deodorize)
    val remainingCeramic: Int, // SILVER: 0 | GOLD: 0 | DIAMOND: 1 (Ceramic coating + Polish)
    
    val isActive: Boolean = true
)

@Entity(tableName = "service_usages")
data class ServiceUsage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val serviceName: String,
    val vehicleSize: String,
    val date: Long = System.currentTimeMillis(),
    val cost: Double, // 0 if covered by subscription package, otherwise normal size price
    val isPackageDeducted: Boolean,
    val notes: String = ""
)

// In-Memory Representation of Single Services
data class ServiceDefinition(
    val name: String,
    val priceMini: Double,
    val priceSedan: Double,
    val priceSuv: Double,
    val durationText: String,
    val description: String = ""
)

object ServiceRegistry {
    val services = listOf(
        ServiceDefinition("Rửa cơ bản", 130000.0, 140000.0, 150000.0, "30-40 phút", "Rửa vỏ và hút bụi cơ bản"),
        ServiceDefinition("Rửa nâng cao", 180000.0, 190000.0, 200000.0, "40-50 phút", "Rửa nâng cao chi tiết bánh mâm gầm"),
        ServiceDefinition("Rửa NC + Wax", 200000.0, 250000.0, 300000.0, "50-60 phút", "Rửa nâng cao kết hợp phủ wax bóng sơn"),
        ServiceDefinition("Rửa khoang máy", 800000.0, 900000.0, 1000000.0, "1-2 giờ", "Vệ sinh khoang động cơ chuyên sâu"),
        ServiceDefinition("VS nội thất cơ bản", 600000.0, 650000.0, 700000.0, "60-80 phút", "Dọn nội thất và lau sạch bề mặt"),
        ServiceDefinition("VS nội thất VIP", 1500000.0, 2000000.0, 2500000.0, "4-8 giờ", "Dọn nội thất VIP tháo ghế giặt sàn sấy khô"),
        ServiceDefinition("Đánh bóng 2 bước", 2000000.0, 2500000.0, 3000000.0, "8-12 giờ", "Xóa xước dăm và làm bóng trung bình"),
        ServiceDefinition("Đánh bóng 3 bước", 2500000.0, 3000000.0, 3500000.0, "8-14 giờ", "Đánh bóng gương loại bỏ hoàn toàn vết xước"),
        ServiceDefinition("Phủ Ceramic 9H", 13000000.0, 15000000.0, 17000000.0, "8-16 giờ", "Sơn phủ bảo vệ Ceramic độ cứng 9H"),
        ServiceDefinition("Tẩy ố kính", 300000.0, 350000.0, 400000.0, "30-60 phút", "Tẩy ố mốc kính chắn gió và kính sườn"),
        ServiceDefinition("Tẩy ố + Phủ kính", 600000.0, 650000.0, 700000.0, "60-90 phút", "Tẩy ố kính kết hợp phủ nano kháng nước"),
        ServiceDefinition("Khử khuẩn nội thất", 200000.0, 250000.0, 300000.0, "8-12 giờ", "Ozone khử mùi diệt khuẩn máy lạnh nội thất"),
        ServiceDefinition("Tẩy bụi sơn", 500000.0, 600000.0, 700000.0, "1-2 giờ", "Tẩy sạch bụi sơn bám trên bề mặt"),
        ServiceDefinition("Đánh bóng từng vết", 100000.0, 100000.0, 100000.0, "30-60 phút", "Xử lý nhanh các vết xước cục bộ"),
        ServiceDefinition("Gõ móp PDR", 500000.0, 500000.0, 500000.0, "2-24 giờ", "Nắn móp không sơn công nghệ PDR")
    )

    fun getPrice(serviceName: String, carType: String): Double {
        val s = services.find { it.name.equals(serviceName, ignoreCase = true) } ?: return 0.0
        return when (carType.uppercase()) {
            "MINI" -> s.priceMini
            "SEDAN" -> s.priceSedan
            "SUV" -> s.priceSuv
            else -> s.priceSedan
        }
    }
}
