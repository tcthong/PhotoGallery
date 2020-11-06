package utc.thong.photogallery

import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import androidx.collection.LruCache
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import utc.thong.photogallery.api.FlickrFetch
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "ThumbnailDownloader"
private const val MESSAGE_DOWNLOAD = 0
private const val MAX_CACHE_SIZE = 50

class ThumbnailDownloader<in T>(
    private val responseHandler: Handler,
    private val onDownLoadedListener: (T, Bitmap) -> Unit
) : HandlerThread(TAG) {

    private val requestMap = ConcurrentHashMap<T, String>()
    private val flickrFetch = FlickrFetch()
    private var hasQuit = false
    private lateinit var requestHandler: Handler
    private val lruCache: LruCache<String, Bitmap> = LruCache(MAX_CACHE_SIZE)
    val fragmentLifecycleObserver = object : LifecycleObserver {
        //A couple of safety notes.
        // -----One: Notice that you access looper after calling start() on your
        //ThumbnailDownloader. This is a way to ensure
        //that the thread’s guts are ready before proceeding, to obviate a potential (though rarely occurring) race
        //condition. Until you first access looper, there is no guarantee that onLooperPrepared() has been
        //called, so there is a possibility that calls to queueThumbnail(…) will fail due to a null Handler.
        //Tại sao chúng ta không gọi luôn onLooperPrepared() mà phải gọi getLooper() vì nếu chúng ta gọi luôn onLooperPrepared() thì sau
        //đó Handler Thread sẽ tiếp tục gọi phương thức đó một lần nữa trước khi Looper lặp -> sẽ được gọi 2 lần -> thừa
        //còn việc gọi getLooper() thì sẽ đảm bảo rằng Handler Thread đã gọi onLooperPrepared -> chỉ gọi 1 lần.
        //Ngoài cách gọi getLooper() ta có thể làm như phương thức đang được ghi chú ở dưới phương thức này
        //(tham khảo thêm cách này tại https://blog.nikitaog.me/2014/10/11/android-looper-handler-handlerthread-i/)
        //  -----Safety note number two: You call quit() to terminate the thread. This is critical. If you do not quit
        //your HandlerThreads, they will never die. Like zombies
        @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
        fun setUp() {
            start()
            looper
        }

        //Có thể xây dụng setUp() như dưới thì không cần override onLooperPrepared()
//        fun setUp() {
//            start()
//            prepareHandler()
//        }

        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun tearDown() {
            quit()
        }
    }

//    fun prepareHandler() {
//        requestHandler = object : Handler(looper) {
//            override fun handleMessage(msg: Message) {
//                if (msg.what == MESSAGE_DOWNLOAD) {
//                    val target = msg.obj as T
//                    handleRequest(target)
//                }
//            }
//        }
//    }

    val viewLifecycleObserver = object : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun clearQueue() {
            requestHandler.removeMessages(MESSAGE_DOWNLOAD)
            requestMap.clear()
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun onLooperPrepared() {
        Log.d(TAG, "OnLooperPrepared has called")
        requestHandler =  object : Handler(looper) {
            override fun handleMessage(msg: Message) {
                if (msg.what == MESSAGE_DOWNLOAD) {
                    val target = msg.obj as T
                    handleRequest(target)
                }
            }
        }
    }

    override fun quit(): Boolean {
        hasQuit = true
        return super.quit()
    }

    fun handleRequest(target: T) {
        val url = requestMap[target] ?: return
        val bitmap = flickrFetch.fetchPhoto(url) ?: return
        lruCache.put(url, bitmap)
//        val bitmap = flickrFetch.fetchPhoto(url) ?: return


        //Có thể thay thể responseHandler bằng Handler(Looper.getMainLooper()) nhưng chi phí tốn kém hơn
        //vì mỗi lần có một message được xử lý thì ta phải khởi tạo một đối tượng Handler mới -> tốn kém.
        responseHandler.post {
//            So what does this code do? First, you double-check the requestMap. This is necessary because the
//            RecyclerView recycles its views. By the time ThumbnailDownloader finishes downloading the Bitmap,
//            RecyclerView may have recycled the PhotoHolder and requested a different URL for it. This check
//            ensures that each PhotoHolder gets the correct image, even if another request has been made in the
//            meantime
            if (requestMap[target] != url || hasQuit) {
                return@post
            }

            requestMap.remove(target)
            onDownLoadedListener(target, bitmap)
        }
    }

    fun queueThumbnail(target: T, url: String) {
        val bitmapCache = lruCache.get(url)
        if (bitmapCache != null) {
            onDownLoadedListener(target, bitmapCache)
        } else {
            requestMap[target] = url
            requestHandler.obtainMessage(MESSAGE_DOWNLOAD, target).sendToTarget()
        }

    }
}