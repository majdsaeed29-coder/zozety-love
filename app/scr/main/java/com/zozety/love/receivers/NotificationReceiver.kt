package com.zozety.love.receivers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.zozety.love.MainActivity
import com.zozety.love.R
import com.zozety.love.services.MessageService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationReceiver : BroadcastReceiver() {
    
    companion object {
        private const val CHANNEL_ID = "love_notifications_channel"
        private const val CHANNEL_NAME = "رسائل الحب"
        private const val NOTIFICATION_ID = 2000
        
        fun sendLoveNotification(context: Context, message: String) {
            createNotificationChannel(context)
            
            val notificationManager = context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager
            
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // صوت الإشعار
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_heart)
                .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.ic_heart))
                .setContentTitle("رسالة حب جديدة 💌")
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentIntent(pendingIntent)
                .setSound(soundUri)
                .setVibrate(longArrayOf(1000, 1000, 1000, 1000))
                .setAutoCancel(true)
                .setOngoing(false)
                .build()
            
            notificationManager.notify(NOTIFICATION_ID, notification)
            
            Log.d("Notification", "Love message sent: $message")
        }
        
        private fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "قناة إشعارات رسائل الحب"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(1000, 1000, 1000, 1000)
                }
                
                val notificationManager = context.getSystemService(
                    Context.NOTIFICATION_SERVICE
                ) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "com.zozety.love.ACTION_SEND_LOVE_MESSAGE" -> {
                CoroutineScope(Dispatchers.IO).launch {
                    // توليد رسالة حب
                    val message = try {
                        AIService.generateLoveMessage(context) ?: getLocalMessage()
                    } catch (e: Exception) {
                        getLocalMessage()
                    }
                    
                    // إرسال الإشعار
                    sendLoveNotification(context, message)
                    
                    // إعادة تشغيل الخدمة
                    MessageService.startService(context)
                }
            }
            
            Intent.ACTION_BOOT_COMPLETED -> {
                // إعادة تشغيل الخدمة بعد إعادة تشغيل الجهاز
                MessageService.startService(context)
            }
        }
    }
    
    private fun getLocalMessage(): String {
        val messages = listOf(
            "أحبك يا أجمل إنسان في حياتي! 💕",
            "تفكيري كله معكِ يا حبيبتي 🌹",
            "أنتِ سبب سعادتي وفرحتي 🥰",
            "كل لحظة بعيداً عنكِ تشعرني بالشوق 💘"
        )
        return messages.random()
    }
}
