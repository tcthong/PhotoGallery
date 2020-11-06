package utc.thong.photogallery

import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.*
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

private const val TAG = "PhotoGalleryFragment"
private const val SPAN_WIDTH_IN_DIPS = 140f
private const val INTERVAL_WORKER_IN_MINUTES = 15L
private const val POLL_WORKER = "poll_worker"

class PhotoGalleryFragment : VisibleFragment() {
    private lateinit var photoRecyclerView: RecyclerView
    private lateinit var photoGalleryViewModel: PhotoGalleryViewModel
    private lateinit var thumbnailDownloader: ThumbnailDownloader<PhotoViewHolder>
    private lateinit var photoProgressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        retainInstance = true
        setHasOptionsMenu(true)
        photoGalleryViewModel = ViewModelProvider(this).get(PhotoGalleryViewModel::class.java)

        thumbnailDownloader = ThumbnailDownloader(Handler(Looper.getMainLooper())) { photoViewHolder, bitmap ->
            val drawable = BitmapDrawable(resources, bitmap)
            photoViewHolder.bindImage(drawable)
        }

        lifecycle.addObserver(thumbnailDownloader.fragmentLifecycleObserver)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_photo_gallery, container, false)
        photoRecyclerView = view.findViewById(R.id.photo_recycler_view)
        photoProgressBar = view.findViewById(R.id.photo_progress_bar)
        hideProgressBar()

        photoRecyclerView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                photoRecyclerView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val colWidthInPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, SPAN_WIDTH_IN_DIPS, requireActivity().resources.displayMetrics)
                val colCount = (photoRecyclerView.width / colWidthInPixels).roundToInt()
                photoRecyclerView.layoutManager = GridLayoutManager(context, colCount)
            }
        })
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycle.addObserver(thumbnailDownloader.viewLifecycleObserver)

        photoGalleryViewModel.galleryItemsLiveData.observe(viewLifecycleOwner) { galleryItems ->
            Log.d(TAG, "Have gallery items from ViewModel $galleryItems")
            photoRecyclerView.adapter = PhotoAdapter(galleryItems)

            hideProgressBar()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewLifecycleOwner.lifecycle.removeObserver(thumbnailDownloader.viewLifecycleObserver)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(thumbnailDownloader.fragmentLifecycleObserver)
    }

    private fun hideProgressBar() {
        photoProgressBar.isIndeterminate = false
        photoProgressBar.visibility = View.GONE

        photoRecyclerView.visibility = View.VISIBLE
    }

    private fun showProgressBar() {
        photoProgressBar.isIndeterminate = true
        photoProgressBar.visibility = View.VISIBLE

        photoRecyclerView.visibility = View.GONE
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_photo_gallery, menu)
        val menuItem: MenuItem = menu.findItem(R.id.menu_item_search)
        val searchView: SearchView = menuItem.actionView as SearchView

        searchView.apply {
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    Log.d(TAG, "QueryTextSubmit: $query")
                    return query?.let {
                        photoGalleryViewModel.fetchPhotos(it)
                        updateUiAfterSubmitSearchQuery(searchView)

                        true
                    } ?: false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    Log.d(TAG, "QueryTextSubmit: $query")
                    return false
                }
            })

            setOnSearchClickListener {
                this.setQuery(photoGalleryViewModel.searchQuery, false)
            }
        }

        val togglePollingItem = menu.findItem(R.id.menu_item_toggle_polling)
        val isPolling = QueryPreferences.isPolling(requireContext())
        togglePollingItem.title = if (isPolling) {
            getString(R.string.stop_polling)
        } else {
            getString(R.string.start_polling)
        }
    }

    private fun updateUiAfterSubmitSearchQuery(searchView: SearchView) {
        //Collapse searchview and hide keyboard
        searchView.onActionViewCollapsed()
        showProgressBar()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_item_clear -> {
                showProgressBar()
                photoGalleryViewModel.fetchPhotos("")
                true
            }
            R.id.menu_item_toggle_polling -> {
                val isPolling = QueryPreferences.isPolling(requireContext())
                QueryPreferences.setPolling(requireContext(), !isPolling)

                if (isPolling) {
                    WorkManager.getInstance(requireContext().applicationContext).cancelUniqueWork(POLL_WORKER)
                } else {
                    val constraints: Constraints = Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.UNMETERED)
                        .build()

                    val workRequest = PeriodicWorkRequestBuilder<PollWorker>(INTERVAL_WORKER_IN_MINUTES, TimeUnit.MINUTES)
                        .setConstraints(constraints)
                        .build()

                    WorkManager.getInstance(requireContext().applicationContext)
                        .enqueueUniquePeriodicWork(
                            POLL_WORKER,
                            ExistingPeriodicWorkPolicy.KEEP,
                            workRequest
                        )
                }
                requireActivity().invalidateOptionsMenu()

                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        fun newInstance(): PhotoGalleryFragment {
            return PhotoGalleryFragment()
        }
    }

    private inner class PhotoAdapter(private val galleryItems: List<GalleryItem>) : RecyclerView.Adapter<PhotoViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val view = inflater.inflate(R.layout.list_item_gallery, parent, false)
            return PhotoViewHolder(view as ImageView)
        }

        override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
            val photo: GalleryItem = galleryItems[position]
            holder.bindGalleryItem(photo)

            val placeholder: Drawable = ContextCompat.getDrawable(
                holder.itemView.context,
                R.drawable.ic_placeholder
            ) ?: ColorDrawable()
            holder.bindImage(placeholder)
            thumbnailDownloader.queueThumbnail(holder, photo.url)
        }

        override fun getItemCount(): Int = galleryItems.size
    }

    private inner class PhotoViewHolder(private val view: ImageView) : RecyclerView.ViewHolder(view), View.OnClickListener {
        private lateinit var galleryItem: GalleryItem
        val bindImage: (Drawable) -> Unit = view::setImageDrawable

        init {
            view.setOnClickListener(this)
        }

        fun bindGalleryItem(item: GalleryItem) {
            this.galleryItem = item
        }

        override fun onClick(v: View?) {
//            startActivity(Intent(Intent.ACTION_VIEW, galleryItem.getPageUri()))
            startActivity(PhotoPageActivity.newIntent(requireContext(), galleryItem.getPageUri()))
        }
    }
}