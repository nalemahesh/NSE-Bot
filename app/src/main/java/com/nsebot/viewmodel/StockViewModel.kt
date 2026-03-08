// viewmodel/StockViewModel.kt
package com.nsebot.viewmodel

import androidx.lifecycle.*
import com.nsebot.data.StockRepository
import com.nsebot.data.models.QuarterlyResult
import com.nsebot.data.models.Stock
import kotlinx.coroutines.launch

class StockViewModel(private val repository: StockRepository) : ViewModel() {

    // F&O Stock list
    private val _stocks = MutableLiveData<List<Stock>>()
    val stocks: LiveData<List<Stock>> = _stocks

    // Selected stock detail
    private val _selectedStock = MutableLiveData<Stock?>()
    val selectedStock: LiveData<Stock?> = _selectedStock

    // Quarterly results
    private val _quarterlyResults = MutableLiveData<List<QuarterlyResult>>()
    val quarterlyResults: LiveData<List<QuarterlyResult>> = _quarterlyResults

    // Bot response
    private val _botResponse = MutableLiveData<String>()
    val botResponse: LiveData<String> = _botResponse

    // Loading & error states
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // ─── LOAD F&O LIST ───────────────────────────────────────────────────

    fun loadFnOStocks(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.getFnOStocks(forceRefresh)
                _stocks.value = result
            } catch (e: Exception) {
                _error.value = "Failed to load stocks: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ─── LOAD SINGLE STOCK ───────────────────────────────────────────────

    fun loadStockDetail(symbol: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val stock = repository.getLiveQuote(symbol)
                _selectedStock.value = stock
                loadQuarterlyResults(symbol)
            } catch (e: Exception) {
                _error.value = "Failed to load $symbol: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ─── LOAD QUARTERLY RESULTS ──────────────────────────────────────────

    fun loadQuarterlyResults(symbol: String) {
        viewModelScope.launch {
            try {
                val results = repository.getQuarterlyResults(symbol)
                _quarterlyResults.value = results
            } catch (e: Exception) {
                _error.value = "Failed to load results for $symbol"
            }
        }
    }

    // ─── BOT QUERY HANDLER ───────────────────────────────────────────────

    fun processQuery(query: String) {
        viewModelScope.launch {
            val q = query.trim().lowercase()
            when {
                // "show fno list" / "list all stocks"
                q.contains("list") || q.contains("fno") || q.contains("f&o") -> {
                    loadFnOStocks()
                    _botResponse.value = "📋 Loading F&O stocks list from NSE..."
                }

                // "price of RELIANCE" / "quote INFY"
                q.contains("price") || q.contains("quote") -> {
                    val symbol = extractSymbol(q)
                    if (symbol != null) {
                        val stock = repository.getLiveQuote(symbol)
                        _botResponse.value = if (stock != null) {
                            """
                            📈 ${stock.companyName} (${stock.symbol})
                            💰 Price: ₹${stock.lastPrice}
                            📊 Change: ${stock.change} (${stock.changePercent}%)
                            📦 Volume: ${stock.volume}
                            """.trimIndent()
                        } else "❌ Could not find stock: $symbol"
                    } else {
                        _botResponse.value = "❓ Please mention a stock symbol. Example: 'price of RELIANCE'"
                    }
                }

                // "quarterly results of TCS" / "results HDFCBANK"
                q.contains("result") || q.contains("quarter") || q.contains("eps") -> {
                    val symbol = extractSymbol(q)
                    if (symbol != null) {
                        val results = repository.getQuarterlyResults(symbol)
                        _botResponse.value = if (results.isNotEmpty()) {
                            val latest = results.first()
                            """
                            📊 ${symbol} — ${latest.quarter}
                            💵 Revenue: ₹${latest.revenue} Cr
                            💰 Net Profit: ₹${latest.netProfit} Cr
                            📈 EPS: ₹${latest.eps}
                            📊 Revenue Growth YoY: ${String.format("%.1f", latest.revenueGrowthYoY)}%
                            📊 Profit Growth YoY: ${String.format("%.1f", latest.profitGrowthYoY)}%
                            """.trimIndent()
                        } else "❌ No quarterly data found for $symbol"
                        _quarterlyResults.value = results
                    } else {
                        _botResponse.value = "❓ Please mention a stock symbol. Example: 'results of TCS'"
                    }
                }

                // Help
                q.contains("help") || q.contains("what can you do") -> {
                    _botResponse.value = """
                    🤖 I can help you with:
                    
                    📋 "show fno list" — All NSE F&O stocks
                    💰 "price of RELIANCE" — Live quote
                    📊 "results of TCS" — Quarterly results
                    🔔 "alert INFY above 1500" — Price alert
                    📈 "top gainers" — Best performers today
                    📉 "top losers" — Worst performers today
                    """.trimIndent()
                }

                // Top gainers
                q.contains("gainer") -> {
                    val stocks = _stocks.value?.sortedByDescending { it.changePercent }?.take(5)
                    _botResponse.value = if (!stocks.isNullOrEmpty()) {
                        "🚀 Top Gainers:\n" + stocks.joinToString("\n") {
                            "• ${it.symbol}: +${it.changePercent}% @ ₹${it.lastPrice}"
                        }
                    } else "Please load the F&O list first. Try: 'show fno list'"
                }

                // Top losers
                q.contains("loser") -> {
                    val stocks = _stocks.value?.sortedBy { it.changePercent }?.take(5)
                    _botResponse.value = if (!stocks.isNullOrEmpty()) {
                        "📉 Top Losers:\n" + stocks.joinToString("\n") {
                            "• ${it.symbol}: ${it.changePercent}% @ ₹${it.lastPrice}"
                        }
                    } else "Please load the F&O list first. Try: 'show fno list'"
                }

                else -> {
                    _botResponse.value = "🤔 I didn't understand that. Type 'help' to see what I can do!"
                }
            }
        }
    }

    // Extract stock symbol from natural language query
    private fun extractSymbol(query: String): String? {
        val knownSymbols = listOf(
            "RELIANCE", "TCS", "INFY", "HDFCBANK", "ICICIBANK", "WIPRO",
            "BAJFINANCE", "AXISBANK", "KOTAKBANK", "SBIN", "HINDUNILVR",
            "TATAMOTORS", "ONGC", "POWERGRID", "NTPC", "MARUTI", "NESTLEIND",
            "LT", "SUNPHARMA", "TITAN", "TECHM", "ULTRACEMCO", "ADANIENT"
        )
        val upper = query.uppercase()
        return knownSymbols.firstOrNull { upper.contains(it) }
            ?: upper.split(" ").lastOrNull { it.length in 3..12 && it.all { c -> c.isLetter() } }
    }

    class Factory(private val repository: StockRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return StockViewModel(repository) as T
        }
    }
}
