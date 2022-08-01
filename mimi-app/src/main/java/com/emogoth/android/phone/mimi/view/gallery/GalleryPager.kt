package com.emogoth.android.phone.mimi.view.gallery

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.emogoth.android.phone.mimi.R
import com.emogoth.android.phone.mimi.util.*
import com.emogoth.android.phone.mimi.viewmodel.GalleryItem
import com.emogoth.android.phone.mimi.viewmodel.GalleryViewModel

import com.mimireader.chanlib.models.ChanPost
import com.mimireader.chanlib.models.ChanThread
import kotlinx.android.synthetic.main.view_gallery_pager.view.*
import java.io.File
import java.util.*

class GalleryPager @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), GalleryView, LifecycleObserver {
    companion object {
        val LOG_TAG: String = GalleryPager::class.java.simpleName
        const val AD_SPACING = 9
        const val AD_POSITION = -1
    }

    private var adapter: GalleryPagerAdapter? = null
    private var player: ExoPlayer2Helper? = null

    private val layoutManager: RecyclerView.LayoutManager

    private var actionBar: ActionBar? = null

    private var pagerPosition: Int = 0
    var position: Int
        get() {
            return pagerPosition
        }
        set(value) {
            try {
                pager.post { pager.layoutManager?.scrollToPosition(value) }
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Error getting gallery position", e)
            }
        }
    var postId: Long = -1
        get() {
            val pagerHolder = pager.findViewHolderForAdapterPosition(position)
            if (pagerHolder != null) {
                val holder: GalleryPagerItemViewHolder = pagerHolder as GalleryPagerItemViewHolder
                return holder.postId
            }

            return -1
        }

    private var galleryViewModel: GalleryViewModel? = null

    var pageChangeCallback: ((Int) -> (Unit))? = null
    var pageClickCallback: ((View) -> (Unit))? = null

    init {
        id = R.id.gallery2_pager
        Log.d(LOG_TAG, "Instance of GalleryPager created")
        player = ExoPlayer2Helper(context)
        layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        inflateView()

        pager.layoutManager = layoutManager
        pager.attachSnapHelperWithListener(PagerSnapHelper(), SnapOnScrollListener.Behavior.NOTIFY_ON_SCROLL, object : OnSnapPositionChangeListener {
            override fun onSnapPositionChange(previous: Int, current: Int) {
                pagerPosition = current

                Log.d(LOG_TAG, "Gallery Position: $current (previous=$previous)")
                if (player?.isPlaying == true) {
                    player?.pause()
                }
                if (previous >= 0) {
                    try {
                        val previousViewHolder = pager.findViewHolderForAdapterPosition(previous)
                        if (previousViewHolder is GalleryPagerItemViewHolder) {
                            previousViewHolder.onSelectionChange(false)
                        }
                    } catch (e: Exception) {
                        // no op
                    }
                }

                try {
                    val currentViewHolder = pager.findViewHolderForAdapterPosition(current)
                    if (currentViewHolder is GalleryPagerItemViewHolder) {
                        currentViewHolder.onSelectionChange(true)
                    }
                } catch (e: Exception) {
                    // no op
                }

                val items = getItems()
                val count = items.size
                if (count > current) {
                    val subtitle: String
                    val pos = current

                    val item = items[pos]
                    val fileName = item.originalFileName
                    file_name.text = fileName

                    val fileSize = MimiUtil.humanReadableByteCount(item.size.toLong(), true)
                    file_size.text = fileSize

                    pageChangeCallback?.invoke(pos)
                    subtitle = "${pos + 1} / ${items.size}"

                    grid_button.visibility = View.VISIBLE

                    actionBar?.subtitle = subtitle
                }
            }
        })

        grid_button.setOnClickListener {
            gridButtonListener?.invoke(it)
        }

        exit_fullscreen_button.setOnClickListener {
            fullScreen(false)
            fullscreenListener?.invoke(false)
        }
    }

    fun setInitialPosition(itemPosition: Int) {
        position = itemPosition
    }

    @OnLifecycleEvent(value = Lifecycle.Event.ON_PAUSE)
    fun pause() {
        player?.pause()
//        release()
//        player = null
    }

    @OnLifecycleEvent(value = Lifecycle.Event.ON_RESUME)
    fun resume() {
        player?.start()
    }

