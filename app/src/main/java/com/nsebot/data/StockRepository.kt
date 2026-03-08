// data/StockRepository.kt
package com.nsebot.data

import com.nsebot.data.models.QuarterlyResult
import com.nsebot.data.models.Stock
import kotlinx.coroutines.flow.Flow

class StockRepository(
    private val apiService: NseApiService,
    private val stockDao: StockDao          // Room DAO for caching
) {

    // F&O Stocks: try network, fall back to cache
    suspend fun getFnOStocks(forceRefresh: Boolean = false): List<Stock> {
        return try {
            val stocks = apiService.getFnOStockList()
            if (stocks.isNotEmpty()) {
                stockDao.insertAll(stocks)  // Cache locally
            }
            stocks
        } catch (e: Exception) {
            stockDao.getAllStocks()          // Return cached data if network fails
        }
    }

    // Live quote for single stock
    suspend fun getLiveQuote(symbol: String): Stock? {
        return apiService.getLiveQuote(symbol)
    }

    // Quarterly results (cached for 24 hrs)
    suspend fun getQuarterlyResults(symbol: String): List<QuarterlyResult> {
        val cached = stockDao.getQuarterlyResults(symbol)
        val isFresh = cached.isNotEmpty() &&
            (System.currentTimeMillis() - stockDao.getLastResultUpdate(symbol)) < 86400000L

        return if (isFresh) {
            cached
        } else {
            val results = apiService.getQuarterlyResults(symbol)
            if (results.isNotEmpty()) {
                stockDao.insertQuarterlyResults(results)
            }
            results
        }
    }

    // Search stocks by name or symbol
    fun searchStocks(query: String): Flow<List<Stock>> {
        return stockDao.searchStocks("%$query%")
    }
}
