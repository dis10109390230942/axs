package com.example.axs_player.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.axs_player.AxsApplication
import com.example.axs_player.MainActivity
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PlaybackService : MediaSessionService() {

    private val scope = MainScope()
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val application = application as AxsApplication
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        session = MediaSession.Builder(this, SessionPlayer(application.playbackController))
            .setSessionActivity(openApp)
            .setCallback(ResumptionCallback(application.playbackQueueStore))
            .build()
            .also(::addSession)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        scope.cancel()
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }

    private inner class ResumptionCallback(private val store: PlaybackQueueStore) : MediaSession.Callback {

        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            isForPlayback: Boolean
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val result = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
            scope.launch {
                val saved = store.load()
                if (saved == null || saved.tracks.isEmpty()) {
                    result.setException(IllegalStateException("Нет сохранённой очереди"))
                } else {
                    result.set(
                        MediaSession.MediaItemsWithStartPosition(
                            saved.tracks.mapIndexed { index, track -> track.toMediaItem(index.toString()) },
                            saved.currentIndex.coerceIn(saved.tracks.indices),
                            saved.positionMs
                        )
                    )
                }
            }
            return result
        }
    }
}
