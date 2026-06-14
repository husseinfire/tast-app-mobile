package com.example.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity

object NotificationHelper {

    private const val CHANNEL_ID = "chronos_ai_alerts"
    private const val LIVE_CHANNEL_ID = "chronos_continuous_sync"
    
    const val CONTINUOUS_NOTIF_ID = 4001
    const val ALERT_NOTIF_ID = 4002

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Chronos AI Study Alerts"
            val descriptionText = "Alerts and instant study cues scheduled by Chronos Optimizer"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }

            val liveName = "Continuous AI Telemetry"
            val liveDescriptionText = "Ongoing status linking your current device routine with the Gemini pipeline"
            val liveImportance = NotificationManager.IMPORTANCE_LOW
            val liveChannel = NotificationChannel(LIVE_CHANNEL_ID, liveName, liveImportance).apply {
                description = liveDescriptionText
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            notificationManager.createNotificationChannel(liveChannel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                "android.permission.POST_NOTIFICATIONS"
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Emphasizes the importance of continuous notifications by launching a persistent background lock screen sync notification.
     * Tells the user that the AI routine analyzer relies on constant background alignment.
     */
    fun showContinuousSyncNotification(context: Context, studyTopic: String, bookGenre: String) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, LIVE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Chronos AI: Persistent Sync Active")
            .setContentText("Continuous telemetry feeds active: Study Topic: $studyTopic")
            .setSubText("AI Energy Saver Enabled")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "Your routine is keeping the Gemini context pipeline aligned.\n" +
                "• Target study slot: $studyTopic\n" +
                "• Genre target: $bookGenre\n" +
                "Continuous notification prevents Android's system task killer from pausing idle telemetry."
            ))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true) // Marks it as continuous
            .setContentIntent(pendingIntent)
            .setColor(0xFF34D399.toInt()) // Mint color

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(CONTINUOUS_NOTIF_ID, builder.build())
    }

    fun cancelContinuousSyncNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(CONTINUOUS_NOTIF_ID)
    }

    /**
     * Fires an high-prominence alert tool alarm indicating study study hours are slot-starting.
     */
    fun triggerInstantAlert(context: Context, slotName: String, timeRange: String, category: String) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("🚨 Chronos Study Cue Alert!")
            .setContentText("Time to: $slotName ($timeRange)")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "Your allocated $category block starting now! Focus is key.\n" +
                "Activity: $slotName\n" +
                "Scheduled Slot: $timeRange\n\n" +
                "Open the app to complete and log your reclaimed habit points."
            ))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(0xFFFB7185.toInt()) // Neon Coral accent

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(ALERT_NOTIF_ID, builder.build())
    }
}
