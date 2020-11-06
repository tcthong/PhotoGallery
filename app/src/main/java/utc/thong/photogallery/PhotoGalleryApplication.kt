package utc.thong.photogallery

import android.app.*
import android.os.Build
import androidx.core.content.getSystemService

const val NOTIFICATION_CHANNEL_ID = "flickr_poll"

class PhotoGalleryApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )

            val notificationManager: NotificationManager? = getSystemService<NotificationManager>()
            notificationManager?.createNotificationChannel(channel)
        }
    }
}