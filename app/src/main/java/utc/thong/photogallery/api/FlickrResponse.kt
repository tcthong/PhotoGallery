package utc.thong.photogallery.api

import com.google.gson.annotations.SerializedName

class FlickrResponse {
    @SerializedName("photos")
    lateinit var photoResponse: PhotoResponse
}