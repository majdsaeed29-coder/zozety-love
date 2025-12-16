package com.zozety.love

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

class ZozetyApp : Application() {
    
    companion object {
        const val CHANNEL_ID = "love_messages_channel"
        const val CHANNEL_NAME = "رسائل الحب"
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "قناة إرسال رسائل الحب الرومانسية"
                enableVibration(true)
                vibrationPattern = longArrayOf(1000, 1000, 1000, 1000)
            }
            
            val notificationManager = getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
