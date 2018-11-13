package co.hellocode.micro.tabs

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import co.hellocode.micro.tabs.fragments.DiscoverFragment
import co.hellocode.micro.tabs.fragments.MediaFragment
import co.hellocode.micro.tabs.fragments.MentionsFragment
import co.hellocode.micro.tabs.fragments.TimelineFragment

class TabAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

    override fun getItem(position: Int): Fragment? = when (position) {
        0 -> TimelineFragment.newInstance()
        1 -> MentionsFragment.newInstance()
        2 -> MediaFragment.newInstance()
        3 -> DiscoverFragment.newInstance()
        else -> null
    }

    override fun getPageTitle(position: Int): CharSequence = when (position) {
        0 -> "Timeline"
        1 -> "Mentions"
        2 -> "Photos"
        3 -> "Discover"
        else -> ""
    }

    override fun getCount(): Int = 4
}