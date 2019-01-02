package android.romstats

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

class Application(): android.app.Application() {
    override fun onCreate() {
        super.onCreate()
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationChannel = NotificationChannel(
            "romstats",
            getString(R.string.notification_channel_title),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationChannel.description = getString(R.string.notification_channel_desc)
        notificationChannel.setShowBadge(false)
        notificationManager.createNotificationChannel(notificationChannel)

    }
}