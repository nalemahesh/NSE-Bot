// alerts/PriceAlertWorker.kt
package com.nsebot.alerts

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.nsebot.data.AppDatabase
import com.nsebot.data.NseApiService
import com.nsebot.data.models.AlertType
import com.nsebot.data.models.StockAlert
import java.util.concurrent.TimeUnit

class PriceAlertWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val CHANNEL_ID = "nse_price_alerts"
        const val WORK_NAME = "price_alert_check"

        // Schedule periodic checks every 15 minutes
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<PriceAlertWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getInstance(context)
            val api = NseApiService()
            val alerts = db.stockDao().getActiveAlerts()

            for (alert in alerts) {
                val stock = api.getLiveQuote(alert.symbol) ?: continue
                val triggered = when (alert.alertType) {
                    AlertType.ABOVE -> stock.lastPrice >= alert.targetPrice
                    AlertType.BELOW -> stock.lastPrice <= alert.targetPrice
                }

                if (triggered) {
                    sendNotification(alert, stock.lastPrice)
                    db.stockDao().deactivateAlert(alert.symbol, alert.targetPrice)
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun sendNotification(alert: StockAlert, currentPrice: Double) {
        createNotificationChannel()
        val direction = if (alert.alertType == AlertType.ABOVE) "above ↑" else "below ↓"
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🔔 ${alert.symbol} Price Alert!")
            .setContentText("${alert.symbol} is now $direction ₹${alert.targetPrice} — Current: ₹$currentPrice")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(alert.symbol.hashCode(), notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "NSE Price Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Alerts when stock prices cross your targets" }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
