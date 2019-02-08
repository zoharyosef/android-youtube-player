package com.pierfrancescosoffritti.androidyoutubeplayer.ui.views

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.pierfrancescosoffritti.androidyoutubeplayer.R
import com.pierfrancescosoffritti.androidyoutubeplayer.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.player.listeners.YouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.utils.Utils

class YouTubePlayerSeekBar(context: Context, attrs: AttributeSet? = null): LinearLayout(context, attrs), SeekBar.OnSeekBarChangeListener, YouTubePlayerListener {

    private var seekBarTouchStarted = false
    // I need this variable because onCurrentSecond gets called every 100 mils, so without the proper checks on this variable in onCurrentSeconds the seek bar glitches when touched.
    private var newSeekBarProgress = -1

    private var isPlaying = false

    var showBufferingProgress = true

    var youtubePlayerSeekBarListener: YouTubePlayerSeekBarListener? = null

    val videoCurrentTimeTextView = TextView(context)
    val videoDurationTextView = TextView(context)
    val seekBar = SeekBar(context)

    init {
        val typedArray = context.theme.obtainStyledAttributes(attrs, R.styleable.YouTubePlayerSeekBar, 0, 0)

        val fontSize = typedArray.getDimensionPixelSize(R.styleable.YouTubePlayerSeekBar_fontSize, resources.getDimensionPixelSize(R.dimen.ayp_12sp))
        val color = typedArray.getColor(R.styleable.YouTubePlayerSeekBar_color, ContextCompat.getColor(context, R.color.ayp_red))

        val padding = resources.getDimensionPixelSize(R.dimen.ayp_8dp)

        videoCurrentTimeTextView.text = resources.getString(R.string.ayp_null_time)
        videoCurrentTimeTextView.setPadding(padding, padding, 0, padding)
        videoCurrentTimeTextView.setTextColor(ContextCompat.getColor(context, android.R.color.white))
        videoCurrentTimeTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        videoCurrentTimeTextView.gravity = Gravity.CENTER_VERTICAL

        videoDurationTextView.text = resources.getString(R.string.ayp_null_time)
        videoDurationTextView.setPadding(0, padding, padding, padding)
        videoDurationTextView.setTextColor(ContextCompat.getColor(context, android.R.color.white))
        videoDurationTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize.toFloat())
        videoDurationTextView.gravity = Gravity.CENTER_VERTICAL

        seekBar.setPadding(padding*2, padding, padding*2, padding)
        DrawableCompat.setTint(seekBar.thumb, color)
        DrawableCompat.setTint(seekBar.progressDrawable, color)

        addView(videoCurrentTimeTextView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        val layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT)
        layoutParams.weight = 1f
        addView(seekBar, layoutParams)

        addView(videoDurationTextView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        gravity = Gravity.CENTER_VERTICAL

        seekBar.setOnSeekBarChangeListener(this)
    }

    private fun updateControlsState(state: PlayerConstants.PlayerState) {
        when (state) {
            PlayerConstants.PlayerState.ENDED -> isPlaying = false
            PlayerConstants.PlayerState.PAUSED -> isPlaying = false
            PlayerConstants.PlayerState.PLAYING -> isPlaying = true
            PlayerConstants.PlayerState.UNSTARTED -> resetUI()
            else -> { }
        }
    }

    private fun resetUI() {
        seekBar.progress = 0
        seekBar.max = 0
        videoDurationTextView.post { videoDurationTextView.text = "" }
    }

    // Seekbar

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        videoCurrentTimeTextView.text = Utils.formatTime(progress.toFloat())
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
        seekBarTouchStarted = true
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        if (isPlaying)
            newSeekBarProgress = seekBar.progress

        youtubePlayerSeekBarListener?.seekTo(seekBar.progress.toFloat())
        seekBarTouchStarted = false
    }

    // YouTubePlayerListener

    override fun onStateChange(state: PlayerConstants.PlayerState) {
        newSeekBarProgress = -1
        updateControlsState(state)
    }

    override fun onCurrentSecond(second: Float) {
        // ignore if the user is currently moving the SeekBar
        if (seekBarTouchStarted)
            return
        // ignore if the current time is older than what the user selected with the SeekBar
        if (newSeekBarProgress > 0 && Utils.formatTime(second) != Utils.formatTime(newSeekBarProgress.toFloat()))
            return

        newSeekBarProgress = -1

        seekBar.progress = second.toInt()
    }

    override fun onVideoDuration(duration: Float) {
        videoDurationTextView.text = Utils.formatTime(duration)
        seekBar.max = duration.toInt()
    }

    override fun onVideoLoadedFraction(loadedFraction: Float) {
        if (showBufferingProgress)
            seekBar.secondaryProgress = (loadedFraction * seekBar.max).toInt()
        else
            seekBar.secondaryProgress = 0
    }

    override fun onReady() { }
    override fun onVideoId(videoId: String) { }
    override fun onApiChange() { }
    override fun onPlaybackQualityChange(playbackQuality: PlayerConstants.PlaybackQuality) { }
    override fun onPlaybackRateChange(playbackRate: PlayerConstants.PlaybackRate) { }
    override fun onError(error: PlayerConstants.PlayerError) { }
}

interface YouTubePlayerSeekBarListener {
    fun seekTo(time: Float)
}