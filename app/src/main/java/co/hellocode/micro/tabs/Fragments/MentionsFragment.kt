package co.hellocode.micro.tabs.fragments

import android.util.Log

class MentionsFragment : BaseTimelineFragment() {

    override var url = "https://micro.blog/posts/mentions"
    override open var title = "Mentions"

    companion object {
        fun newInstance(): MentionsFragment = MentionsFragment()
    }

    override fun refresh() {
        Log.i("MentionsFrag", "url: ${ this.url }")
        getTimeline(this.url)
    }
}