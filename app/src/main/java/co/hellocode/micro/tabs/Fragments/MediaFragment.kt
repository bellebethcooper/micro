package co.hellocode.micro.tabs.fragments

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import co.hellocode.micro.*
import co.hellocode.micro.tabs.recyclers.BaseRecyclerAdapter
import co.hellocode.micro.tabs.viewholders.MediaPostViewHolder
import kotlinx.android.synthetic.main.baselayout_timeline.view.*

class MediaFragment : BaseTimelineFragment() {

    override var url = "https://micro.blog/posts/photos"
    override var title = "Photos"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.baselayout_timeline, container, false)
        this.linearLayoutManager = LinearLayoutManager(context)
        view.recyclerView.layoutManager = this.linearLayoutManager
        this.adapter = BaseRecyclerAdapter({ MediaPostViewHolder(container!!, true) }, this.posts)
        view.recyclerView.adapter = this.adapter
        this.refresh = view.refresher
        this.refresh.setOnRefreshListener { refresh() }
        return view
    }

    companion object {
        fun newInstance(): MediaFragment = MediaFragment()
    }

    override fun refresh() {
        getTimeline(this.url)
    }
}