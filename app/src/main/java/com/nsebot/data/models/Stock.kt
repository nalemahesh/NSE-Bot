// data/models/Stock.kt
package com.nsebot.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stocks")
data class Stock(
    @PrimaryKey
    val symbol: String,
    val companyName: String,
    val series: String,          // EQ, BE, etc.
    val lastPrice: Double,
    val change: Double,
    val changePercent: Double,
    val volume: Long,
    val marketCap: Double,
    val isFnO: Boolean = true,   // All stocks here are F&O
    val exchange: String,        // NSE or BSE
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "quarterly_results")
data class QuarterlyResult(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val symbol: String,
    val quarter: String,         // e.g. "Q3 FY2024"
    val revenue: Double,         // in Crores
    val netProfit: Double,
    val eps: Double,             // Earnings Per Share
    val revenueGrowthYoY: Double, // Year-over-Year %
    val profitGrowthYoY: Double,
    val operatingMargin: Double,
    val resultDate: String
)

data class StockAlert(
    val symbol: String,
    val targetPrice: Double,
    val alertType: AlertType,    // ABOVE or BELOW
    val isActive: Boolean = true
)

enum class AlertType { ABOVE, BELOW }
