package utc.thong.photogallery.api

import com.google.gson.annotations.SerializedName
import utc.thong.photogallery.GalleryItem

class PhotoResponse {
    @SerializedName("photo")
    lateinit var galleryItems: List<GalleryItem>
}