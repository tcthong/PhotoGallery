package utc.thong.photogallery

import android.net.Uri
import com.google.gson.annotations.SerializedName

data class GalleryItem(
    val id: String = "",
    val title: String = "",
    @SerializedName("url_s") val url: String = "",
    val owner: String = ""
) {
    fun getPageUri(): Uri = Uri.parse("https://www.flickr.com/photos/")
        .buildUpon()
        .appendPath(owner)
        .appendPath(id)
        .build()
}