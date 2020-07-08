package com.example.podplay.ui


import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.appcompat.widget.SearchView
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.work.*
import com.example.podplay.R
import com.example.podplay.adapter.PodcastListAdapter
import com.example.podplay.adapter.PodcastListAdapter.PodcastListAdapterListener
import com.example.podplay.db.PodPlayDatabase
import com.example.podplay.repository.ItunesRepo
import com.example.podplay.repository.PodcastRepo
import com.example.podplay.worker.EpisodeUpdateWorker
import com.example.podplay.service.FeedService
import com.example.podplay.service.ItunesService
import com.example.podplay.ui.PodcastDetailsFragment.OnPodcastDetailsListener
import com.example.podplay.viewmodel.PodcastViewModel
import com.example.podplay.viewmodel.SearchViewModel
import kotlinx.android.synthetic.main.activity_podcast.*
import java.util.concurrent.TimeUnit

class PodcastActivity : AppCompatActivity(), PodcastListAdapterListener,
    OnPodcastDetailsListener {

    private val searchViewModel by viewModels<SearchViewModel>()
    private val podcastViewModel by viewModels<PodcastViewModel>()
    private lateinit var podcastListAdapter: PodcastListAdapter
    private lateinit var searchMenuItem: MenuItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_podcast)
        setupToolbar()
        setupViewModels()
        updateControls()
        setupPodcastListView()
        handleIntent(intent)
        addBackStackListener()
        scheduleJobs()
    }

    override fun onSubscribe() {
        podcastViewModel.saveActivePodcast()
        supportFragmentManager.popBackStack()
    }

    override fun onUnsubscribe() {
        podcastViewModel.deleteActivePodcast()
        supportFragmentManager.popBackStack()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_search, menu)

        searchMenuItem = menu.findItem(R.id.search_item)
        val searchView = searchMenuItem.actionView as SearchView

        searchMenuItem.setOnActionExpandListener(object: MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(p0: MenuItem?): Boolean {
                return true
            }
            override fun onMenuItemActionCollapse(p0: MenuItem?): Boolean {
                showSubscribedPodcasts()
                return true
            }
        })

        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))

        if (supportFragmentManager.backStackEntryCount > 0) {
            podcastRecyclerView.visibility = View.INVISIBLE
        }

        if (podcastRecyclerView.visibility == View.INVISIBLE) {
            searchMenuItem.isVisible = false
        }

        return true
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }


    override fun onShowDetails(podcastSummaryViewData: SearchViewModel.PodcastSummaryViewData) {

        val feedUrl = podcastSummaryViewData.feedUrl ?: return

        showProgressBar()

        podcastViewModel.getPodcast(podcastSummaryViewData) {

            hideProgressBar()

            if (it != null) {
                showDetailsFragment()
            } else {
                showError("Error loading feed $feedUrl")
            }
        }
    }

    private fun scheduleJobs() {

        val constraints: Constraints = Constraints.Builder().apply {
            setRequiredNetworkType(NetworkType.CONNECTED)
            setRequiresCharging(true)
        }.build()

        val request = PeriodicWorkRequestBuilder<EpisodeUpdateWorker>(
            1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(TAG_EPISODE_UPDATE_JOB,
            ExistingPeriodicWorkPolicy.REPLACE, request)
    }

    private fun showSubscribedPodcasts()
    {
        val podcasts = podcastViewModel.getPodcasts()?.value

        if (podcasts != null) {
            toolbar.title = getString(R.string.subscribed_podcasts)
            podcastListAdapter.setSearchData(podcasts)
        }
    }

    private fun performSearch(term: String) {
        showProgressBar()
        searchViewModel.searchPodcasts(term) { results ->
            hideProgressBar()
            toolbar.title = term
            podcastListAdapter.setSearchData(results)
        }
    }

    private fun handleIntent(intent: Intent) {
        if (Intent.ACTION_SEARCH == intent.action) {
            val query = intent.getStringExtra(SearchManager.QUERY) ?: return
            performSearch(query)
        }
        val podcastFeedUrl = intent.getStringExtra(EpisodeUpdateWorker.EXTRA_FEED_URL)
        if (podcastFeedUrl != null) {
            podcastViewModel.setActivePodcast(podcastFeedUrl) {
                it?.let { podcastSummaryView -> onShowDetails(podcastSummaryView) }
            }
        }
    }


    private fun setupToolbar() {
        setSupportActionBar(toolbar)
    }

    private fun setupViewModels() {
        val service = ItunesService.instance
        searchViewModel.iTunesRepo = ItunesRepo(service)
        val rssService = FeedService.instance
        val db = PodPlayDatabase.getInstance(this)
        val podcastDao = db.podcastDao()
        podcastViewModel.podcastRepo = PodcastRepo(rssService, podcastDao)
    }

    private fun setupPodcastListView() {
        podcastViewModel.getPodcasts()?.observe(this, Observer {
            if (it != null) {
                showSubscribedPodcasts()
            }
        })
    }

    private fun addBackStackListener()
    {
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                podcastRecyclerView.visibility = View.VISIBLE
            }
        }
    }

    private fun updateControls() {
        podcastRecyclerView.setHasFixedSize(true)

        val layoutManager = LinearLayoutManager(this)
        podcastRecyclerView.layoutManager = layoutManager

        val dividerItemDecoration = DividerItemDecoration(
            podcastRecyclerView.context, layoutManager.orientation)
        podcastRecyclerView.addItemDecoration(dividerItemDecoration)

        podcastListAdapter = PodcastListAdapter(null, this, this)
        podcastRecyclerView.adapter = podcastListAdapter
    }


    private fun showDetailsFragment() {
        val podcastDetailsFragment = createPodcastDetailsFragment()

        supportFragmentManager.beginTransaction().add(R.id.podcastDetailsContainer,
            podcastDetailsFragment, TAG_DETAILS_FRAGMENT).addToBackStack("DetailsFragment").commit()
        podcastRecyclerView.visibility = View.INVISIBLE
        searchMenuItem.isVisible = false
    }

    private fun createPodcastDetailsFragment(): PodcastDetailsFragment {
        var podcastDetailsFragment = supportFragmentManager.findFragmentByTag(TAG_DETAILS_FRAGMENT) as
                PodcastDetailsFragment?

        if (podcastDetailsFragment == null) {
            podcastDetailsFragment = PodcastDetailsFragment.newInstance()
        }

        return podcastDetailsFragment
    }

    private fun showProgressBar() {
        progressBar.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        progressBar.visibility = View.INVISIBLE
    }

    private fun showError(message: String) {
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton(getString(R.string.ok_button), null)
            .create()
            .show()
    }

    companion object {
        private const val TAG_DETAILS_FRAGMENT = "DetailsFragment"
        private const val TAG_EPISODE_UPDATE_JOB = "com.raywenderlich.podplay.episodes"
    }
}
