package com.example.podplay.ui

import android.content.ComponentName
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.method.ScrollingMovementMethod
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.podplay.R
import com.example.podplay.adapter.EpisodeListAdapter
import com.example.podplay.adapter.EpisodeListAdapterListener
import com.example.podplay.service.PodplayMediaService
import com.example.podplay.viewmodel.PodcastViewModel
import kotlinx.android.synthetic.main.fragment_podcast_details.*

class PodcastDetailsFragment : Fragment(), EpisodeListAdapterListener {
    private lateinit var mediaBrowser: MediaBrowserCompat
    private var mediaControllerCallback: MediaControllerCallback? = null
    private lateinit var episodeListAdapter: EpisodeListAdapter



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ):
            View? {
        return inflater.inflate(
            R.layout.fragment_podcast_details,
            container, false
        )


    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {

        super.onActivityCreated(savedInstanceState)
    }

    override fun onCreateOptionsMenu(
        menu: Menu,
        inflater: MenuInflater)
    {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_details, menu)
        updateControls()

        initMediaBrowser()

    }

    private val podcastViewModel: PodcastViewModel by activityViewModels()
    private fun updateControls() {
        val viewData = podcastViewModel.activePodcastViewData ?: return
        feedTitleTextView.text = viewData.feedTitle
        feedDescTextView.text = viewData.feedDesc
        activity?.let { activity ->
            Glide.with(activity).load(viewData.imageUrl)
                .into(feedImageView)
        }

    }
    
    companion object {
        fun newInstance(): PodcastDetailsFragment {
            return PodcastDetailsFragment()
        }
    }
    inner class MediaControllerCallback:
        MediaControllerCompat.Callback() {
        override fun onMetadataChanged(metadata:
                                       MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            println(
                "metadata changed to ${metadata?.getString(   
                    MediaMetadataCompat.METADATA_KEY_MEDIA_URI)}")
        }      override fun onPlaybackStateChanged(state: PlaybackStateCompat?)
        {
            super.onPlaybackStateChanged(state)
            println("state changed to $state")   }
    }
    private fun registerMediaController(token: MediaSessionCompat.Token) {
        val fragmentActivity = activity as FragmentActivity
        val mediaController = MediaControllerCompat(fragmentActivity, token)
        MediaControllerCompat.setMediaController(fragmentActivity, mediaController)
        mediaControllerCallback = MediaControllerCallback()
        mediaController.registerCallback(mediaControllerCallback!!)
    }
    inner class MediaBrowserCallBacks:   MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected()
        {
            super.onConnected()
            registerMediaController(mediaBrowser.sessionToken)
            println("onConnected")   }
        override fun onConnectionSuspended() {
            super.onConnectionSuspended()
            println("onConnectionSuspended")
        }
            override fun onConnectionFailed() {

            }
    }
    private fun initMediaBrowser() {
        val fragmentActivity = activity as FragmentActivity
        mediaBrowser = MediaBrowserCompat(fragmentActivity,
            ComponentName(fragmentActivity,
                PodplayMediaService::class.java),
            MediaBrowserCallBacks(),
            null)
    }
    override fun onStart() {
        super.onStart()
        if (mediaBrowser.isConnected) {
            val fragmentActivity = activity as FragmentActivity
            if (MediaControllerCompat.getMediaController
                    (fragmentActivity) == null) {
                registerMediaController(mediaBrowser.sessionToken)
            }
        }
        else {
            mediaBrowser.connect()
        }
    }
    override fun onStop() {
        super.onStop()
        val fragmentActivity = activity as FragmentActivity
        if (MediaControllerCompat.getMediaController(fragmentActivity)
            != null)
        {
            mediaControllerCallback?.let {
                MediaControllerCompat.getMediaController(fragmentActivity)
                    .unregisterCallback(it)
            }
        }
    }
    private fun startPlaying(
        episodeViewData: PodcastViewModel.EpisodeViewData) {
        val fragmentActivity = activity as FragmentActivity
        val controller =
            MediaControllerCompat.getMediaController(fragmentActivity)
        controller.transportControls.playFromUri(
            Uri.parse(episodeViewData.mediaUrl),
            null)
    }
    override fun onSelectedEpisode(episodeViewData: PodcastViewModel.EpisodeViewData) {
        val fragmentActivity = activity as FragmentActivity
        val controller =
            MediaControllerCompat.getMediaController(fragmentActivity)
        if (controller.playbackState != null) {
            if (controller.playbackState.state ==
                PlaybackStateCompat.STATE_PLAYING) {
                controller.transportControls.pause()
            } else {
                startPlaying(episodeViewData)
            }
        } else {
            startPlaying(episodeViewData)
        }

    }


    }




