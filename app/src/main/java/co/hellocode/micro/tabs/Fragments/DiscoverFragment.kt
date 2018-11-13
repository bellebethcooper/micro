package co.hellocode.micro.tabs.fragments

class DiscoverFragment : BaseTimelineFragment() {

    override var url = "https://micro.blog/posts/discover"
    override open var title = "Discover"

    companion object {
        fun newInstance(): DiscoverFragment = DiscoverFragment()
    }

    override fun refresh() {
        getTimeline(this.url)
    }
}