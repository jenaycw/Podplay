package com.example.podplay.repository

import com.example.podplay.model.Podcast

class PodcastRepo {
    fun getPodcast(feedUrl: String,
                   callback: (Podcast?) -> Unit) {
        callback(
            Podcast(feedUrl, "No Name", "No description", "No image")
        )
    }
}