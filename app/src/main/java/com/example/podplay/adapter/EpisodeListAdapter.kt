package com.example.podplay.adapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.example.podplay.viewmodel.PodcastViewModel

interface EpisodeListAdapterListener {
    fun onSelectedEpisode(episodeViewData: PodcastViewModel.EpisodeViewData)
}
class EpisodeListAdapter(
    private var episodeViewList: List<PodcastViewModel.EpisodeViewData>?,
    private val episodeListAdapterListener:
    EpisodeListAdapterListener) :
    RecyclerView.Adapter<EpisodeListAdapter.ViewHolder>() {
    class ViewHolder(
        v: View, private
        val episodeListAdapterListener:
        EpisodeListAdapterListener) :
        RecyclerView.ViewHolder(v)

}

