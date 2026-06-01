package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.database.DbMediaTrack

class MinaMusicService : Service() {

    companion object {
        const val CHANNEL_ID = "mina_player_channel"
        const val NOTIFICATION_ID = 2026
        
        const val ACTION_PLAY = "com.example.play"
        const val ACTION_PAUSE = "com.example.pause"
        const val ACTION_NEXT = "com.example.next"
        const val ACTION_PREV = "com.example.prev"
        const val ACTION_STOP = "com.example.stop"
        
        @Volatile
        var activeInstance: MinaMusicService? = null
            private set

        @Volatile
        var isAutoplayEnabled = true

        @Volatile
        var isAudioQualityHigh = false
    }

    var mediaPlayer: MediaPlayer? = null
        private set

    var currentTrack: DbMediaTrack? = null
        private set

    var isTrackPlaying = false
        private set

    var playlist: List<DbMediaTrack> = emptyList()

    var onStateChangedListener: (() -> Unit)? = null

    override fun onCreate() {
        super.onCreate()
        activeInstance = this
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            when (action) {
                ACTION_PLAY -> resumePlayback()
                ACTION_PAUSE -> pausePlayback()
                ACTION_NEXT -> playNextTrack()
                ACTION_PREV -> playPreviousTrack()
                ACTION_STOP -> stopAndCleanup()
            }
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "تشغيل الموسيقى",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "التحكم في تشغيل الأغاني خارج التطبيق"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun startPlaying(track: DbMediaTrack, tracksList: List<DbMediaTrack>) {
        currentTrack = track
        playlist = tracksList

        mediaPlayer?.release()
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setOnPreparedListener { mp ->
                    mp.start()
                    isTrackPlaying = true
                    showNotification()
                    onStateChangedListener?.invoke()
                }
                setOnCompletionListener {
                    if (isAutoplayEnabled) {
                        playNextTrack()
                    } else {
                        isTrackPlaying = false
                        showNotification()
                        onStateChangedListener?.invoke()
                    }
                }
                setOnErrorListener { _, _, _ ->
                    // Auto play next on error
                    playNextTrack()
                    true
                }
                
                val urlStr = track.url
                if (urlStr.startsWith("content://") || urlStr.startsWith("http://") || urlStr.startsWith("https://")) {
                    setDataSource(this@MinaMusicService, Uri.parse(urlStr))
                } else {
                    setDataSource(urlStr)
                }
                prepareAsync()
            }
            isTrackPlaying = false
            showNotification()
            onStateChangedListener?.invoke()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun resumePlayback() {
        mediaPlayer?.let {
            if (!it.isPlaying) {
                it.start()
                isTrackPlaying = true
                showNotification()
                onStateChangedListener?.invoke()
            }
        }
    }

    fun pausePlayback() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                isTrackPlaying = false
                showNotification()
                onStateChangedListener?.invoke()
            }
        }
    }

    fun playNextTrack() {
        if (playlist.isEmpty()) return
        val currentIndex = playlist.indexOfFirst { it.id == currentTrack?.id }
        if (currentIndex != -1 && playlist.size > 1) {
            val nextIndex = (currentIndex + 1) % playlist.size
            startPlaying(playlist[nextIndex], playlist)
        } else if (playlist.isNotEmpty()) {
            startPlaying(playlist.first(), playlist)
        }
    }

    fun playPreviousTrack() {
        if (playlist.isEmpty()) return
        val currentIndex = playlist.indexOfFirst { it.id == currentTrack?.id }
        if (currentIndex != -1 && playlist.size > 1) {
            val prevIndex = if (currentIndex - 1 < 0) playlist.size - 1 else currentIndex - 1
            startPlaying(playlist[prevIndex], playlist)
        } else if (playlist.isNotEmpty()) {
            startPlaying(playlist.first(), playlist)
        }
    }

    private fun showNotification() {
        val track = currentTrack ?: return

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevPendingIntent = PendingIntent.getService(
            this, 1, Intent(this, MinaMusicService::class.java).setAction(ACTION_PREV),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val playAction = if (isTrackPlaying) ACTION_PAUSE else ACTION_PLAY
        val playPendingIntent = PendingIntent.getService(
            this, 2, Intent(this, MinaMusicService::class.java).setAction(playAction),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextPendingIntent = PendingIntent.getService(
            this, 3, Intent(this, MinaMusicService::class.java).setAction(ACTION_NEXT),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playIconRes = if (isTrackPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(track.name)
            .setContentText("مشغل مينا • جاري التشغيل في الخلفية")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(contentPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(android.R.drawable.ic_media_previous, "السابق", prevPendingIntent)
            .addAction(playIconRes, if (isTrackPlaying) "إيقاف مؤقت" else "تشغيل", playPendingIntent)
            .addAction(android.R.drawable.ic_media_next, "التالي", nextPendingIntent)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0, 1, 2))
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    fun stopAndCleanup() {
        mediaPlayer?.release()
        mediaPlayer = null
        isTrackPlaying = false
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (activeInstance == this) {
            activeInstance = null
        }
        mediaPlayer?.release()
    }
}
