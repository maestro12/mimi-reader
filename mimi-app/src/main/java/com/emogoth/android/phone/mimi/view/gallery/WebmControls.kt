package com.emogoth.android.phone.mimi.view.gallery

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.widget.FrameLayout
import android.widget.SeekBar
import androidx.annotation.DrawableRes
import androidx.core.content.res.ResourcesCompat
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.emogoth.android.phone.mimi.R
import com.emogoth.android.phone.mimi.util.MediaInfo
import com.emogoth.android.phone.mimi.util.MediaUtil
import com.emogoth.android.phone.mimi.util.MimiUtil
import com.emogoth.android.phone.mimi.util.Utils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.view_gallery_webm_bar.view.*
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

class WebmControls @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        val LOG_TAG = WebmControls::class.java.simpleName

        const val AUDIO_DISABLED = 0
        const val AUDIO_ENABLED = 1
        const val AUDIO_LOCKED = 2
    }

    private var audioLocked = false
    private var muted: Boolean = true

    private var duration = -1L
        set(value) {
            field = value
            webm_scrubber.max = value.toInt()
            webm_scrubber.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        scrubberListener?.invoke(progress.toLong())
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    // no op
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    // no op
                }
            })
        }
    var progress = 0L
        set(value) {
            field = value
            if (duration < 0) {
                return
            }

            if (value > 0L) {
                val display = String.format(Locale.getDefault(), "%02d:%02d / %02d:%02d",
                        TimeUnit.MINUTES.convert(value, TimeUnit.MILLISECONDS),
                        TimeUnit.MILLISECONDS.toSeconds(value) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(value)),
                        TimeUnit.MINUTES.convert(duration, TimeUnit.MILLISECONDS),
                        TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration)))

                webm_time.text = display
                webm_scrubber.progress = value.toInt()
            }
        }
    var paused = false
        set(value) {
            field = value
            if (value) {
                webm_play_button.setImageResource(R.drawable.ic_play_arrow)
            } else {
                webm_play_button.setImageResource(R.drawable.ic_pause)
            }
        }
    private var videoInfo: Disposable? = null
    var videoLocation: String = ""
        set(value) {
            field = value
            if (value == "") {
                return
            }

            val file = File(value)
            if (!file.exists()) {
                field = ""
                return
            }

            videoInfo = MediaUtil.videoInfo(value)
                    .onErrorReturn {
                        Log.e(LOG_TAG, "Error fetching media info for $value: exists:${file.exists()}", it)
                        MediaInfo.empty()
                    }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { info ->
                        duration = info.duration
                        if (duration > 0) {
                            setup(info.hasAudio)
                        }
                    }
        }
    private var hasAudio = false

    init {
        inflate(context, R.layout.view_gallery_webm_bar, this)
        initOpenButton()
    }

    private fun initOpenButton() {
        open_button.setOnClickListener {
            if (videoLocation != "") {
                val ext = videoLocation.substring(videoLocation.lastIndexOf(".") + 1)
                val fileUri = MimiUtil.getFileProvider(File(videoLocation))
                if (fileUri != null) {
                    val intent = Intent(Intent.ACTION_VIEW, fileUri)
                    intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    intent.setDataAndType(fileUri, Utils.getMimeType(ext))

                    if (context is Activity) {
                        val act = context as Activity
                        act.startActivity(intent)
                    }
                }
            }
        }
    }

    fun setAudioLock(lock: Boolean, fromUser: Boolean) {
        if (audioLocked == lock) {
            return
        }

        audioLocked = lock

        if (fromUser) {
            audioLockListener?.invoke(lock)
        }
    }

    fun setMuted(muted: Boolean, fromUser: Boolean) {
        this.muted = muted
        if (!hasAudio) {
            mute_button.setImageDrawable(audioDrawable(R.drawable.ic_audio_off, AUDIO_DISABLED))
        } else if (muted) {
            mute_button.setImageDrawable(audioDrawable(R.drawable.ic_audio_off, AUDIO_ENABLED))
        } else if (audioLocked) {
            mute_button.setImageDrawable(audioDrawable(R.drawable.ic_audio_on, AUDIO_LOCKED))
        } else if (!audioLocked) {
            mute_button.setImageDrawable(audioDrawable(R.drawable.ic_audio_on, AUDIO_ENABLED))
        }

        if (fromUser) {
            muteListener?.invoke(muted)
        }
    }

    fun isMuted(): Boolean {
        return this.muted
    }

    @SuppressLint("SetTextI18n")
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        webm_time.text = "00:00 / 00:00"
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        videoInfo?.dispose()

        release()
    }

    private fun audioDrawable(@DrawableRes res: Int, setting: Int): Drawable? {
        val drawable = VectorDrawableCompat.create(context.resources, res, context.theme)
        val color = when(setting) {
            AUDIO_DISABLED ->
                ResourcesCompat.getColor(context.resources, R.color.md_grey_700, context.theme)
            AUDIO_ENABLED ->
                ResourcesCompat.getColor(context.resources, R.color.md_white_1000, context.theme)
            AUDIO_LOCKED ->
                ResourcesCompat.getColor(context.resources, R.color.md_green_400, context.theme)
            else -> ResourcesCompat.getColor(context.resources, R.color.md_grey_700, context.theme)

        }

        drawable?.mutate()?.setTint(color)
        return drawable
    }

    private fun setup(hasAudio: Boolean) {
        this.hasAudio = hasAudio
        if (hasAudio) {
            val m = !audioLocked
            setMuted(m, false)

            mute_button.setOnClickListener {
                if (audioLocked) {
                    setAudioLock(lock = false, fromUser = true)
                }
                setMuted(!muted, true)
            }

            mute_button.setOnLongClickListener {
                setAudioLock(lock = true, fromUser = true)
                setMuted(muted = false, fromUser = true)

                true
            }

            webm_play_button.setOnClickListener {
                paused = !paused
                playListener?.invoke(paused)
            }
        } else {
            setMuted(muted = true, fromUser = true)
            mute_button.setOnLongClickListener(null)
            mute_button.setOnClickListener(null)
//            release()
        }
    }

    fun release() {
//        mute_button.setOnLongClickListener(null)
//        mute_button.setOnClickListener(null)
//        webm_play_button.setOnClickListener(null)
//
//        if (!audioLocked) {
//            muted = true
//        }
//
//        hasAudio = false
    }

    var muteListener: ((Boolean) -> Unit)? = null
    var audioLockListener: ((Boolean) -> Unit)? = null
    var playListener: ((Boolean) -> Unit)? = null
    var scrubberListener: ((Long) -> Unit)? = null
}