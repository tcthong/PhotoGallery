package utc.thong.photogallery.api

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import utc.thong.photogallery.GalleryItem
import java.lang.Exception

private const val TAG = "FlickrFetch"

class FlickrFetch {
    private val flickrApi: FlickrApi
    private lateinit var fetchPhotoRequest: Call<FlickrResponse>
    private lateinit var searchPhotoRequest: Call<FlickrResponse>

    init {
        val gson: Gson = GsonBuilder().create()
        val client = OkHttpClient.Builder()
            .addInterceptor(PhotoInterceptor())
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.flickr.com/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(client)
            .build()

        flickrApi = retrofit.create(FlickrApi::class.java)
    }

    fun fetchPhotosRequest(): Call<FlickrResponse> = flickrApi.fetchPhotos()

    fun fetchPhotos(): LiveData<List<GalleryItem>> {
        fetchPhotoRequest = fetchPhotosRequest()
        return fetchPhotoMetadata(fetchPhotoRequest)
    }

    fun searchPhotosRequest(query: String): Call<FlickrResponse> = flickrApi.searchPhotos(query)

    fun searchPhotos(query: String): LiveData<List<GalleryItem>> {
        searchPhotoRequest = searchPhotosRequest(query)
        return fetchPhotoMetadata(searchPhotoRequest)
    }

    private fun fetchPhotoMetadata(request: Call<FlickrResponse>): LiveData<List<GalleryItem>> {
        val responseLiveData = MutableLiveData<List<GalleryItem>>()

        request.enqueue(object : Callback<FlickrResponse> {
            override fun onResponse(
                call: Call<FlickrResponse>,
                response: Response<FlickrResponse>
            ) {
                if (response.isSuccessful) {
                    val flickrResponse: FlickrResponse? = response.body()
                    val photoResponse: PhotoResponse? = flickrResponse?.photoResponse
                    var galleryItems: List<GalleryItem> =
                        photoResponse?.galleryItems ?: listOf()
                    galleryItems = galleryItems.filter {
                        it.url.isNotBlank()
                    }

                    Log.d(TAG, "Response received: $galleryItems")
                    responseLiveData.value = galleryItems
                }
            }

            override fun onFailure(call: Call<FlickrResponse>, t: Throwable) {
                if (!call.isCanceled) {
                    Log.d(TAG, "Failed to fetch photos", t)
                    call.cancel()
                } else {
                    Log.d(TAG, "Indeed cancel")
                }
            }
        })

        return responseLiveData
    }

    @WorkerThread
    fun fetchPhoto(url: String): Bitmap? {
        val response: Response<ResponseBody>? = try {
            flickrApi.fetchUrlBytes(url).execute()
        } catch (e: Exception) {
            Log.d(TAG, e.toString())
            null
        }

        val bitmap = response?.body()?.byteStream().use(BitmapFactory::decodeStream)

        Log.i(TAG, "Decoded bitmap=$bitmap from response=$response")
        return bitmap
    }

    fun cancelRequestInFight() {
        if (::fetchPhotoRequest.isInitialized) {
            fetchPhotoRequest.cancel()
        }

        if (::searchPhotoRequest.isInitialized) {
            searchPhotoRequest.cancel()
        }
    }
}