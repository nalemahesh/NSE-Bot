// data/StockDao.kt
package com.nsebot.data

import androidx.room.*
import com.nsebot.data.models.QuarterlyResult
import com.nsebot.data.models.Stock
import kotlinx.coroutines.flow.Flow

@Dao
interface StockDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stocks: List<Stock>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuarterlyResults(results: List<QuarterlyResult>)

    @Query("SELECT * FROM stocks ORDER BY changePercent DESC")
    suspend fun getAllStocks(): List<Stock>

    @Query("SELECT * FROM stocks WHERE symbol LIKE :query OR companyName LIKE :query")
    fun searchStocks(query: String): Flow<List<Stock>>

    @Query("SELECT * FROM quarterly_results WHERE symbol = :symbol ORDER BY id DESC")
    suspend fun getQuarterlyResults(symbol: String): List<QuarterlyResult>

    @Query("SELECT lastUpdated FROM stocks WHERE symbol = :symbol LIMIT 1")
    suspend fun getLastResultUpdate(symbol: String): Long

    @Query("DELETE FROM stocks")
    suspend fun clearAll()
}

@Database(
    entities = [Stock::class, QuarterlyResult::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun stockDao(): StockDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "nsebot.db")
                    .build().also { INSTANCE = it }
            }
        }
    }
}
