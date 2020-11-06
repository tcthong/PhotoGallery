package utc.thong.photogallery

import android.content.Context
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.preference.PreferenceManager

object QueryPreferences {
    private const val PREF_SEARCH_QUERY = "searchQuery"
    private const val PREF_LAST_RESULT_ID = "lastResultId"
    private const val PREF_IS_POLLING = "isPolling"

    fun isPolling(context: Context): Boolean = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        .getBoolean(PREF_IS_POLLING, false)

    fun setPolling(context: Context, isPolling: Boolean) = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        .edit {
            putBoolean(PREF_IS_POLLING, isPolling)
        }

    fun getLastResultId(context: Context): String = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        .getString(PREF_LAST_RESULT_ID, "")!!

    fun setLastResultId(context: Context, id: String) {
        PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
            .edit {
                putString(PREF_LAST_RESULT_ID, id)
            }
    }

    fun getStoredQuery(context: Context): String = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        .getString(PREF_SEARCH_QUERY, "")!!

    fun setStoredQuery(context: Context, query: String) {
        PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
            .edit {
                putString(PREF_SEARCH_QUERY, query)
            }
    }
}