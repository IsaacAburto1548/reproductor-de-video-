package com.example.reproductordevideo

import android.app.PictureInPictureParams
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@OptIn(UnstableApi::class)
class MainActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var touchOverlay: View
    private lateinit var rewindIndicator: TextView
    private lateinit var forwardIndicator: TextView
    private lateinit var centerPlayButton: ImageView

    private lateinit var gestureDetector: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        playerView = findViewById(R.id.playerView)
        touchOverlay = findViewById(R.id.touchOverlay)
        rewindIndicator = findViewById(R.id.rewindIndicator)
        forwardIndicator = findViewById(R.id.forwardIndicator)
        centerPlayButton = findViewById(R.id.centerPlayButton)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupGestures()
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
                if (player?.isPlaying == true) {
                    player?.pause()
                    centerPlayButton.setImageResource(android.R.drawable.ic_media_pause)
                } else {
                    player?.play()
                    centerPlayButton.setImageResource(android.R.drawable.ic_media_play)
                }
                centerPlayButton.visibility = View.VISIBLE
                centerPlayButton.postDelayed({ centerPlayButton.visibility = View.GONE }, 800)
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

    private fun seekRelative(millis: Long) {
        player?.let {
            val newPosition = it.currentPosition + millis
            it.seekTo(newPosition.coerceIn(0, it.duration))
        }
    }

    private fun showIndicator(indicator: TextView) {
        indicator.visibility = View.VISIBLE
        indicator.postDelayed({ indicator.visibility = View.GONE }, 800)
    }

    private fun initializePlayer() {
        player = ExoPlayer.Builder(this).build().also { exoPlayer ->
            playerView.player = exoPlayer

            val videoUrl = "https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny.mp4"
            val mediaItem = MediaItem.fromUri(videoUrl)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()
        }
    }

    override fun onStart() {
        super.onStart()
        initializePlayer()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    private fun releasePlayer() {
        player?.release()
        player = null
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
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
}