    @OnLifecycleEvent(value = Lifecycle.Event.ON_DESTROY)
    fun release() {
        adapter?.destroy()
        player?.release()
        player = null
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (player == null) {
            player = ExoPlayer2Helper(context)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        release()
        player = null
    }

    private fun inflateView() {
        inflate(context, R.layout.view_gallery_pager, this)
    }

    override fun setViewModel(viewModel: GalleryViewModel) {
        galleryViewModel = viewModel
    }

    override fun setItems(items: List<GalleryItem>) {
        if (galleryViewModel == null) {
            throw IllegalStateException("Viewmodel must be set before setting the gallery items")
        }

        val viewModel: GalleryViewModel = galleryViewModel as GalleryViewModel
        if (adapter == null) {
            adapter = GalleryPagerAdapter(items, viewModel, player)
            pager.adapter = adapter

            actionBar = if (this.context is AppCompatActivity) {
                (this.context as AppCompatActivity).supportActionBar
            } else {
                null
            }
        }
    }

    fun getItems(): List<GalleryItem> {
        return adapter?.items ?: Collections.emptyList()
    }

    var gridButtonListener: ((View) -> Unit)? = null

    private var fullscreenListener: ((Boolean) -> Unit)? = null
    override fun fullScreenListener(listener: (Boolean) -> Unit) {
        this.fullscreenListener = listener
    }

    fun getLocalPathForPosition(position: Int): File {
        val holder: GalleryPagerItemViewHolder = pager.findViewHolderForAdapterPosition(position) as GalleryPagerItemViewHolder
        return holder.localPath
    }

    override fun fullScreen(enabled: Boolean) {
        if (enabled) {
            gallery_toolbar.visibility = View.GONE
            exit_fullscreen_button.visibility = View.VISIBLE
        } else {
            gallery_toolbar.visibility = View.VISIBLE
            exit_fullscreen_button.visibility = View.GONE
        }

        val currentViewHolder = pager.findViewHolderForAdapterPosition(pagerPosition)
        if (currentViewHolder is GalleryPagerItemViewHolder) {
            exit_fullscreen_button.post { currentViewHolder.fullScreen(enabled) }
        }
    }

    override fun shareImage() {
        val activity: Activity
        val c = context
        if (c is Activity) {
            activity = c
        } else {
            return
        }

        val item = adapter?.getGalleryItem(pagerPosition) ?: GalleryItem.empty()
        if (item.id <= 0) {
            return
        }

        val holder: GalleryPagerItemViewHolder = pager.findViewHolderForAdapterPosition(pagerPosition) as GalleryPagerItemViewHolder
        val menuView = activity.findViewById<View>(R.id.share_menu)
        val popupMenu = PopupMenu(activity, menuView)
        popupMenu.inflate(R.menu.share_popup)
        popupMenu.setOnMenuItemClickListener { item1 ->
            if (context == null) {
                return@setOnMenuItemClickListener false
            }

            val shareIntent = Intent()
            if (item1.itemId == R.id.share_link) {
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.putExtra(Intent.EXTRA_TEXT, item.downloadUrl)
                shareIntent.type = "text/plain"
            } else {
                val shareFile = holder.localPath

                if (shareFile.exists()) {
                    val uri = MimiUtil.getFileProvider(shareFile)
                    if (uri == null) {
                        Toast.makeText(context, R.string.error_while_sharing_file, Toast.LENGTH_SHORT).show()
                        return@setOnMenuItemClickListener true
                    }

                    val type: String
                    if (shareFile.name.endsWith(".webm")) {
                        type = "video/webm"
                    } else {
                        type = "image/*"
                    }

                    shareIntent.action = Intent.ACTION_SEND
                    shareIntent.setDataAndType(uri, type)
                    shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                }
            }

            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share)))
            true
        }

        popupMenu.show()
    }

}

class GalleryPagerAdapter(val items: List<GalleryItem>, val viewModel: GalleryViewModel, private val player: ExoPlayer2Helper?) : RecyclerView.Adapter<GalleryPagerItemViewHolder>() {
    enum class ItemType(val value: Int) {
        UNKNOWN(0), IMAGE(1), GIF(2), WEBM(3), PDF(4);
    }

    companion object {
        @JvmStatic
        fun getPostsWithImages(thread: ChanThread): ArrayList<ChanPost> {
            return getPostsWithImages(thread.posts)
        }

        @JvmStatic
        fun getPostsWithImages(postList: List<ChanPost>): ArrayList<ChanPost> {
            val posts = ArrayList<ChanPost>()
            for (post in postList) {
                if (!TextUtils.isEmpty(post.filename)) {
                    posts.add(post)
                }
            }

            return posts
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryPagerItemViewHolder {
        return when (viewType) {
            ItemType.IMAGE.value -> GalleryImageViewHolder(ImagePage(parent.context, viewModel))
            ItemType.GIF.value -> GalleryGifViewHolder(GifPage(parent.context, viewModel))
            ItemType.WEBM.value -> GalleryWebmViewHolder(WebmPage(parent.context, viewModel, player))
            ItemType.PDF.value -> GalleryPdfViewHolder(PdfPage(parent.context, viewModel))
            else -> GalleryImageViewHolder(ImagePage(parent.context, viewModel))
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun getGalleryItem(position: Int): GalleryItem {
        return items[position]
    }

    override fun getItemViewType(position: Int): Int {
        if (position >= items.size) {
            Log.e("GalleryPager.Adapter", "Item position=$position, Original position=$position")
        }
        return when (items[position].ext) {
            ".gif" -> ItemType.GIF.value
            ".webm" -> ItemType.WEBM.value
            ".pdf" -> ItemType.PDF.value
            else -> ItemType.IMAGE.value
        }
    }

    override fun onBindViewHolder(holder: GalleryPagerItemViewHolder, position: Int) {
        holder.bind(if (position >= 0) items[position] else GalleryItem.empty())
    }

    fun destroy() {
        // no op
    }

}

abstract class GalleryPagerItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun bind(item: GalleryItem) {
        if (itemView is GalleryPage) {
            itemView.bind(item)
        }
    }

    fun onSelectionChange(selected: Boolean) {
        if (itemView is GalleryPage) {
            itemView.onPageSelectedChange(selected)
        }
    }

    fun fullScreen(enabled: Boolean = true) {
        if (itemView is GalleryPage) {
            itemView.fullScreen(enabled)
        }
    }

    val postId: Long
        get() {
            if (itemView is GalleryPage) {
                return itemView.postId
            }

            return -1
        }

    val localPath: File
        get() {
            if (itemView is GalleryPage) {
                return itemView.localFile
            }

            return File("")
        }
}

// These need to be different classes to prevent the RecyclerView from reusing the wrong viewholder
class GalleryImageViewHolder(itemView: View) : GalleryPagerItemViewHolder(itemView)
class GalleryWebmViewHolder(itemView: View) : GalleryPagerItemViewHolder(itemView)
class GalleryGifViewHolder(itemView: View) : GalleryPagerItemViewHolder(itemView)
class GalleryPdfViewHolder(itemView: View) : GalleryPagerItemViewHolder(itemView)
