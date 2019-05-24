package com.emogoth.android.phone.mimi.view.gallery

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.SurfaceTexture
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.net.toUri
import com.emogoth.android.phone.mimi.R
import com.emogoth.android.phone.mimi.db.DatabaseUtils
import com.emogoth.android.phone.mimi.util.ExoPlayer2Helper
import com.emogoth.android.phone.mimi.util.GlideApp
import com.emogoth.android.phone.mimi.viewmodel.GalleryViewModel
import io.reactivex.Flowable
import io.reactivex.disposables.Disposable
import java.util.concurrent.TimeUnit

@SuppressLint("ViewConstructor")
class WebmPage(context: Context, private val viewModel: GalleryViewModel, private val player: ExoPlayer2Helper?) : GalleryPage(context, viewModel), ExoPlayer2Helper.Listener {
    private val videoView = TextureView(context)
    private var controlView: WebmControls
    private var preview: AppCompatImageView

    init {
        addMainChildView(videoView)

        videoView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
                Log.d(LOG_TAG, "Surface texture changed (width: $width, height: $height)")
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
                Log.d(LOG_TAG, "Surface texture updated")
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
                Log.d(LOG_TAG, "Surface texture destroyed")
                return true
            }

            override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
                Log.d(LOG_TAG, "Surface texture available")
            }
        }

        controlView = WebmControls(context)
        controlView.setAudioLock(viewModel.audioLock, false)
        controlView.audioLockListener = { locked ->
            viewModel.audioLock = locked
        }
        controlView.muteListener = { muted ->
            player?.mute(muted)
        }
        controlView.playListener = { paused ->
            if (paused) {
                player?.pause()
            } else {
                player?.start()
            }
        }
        controlView.scrubberListener = { value ->
            player?.seekTo(value)
        }
        addView(controlView)

        videoView.setOnClickListener {
            controlView.visibility = if (controlView.visibility == View.VISIBLE) View.INVISIBLE else View.VISIBLE
        }

        preview = AppCompatImageView(context)
        addView(preview)

    }

    override fun onComplete() {
        super.onComplete()
        if (!isAttachedToWindow) {
            return
        }

        controlView.videoLocation = downloadItem.file.absolutePath
        updateVideoPlaybackState()
        loaded = true
    }

    override fun onPageSelectedChange(selected: Boolean) {
        super.onPageSelectedChange(selected)
        val m = !viewModel.audioLock
        controlView.setAudioLock(lock = viewModel.audioLock, fromUser = false)
        controlView.setMuted(muted = m, fromUser = false)
        updateVideoPlaybackState()
    }

    private fun updateVideoPlaybackState() {
        if (downloadComplete && pageSelected) {
            controlView.setAudioLock(viewModel.audioLock, false)

            showVideoView()
            scaleView(videoView, downloadItem.width, downloadItem.height)

            player?.addListener(this)
            player?.setTextureView(videoView)
            player?.initVideo(downloadItem.file.toUri())
            player?.mute(controlView.isMuted())
            player?.start()
        } else if (downloadComplete && !pageSelected) {
            GlideApp.with(preview).clear(preview)

            player?.removeListener(this)
            showPreviewView()
        }
    }

    private fun scaleView(view: View, videoWidth: Int, videoHeight: Int) {
        val ratio = scaleToFit(width, height, videoWidth, videoHeight)

        val params = view.layoutParams
        params.width = (videoWidth * ratio).toInt()
        params.height = (videoHeight * ratio).toInt()
        if (params is FrameLayout.LayoutParams) {
            params.gravity = Gravity.CENTER
        }
        view.layoutParams = params
    }

    private fun scaleToFit(displayWidth: Int, displayHeight: Int, videoWidth: Int, videoHeight: Int): Float {
        val widthRatio = displayWidth.toFloat() / videoWidth.toFloat()
        val heightRatio = displayHeight.toFloat() / videoHeight.toFloat()
        return if (widthRatio < heightRatio) widthRatio else heightRatio
    }

    private fun showPreviewView(loadImage: Boolean = true) {
        videoView.visibility = View.INVISIBLE
        preview.visibility = View.VISIBLE

        if (loadImage) {
            try {
                preview.setImageDrawable(null)
                GlideApp.with(preview)
                        .load(downloadItem.thumbUrl)
                        .error(R.drawable.ic_content_picture)
                        .into(preview)
            } catch (e: IllegalStateException) {
                // no op
            }
        }
    }

    private fun showVideoView() {
        videoView.visibility = View.VISIBLE
        preview.visibility = View.GONE
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (context is Activity) {
            val act: Activity = context as Activity
            act.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        controlView.setAudioLock(viewModel.audioLock, false)
        startTimer()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        videoTimerSubscription?.dispose()
        showPreviewView(loadImage = false)
    }

    private var videoTimerSubscription: Disposable? = null

    private fun startTimer() {
        videoTimerSubscription = Flowable.interval(250, TimeUnit.MILLISECONDS).timeInterval()
                .compose(DatabaseUtils.applySchedulers())
                .subscribe({
                    if (player != null && player.isPlaying) {
                        controlView.progress = player.currentPosition
                    }
                }, { throwable -> Log.e(LOG_TAG, "Timer error", throwable) })
    }

    override fun fullScreen(enabled: Boolean) {
        super.fullScreen(enabled)
        scaleView(videoView, downloadItem.width, downloadItem.height)
    }

    override fun onViewBind() {
        Log.d(LOG_TAG, "width=${downloadItem.width}, height=${downloadItem.height}")
    }

    override fun onStateChanged(playWhenReady: Boolean, playbackState: Int) {
        Log.d(LOG_TAG, "State changed (playWhenReady: $playWhenReady, state: $playbackState)")
    }

    override fun onError(e: Exception?) {
        Log.e(LOG_TAG, "Error playing file", e)
    }

    override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
        Log.d(LOG_TAG, "Video size changed (width: $width, height: $height)")
        scaleView(videoView, width, height)
    }

    override fun onDrawnToSurface(surface: Surface?) {
        // no op
    }

    override fun onRenderedFirstFrame(surface: Surface?) {
        Log.d(LOG_TAG, "First frame of video rendered")
        videoView.post { showVideoView() }
    }
}