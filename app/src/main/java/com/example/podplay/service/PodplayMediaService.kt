package com.example.podplay.service

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.MediaBrowserServiceCompat



private lateinit var mediaSession: MediaSessionCompat
class PodplayMediaService : MediaBrowserServiceCompat() {
    override fun onCreate() {
        super.onCreate()
    }

    override fun onLoadChildren (
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>)
    {}

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int, rootHints: Bundle?
    ):
            BrowserRoot? {
        return  MediaBrowserServiceCompat.BrowserRoot(     PODPLAY_EMPTY_ROOT_MEDIA_ID, null)

    }

    private fun createMediaSession() {
        mediaSession = MediaSessionCompat(this, "PodplayMediaService")
        setSessionToken(mediaSession.sessionToken)
        val callBack = PodplayMediaCallback(this, mediaSession)
        mediaSession.setCallback(callBack)
        createMediaSession()

    }
    companion object {   private const val PODPLAY_EMPTY_ROOT_MEDIA_ID =
        "podplay_empty_root_media_id" }

}

