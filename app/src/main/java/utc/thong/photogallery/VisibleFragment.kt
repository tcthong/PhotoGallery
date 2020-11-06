package utc.thong.photogallery

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.fragment.app.Fragment

private const val TAG = "VisibleFragment"

abstract class VisibleFragment : Fragment() {
    private val onShowNotification = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // If we receive this, we're visible, so cancel the notification
            Log.i(TAG, "Canceling notification")
            resultCode = Activity.RESULT_CANCELED
        }
    }

    override fun onStart() {
        super.onStart()
        requireActivity().registerReceiver(
            onShowNotification,
            IntentFilter(PollWorker.ACTION_SHOW_NOTIFICATION),
            PollWorker.PERMISSION_PRIVATE,
            null
        )
    }

    override fun onStop() {
        super.onStop()
        requireActivity().unregisterReceiver(onShowNotification)
    }
}