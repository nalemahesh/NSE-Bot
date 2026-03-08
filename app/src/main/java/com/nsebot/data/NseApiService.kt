// data/NseApiService.kt
package com.nsebot.data

import com.nsebot.data.models.QuarterlyResult
import com.nsebot.data.models.Stock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Fetches live data from NSE India public APIs (no auth needed)
 * NSE provides JSON endpoints that are publicly accessible.
 */
class NseApiService {

    // NSE public API base URLs
    companion object {
        const val NSE_BASE = "https://www.nseindia.com"
        const val NSE_FNO_LIST = "$NSE_BASE/api/equity-stockIndices?index=SECURITIES%20IN%20F%26O"
        const val NSE_QUOTE = "$NSE_BASE/api/quote-equity?symbol="
        const val NSE_FINANCIALS = "$NSE_BASE/api/financial-results?index=equities&symbol="
        const val SCREENER_BASE = "https://www.screener.in/company/"
    }

    // OkHttp client with NSE headers (required to avoid 401)
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Accept", "application/json, text/plain, */*")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Referer", "https://www.nseindia.com/")
                .build()
            chain.proceed(request)
        }
        .build()

    // ─── F&O STOCKS LIST ────────────────────────────────────────────────────

    suspend fun getFnOStockList(): List<Stock> = withContext(Dispatchers.IO) {
        try {
            // Step 1: Get cookies by hitting homepage first
            val cookieRequest = Request.Builder().url(NSE_BASE).build()
            client.newCall(cookieRequest).execute().close()

            // Step 2: Fetch F&O list
            val request = Request.Builder().url(NSE_FNO_LIST).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext emptyList()

            val json = JSONObject(body)
            val data = json.getJSONArray("data")
            val stocks = mutableListOf<Stock>()

            for (i in 0 until data.length()) {
                val item = data.getJSONObject(i)
                stocks.add(
                    Stock(
                        symbol = item.optString("symbol"),
                        companyName = item.optString("meta_companyName", item.optString("symbol")),
                        series = item.optString("series", "EQ"),
                        lastPrice = item.optDouble("lastPrice", 0.0),
                        change = item.optDouble("change", 0.0),
                        changePercent = item.optDouble("pChange", 0.0),
                        volume = item.optLong("totalTradedVolume", 0L),
                        marketCap = item.optDouble("marketCap", 0.0),
                        exchange = "NSE"
                    )
                )
            }
            stocks
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // ─── LIVE QUOTE FOR A STOCK ─────────────────────────────────────────────

    suspend fun getLiveQuote(symbol: String): Stock? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$NSE_QUOTE${symbol.uppercase()}")
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext null
            val json = JSONObject(body)
            val priceInfo = json.getJSONObject("priceInfo")
            val info = json.getJSONObject("info")

            Stock(
                symbol = symbol.uppercase(),
                companyName = info.optString("companyName", symbol),
                series = info.optString("series", "EQ"),
                lastPrice = priceInfo.optDouble("lastPrice", 0.0),
                change = priceInfo.optDouble("change", 0.0),
                changePercent = priceInfo.optDouble("pChange", 0.0),
                volume = json.optJSONObject("marketDeptOrderBook")
                    ?.optJSONObject("tradeInfo")
                    ?.optLong("totalTradedVolume", 0L) ?: 0L,
                marketCap = 0.0,
                exchange = "NSE"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ─── QUARTERLY RESULTS ──────────────────────────────────────────────────

    suspend fun getQuarterlyResults(symbol: String): List<QuarterlyResult> =
        withContext(Dispatchers.IO) {
            try {
                // Scrape Screener.in for quarterly results
                val doc = Jsoup.connect("$SCREENER_BASE${symbol.uppercase()}/consolidated/")
                    .userAgent("Mozilla/5.0")
                    .timeout(15000)
                    .get()

                val results = mutableListOf<QuarterlyResult>()

                // Parse quarterly results table from Screener
                val table = doc.select("section#quarters table").firstOrNull()
                    ?: return@withContext emptyList()

                val headers = table.select("thead tr th").map { it.text() }
                val rows = table.select("tbody tr")

                // Find revenue and profit rows
                var revenueRow: List<String>? = null
                var profitRow: List<String>? = null

                for (row in rows) {
                    val label = row.select("td").firstOrNull()?.text()?.lowercase() ?: ""
                    val values = row.select("td").map { it.text() }
                    when {
                        label.contains("revenue") || label.contains("sales") -> revenueRow = values
                        label.contains("net profit") || label.contains("profit after tax") -> profitRow = values
                    }
                }

                // Build quarterly result objects for last 8 quarters
                for (i in 1 until minOf(headers.size, 9)) {
                    val quarter = headers.getOrNull(i) ?: continue
                    val revenue = revenueRow?.getOrNull(i)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
                    val profit = profitRow?.getOrNull(i)?.replace(",", "")?.toDoubleOrNull() ?: 0.0

                    val prevRevenue = revenueRow?.getOrNull(i + 4)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
                    val prevProfit = profitRow?.getOrNull(i + 4)?.replace(",", "")?.toDoubleOrNull() ?: 0.0

                    results.add(
                        QuarterlyResult(
                            symbol = symbol.uppercase(),
                            quarter = quarter,
                            revenue = revenue,
                            netProfit = profit,
                            eps = 0.0, // Can be parsed from EPS row if needed
                            revenueGrowthYoY = if (prevRevenue != 0.0) ((revenue - prevRevenue) / prevRevenue * 100) else 0.0,
                            profitGrowthYoY = if (prevProfit != 0.0) ((profit - prevProfit) / prevProfit * 100) else 0.0,
                            operatingMargin = if (revenue != 0.0) (profit / revenue * 100) else 0.0,
                            resultDate = quarter
                        )
                    )
                }
                results
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
}
