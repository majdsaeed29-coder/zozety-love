package com.zozety.love.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.zozety.love.MainActivity
import com.zozety.love.R
import com.zozety.love.receivers.NotificationReceiver
import kotlinx.coroutines.*

class MessageService : Service() {
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "love_service_channel"
        private const val SERVICE_NAME = "Zozety Love Service"
        
        fun startService(context: Context) {
            val intent = Intent(context, MessageService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, MessageService::class.java)
            context.stopService(intent)
        }
    }
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var alarmManager: AlarmManager
    private lateinit var pendingIntent: PendingIntent
    
    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        scheduleMessages()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // بدء إرسال الرسائل
        serviceScope.launch {
            sendScheduledLoveMessage()
        }
        return START_STICKY
    }
    
    private fun startForegroundService() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Zozety Love 💖")
            .setContentText("جاري إرسال رسائل الحب...")
            .setSmallIcon(R.drawable.ic_heart)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
        
        startForeground(NOTIFICATION_ID, notification)
    }
    
    private fun scheduleMessages() {
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        val intent = Intent(this, NotificationReceiver::class.java).apply {
            action = "com.zozety.love.ACTION_SEND_LOVE_MESSAGE"
        }
        
        pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // جدولة كل 30 دقيقة
        val interval = 30 * 60 * 1000L
        val triggerTime = System.currentTimeMillis() + interval
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }
    
    private suspend fun sendScheduledLoveMessage() {
        val messages = listOf(
            "أحبك يا أجمل إنسان في حياتي! 💕",
            "تفكيري كله معكِ يا حبيبتي 🌹",
            "أنتِ سبب سعادتي وفرحتي 🥰",
            "كل لحظة بعيداً عنكِ تشعرني بالشوق 💘",
            "عيني عليكِ باردة وقلبي معكِ ساكن 🙏"
        )
        
        val randomMessage = messages.random()
        
        // إرسال الإشعار
        withContext(Dispatchers.Main) {
            NotificationReceiver.sendLoveNotification(
                this@MessageService,
                randomMessage
            )
        }
        
        // إعادة الجدولة
        scheduleMessages()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        try {
            alarmManager.cancel(pendingIntent)
        } catch (e: Exception) {
            // تجاهل الخطأ
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
}
