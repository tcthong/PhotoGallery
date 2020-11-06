package utc.thong.photogallery

import android.app.Activity
import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat

private const val TAG = "NotificationReceiver"

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (resultCode == Activity.RESULT_CANCELED) {
            return
        }

        val requestCode = intent?.getIntExtra(PollWorker.KEY_REQUEST_CODE, 0) ?: return
        val notification = intent?.getParcelableExtra<Notification>(PollWorker.KEY_NOTIFICATION) ?: return
        context?.let {
            NotificationManagerCompat.from(it).notify(requestCode, notification)
        }
    }
}