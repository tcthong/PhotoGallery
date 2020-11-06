package utc.thong.photogallery

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import utc.thong.photogallery.api.FlickrFetch

class PhotoGalleryViewModel(private val app: Application) : AndroidViewModel(app) {
    private val flickrFetch: FlickrFetch = FlickrFetch()
    private val queryLiveData = MutableLiveData<String>()
    val galleryItemsLiveData: LiveData<List<GalleryItem>>
    val searchQuery: String
        get() = queryLiveData.value ?: ""

    init {
        galleryItemsLiveData = Transformations.switchMap(queryLiveData) { query ->
            if (query.isBlank()) {
                flickrFetch.fetchPhotos()
            } else {
                flickrFetch.searchPhotos(query)
            }
        }

        queryLiveData.value = QueryPreferences.getStoredQuery(app)
    }

    fun fetchPhotos(query: String = "") {
        queryLiveData.value = query
        QueryPreferences.setStoredQuery(app, query)
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("Flickr", "ViewModel be about to destroy")
        flickrFetch.cancelRequestInFight()
    }
}