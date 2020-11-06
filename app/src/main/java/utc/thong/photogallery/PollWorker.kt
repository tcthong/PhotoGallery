package utc.thong.photogallery

import android.app.Activity
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import utc.thong.photogallery.api.FlickrFetch
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.logging.Handler

private const val TAG = "PollWorker"

class PollWorker(private val context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    override fun doWork(): Result {
        val query = QueryPreferences.getStoredQuery(context)
        val flickrFetch = FlickrFetch()

        val galleryItems: List<GalleryItem> = if (query.isBlank()) {
            flickrFetch.fetchPhotosRequest()
                .execute()
                .body()
                ?.photoResponse
                ?.galleryItems
        } else {
            flickrFetch.searchPhotosRequest(query)
                .execute()
                .body()
                ?.photoResponse
                ?.galleryItems
        } ?: emptyList()

        if (galleryItems.isEmpty()) {
            return Result.success()
        }

        val lastResultId = QueryPreferences.getLastResultId(context)
        val currentId = galleryItems.first().id
        if (lastResultId == currentId) {
            Log.d(TAG, "Old data + last id = $currentId")
        } else {
            Log.d(TAG, "Has new data + id = $currentId")
            QueryPreferences.setLastResultId(context, currentId)

            val intent = MainActivity.newIntent(context)
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)

            val resources = context.resources
            val notification: Notification =
                NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                    .setTicker(resources.getString(R.string.new_pictures_title))
                    .setContentTitle(resources.getString(R.string.new_pictures_title))
                    .setContentText(resources.getString(R.string.new_pictures_text))
                    .setSmallIcon(android.R.drawable.ic_menu_report_image)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build()

            showBackgroundNotification(0, notification)
        }

        return Result.success()
    }

    private fun showBackgroundNotification(
        requestCode: Int,
        notification: Notification
    ) {
        val intent: Intent = Intent(ACTION_SHOW_NOTIFICATION).apply {
            putExtra(KEY_REQUEST_CODE, requestCode)
            putExtra(KEY_NOTIFICATION, notification)
        }

        context.sendOrderedBroadcast(intent, PERMISSION_PRIVATE)
    }

    companion object {
        const val ACTION_SHOW_NOTIFICATION = "utc.thong.photogallery.ACTION_SHOW_NOTIFICATION"
        const val PERMISSION_PRIVATE = "utc.thong.photogallery.PRIVATE"
        const val KEY_REQUEST_CODE = "keyRequestCode"
        const val KEY_NOTIFICATION = "keyNotification"
    }
}