package co.hellocode.micro.conversation

import android.content.Intent
import android.support.v7.widget.RecyclerView
import android.text.format.DateUtils
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import co.hellocode.micro.profile.ProfileActivity
import co.hellocode.micro.R
import co.hellocode.micro.models.Post
import co.hellocode.micro.newpost.NewPostActivity
import co.hellocode.micro.tabs.viewholders.PostViewHolder
import co.hellocode.micro.utils.inflate
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.CropCircleTransformation
import kotlinx.android.synthetic.main.layout_post_image.view.*
import kotlinx.android.synthetic.main.timeline_item.view.*


open class TimelineRecyclerAdapter(private val posts: ArrayList<Post>, private val canShowConversations: Boolean = true) : RecyclerView.Adapter<PostViewHolder>() {

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): PostViewHolder {
        return PostViewHolder(p0, canShowConversations)
    }

    override fun getItemCount() = posts.size

    override fun onBindViewHolder(p0: PostViewHolder, p1: Int) {
        val itemPost = posts[p1]
        p0.bindItem(itemPost)
    }
}