package com.example.podplay.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.podplay.repository.ItunesRepo
import com.example.podplay.service.PodcastResponse
class SearchViewModel(application: Application) :
    AndroidViewModel(application) {
    fun searchPodcasts(term: String, function: (Nothing) -> Unit) {

    }
}

var iTunesRepo: ItunesRepo? = null

data class PodcastSummaryViewData(
    var name: String? = "",
    var lastUpdated: String? = "",
    var imageUrl: String? = "",
    var feedUrl: String? = "")

private fun itunesPodcastToPodcastSummaryView(
    itunesPodcast: PodcastResponse.ItunesPodcast):
        PodcastSummaryViewData {
    return PodcastSummaryViewData(
        itunesPodcast.collectionCensoredName,
        itunesPodcast.releaseDate,
        itunesPodcast.artworkUrl30,
        itunesPodcast.feedUrl)
}

fun searchPodcasts(term: String,    callback: (List<PodcastSummaryViewData>) -> Unit) {
    iTunesRepo?.searchByTerm(term) { results ->
        if (results == null) {
            callback(emptyList())
        } else {
      val searchViews = results.map { podcast ->    itunesPodcastToPodcastSummaryView(podcast)    }
            callback(searchViews)
        }
    }
}
