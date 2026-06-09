package com.example.reproductordevideo

import android.app.PictureInPictureParams
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Rational
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.util.Formatter
import java.util.Locale

@OptIn(UnstableApi::class)
class MainActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var touchOverlay: View
    private lateinit var controlsContainer: View
    private lateinit var btnPlayPause: ImageView
    private lateinit var btnRewind: ImageView
    private lateinit var btnForward: ImageView
    private lateinit var btnPiP: ImageView
    private lateinit var btnFullscreen: ImageView
    private lateinit var btnBack: ImageView
    private lateinit var tvCurrentTime: TextView
    private lateinit var tvDuration: TextView
    private lateinit var videoSeekBar: SeekBar
    private lateinit var rewindIndicator: TextView
    private lateinit var forwardIndicator: TextView

    private lateinit var gestureDetector: GestureDetector
    private val handler = Handler(Looper.getMainLooper())
    private val updateProgressAction = object : Runnable {
        override fun run() {
            updateProgressBar()
            handler.postDelayed(this, 1000)
        }
    }

    private var areControlsVisible = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        initViews()
        setupListeners()
        setupGestures()
        showControls()
    }

    private fun initViews() {
        playerView = findViewById(R.id.playerView)
        touchOverlay = findViewById(R.id.touchOverlay)
        controlsContainer = findViewById(R.id.controlsContainer)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        btnRewind = findViewById(R.id.btnRewind)
        btnForward = findViewById(R.id.btnForward)
        btnPiP = findViewById(R.id.btnPiP)
        btnFullscreen = findViewById(R.id.btnFullscreen)
        btnBack = findViewById(R.id.btnBack)
        tvCurrentTime = findViewById(R.id.tvCurrentTime)
        tvDuration = findViewById(R.id.tvDuration)
        videoSeekBar = findViewById(R.id.videoSeekBar)
        rewindIndicator = findViewById(R.id.rewindIndicator)
        forwardIndicator = findViewById(R.id.forwardIndicator)
    }

    private fun setupListeners() {
        btnPlayPause.setOnClickListener { togglePlayPause() }
        btnRewind.setOnClickListener { seekRelative(-10000) }
        btnForward.setOnClickListener { seekRelative(10000) }
        btnPiP.setOnClickListener { enterPiP() }
        btnBack.setOnClickListener { finish() }
        btnFullscreen.setOnClickListener { 
            // In a real app, this might toggle orientation or immersive mode
            hideSystemBars() 
        }

        videoSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    player?.seekTo(progress.toLong())
                    tvCurrentTime.text = formatTime(progress.toLong())
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                handler.removeCallbacks(updateProgressAction)
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                handler.post(updateProgressAction)
                resetControlsTimer()
            }
        })
    }

    private fun setupGestures() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                val width = touchOverlay.width
                if (e.x < width / 2) {
                    seekRelative(-10000)
                    showIndicator(rewindIndicator)
                } else {
                    seekRelative(10000)
                    showIndicator(forwardIndicator)
                }
                return true
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                toggleControls()
                return true
            }
        })

        touchOverlay.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                v.performClick()
            }
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun togglePlayPause() {
        player?.let {
            if (it.isPlaying) {
                it.pause()
                btnPlayPause.setImageResource(R.drawable.ic_play)
            } else {
                it.play()
                btnPlayPause.setImageResource(R.drawable.ic_pause)
            }
            resetControlsTimer()
        }
    }

    private fun toggleControls() {
        if (areControlsVisible) {
            hideControls()
        } else {
            showControls()
        }
    }

    private fun showControls() {
        controlsContainer.visibility = View.VISIBLE
        areControlsVisible = true
        resetControlsTimer()
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
    }

    private fun hideControls() {
        controlsContainer.visibility = View.GONE
        areControlsVisible = false
        hideSystemBars()
    }

    private fun resetControlsTimer() {
        handler.removeCallbacks(hideControlsAction)
        handler.postDelayed(hideControlsAction, 3000)
    }

    private val hideControlsAction = Runnable { hideControls() }

    private fun hideSystemBars() {
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun seekRelative(millis: Long) {
        player?.let {
            if (it.duration <= 0) return
            val newPosition = it.currentPosition + millis
            it.seekTo(newPosition.coerceIn(0, it.duration))
            updateProgressBar()
            resetControlsTimer()
        }
    }

    private fun showIndicator(indicator: TextView) {
        indicator.visibility = View.VISIBLE
        indicator.postDelayed({ indicator.visibility = View.GONE }, 800)
    }

    private fun initializePlayer() {
        player = ExoPlayer.Builder(this).build().also { exoPlayer ->
            playerView.player = exoPlayer

            val videoUrl = "https://www.w3schools.com/html/mov_bbb.mp4"
            val mediaItem = MediaItem.fromUri(videoUrl)
            exoPlayer.setMediaItem(mediaItem)
            
            exoPlayer.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) {
                        videoSeekBar.max = exoPlayer.duration.toInt()
                        tvDuration.text = String.format("/ %s", formatTime(exoPlayer.duration))
                        handler.post(updateProgressAction)
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    btnPlayPause.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
                }
            })

            exoPlayer.prepare()
            exoPlayer.play()
        }
    }

    private fun updateProgressBar() {
        player?.let {
            videoSeekBar.progress = it.currentPosition.toInt()
            tvCurrentTime.text = formatTime(it.currentPosition)
        }
    }

    private fun formatTime(timeMs: Long): String {
        val totalSeconds = timeMs / 1000
        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        val hours = totalSeconds / 3600
        val formatBuilder = StringBuilder()
        val formatter = Formatter(formatBuilder, Locale.getDefault())
        return if (hours > 0) {
            formatter.format("%d:%02d:%02d", hours, minutes, seconds).toString()
        } else {
            formatter.format("%02d:%02d", minutes, seconds).toString()
        }
    }

    private fun enterPiP() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        } else {
            @Suppress("DEPRECATION")
            enterPictureInPictureMode()
        }
    }

    override fun onStart() {
        super.onStart()
        initializePlayer()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
        handler.removeCallbacks(updateProgressAction)
    }

    private fun releasePlayer() {
        player?.release()
        player = null
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        enterPiP()
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: android.content.res.Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) {
            hideControls()
        } else {
            showControls()
        }
    }
}