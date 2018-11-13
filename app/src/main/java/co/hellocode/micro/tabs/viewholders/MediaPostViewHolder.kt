package co.hellocode.micro.tabs.viewholders

import android.content.Intent
import android.text.format.DateUtils
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import co.hellocode.micro.conversation.ConversationActivity
import co.hellocode.micro.models.Post
import co.hellocode.micro.profile.ProfileActivity
import co.hellocode.micro.R
import co.hellocode.micro.newpost.NewPostActivity
import co.hellocode.micro.tabs.recyclers.BaseViewHolder
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.CropCircleTransformation
import kotlinx.android.synthetic.main.layout_post_image.view.*
import kotlinx.android.synthetic.main.timeline_media_item.view.*

class MediaPostViewHolder(parent: ViewGroup, private var canShowConversations: Boolean)
    : BaseViewHolder<Post>(parent, R.layout.timeline_media_item) {

    private var post: Post? = null

    init {
        Log.i("MediaPostVH", "init")
        if (this.canShowConversations) {
            rootView.setOnClickListener {
                postDetailIntent(it)
            }
            rootView.media_post_itemText.setOnClickListener {
                postDetailIntent(it)
            }
        }
        rootView.setOnLongClickListener {
            if (post == null) {
                return@setOnLongClickListener false
            }
            newPostIntent(it)
            true
        }
        rootView.media_post_itemText.setOnLongClickListener {
            if (post == null) {
                return@setOnLongClickListener false
            }
            newPostIntent(it)
            true
        }
        rootView.media_post_avatar.setOnClickListener {
            avatarClick(it)
        }
    }

    private fun avatarClick(view: View) {
        if (this.post?.username == null) {
            return
        }
        val intent = Intent(view.context, ProfileActivity::class.java)
        intent.putExtra("username", this.post?.username)
        view.context.startActivity(intent)
    }

    private fun newPostIntent(view: View) {
        val intent = Intent(view.context, NewPostActivity::class.java)
        intent.putExtra("@string/reply_intent_extra_postID", this.post?.ID)
        intent.putExtra("@string/reply_intent_extra_author", this.post?.username)
        if (this.post?.mentions != null) {
            intent.putStringArrayListExtra("mentions", this.post?.mentions)
        }
        view.context.startActivity(intent)
    }

    private fun postDetailIntent(view: View) {
        val intent = Intent(view.context, ConversationActivity::class.java)
        intent.putExtra("@string/reply_intent_extra_postID", this.post?.ID)
        intent.putExtra("@string/reply_intent_extra_author", this.post?.username)
        if (this.post?.mentions != null) {
            intent.putStringArrayListExtra("@string/reply_intent_extra_mentions", this.post?.mentions)
        }
        view.context.startActivity(intent)
    }

    override fun bindItem(item: Post) {
        Log.i("MediaVH", "bindItem")
        this.post = item
        // remove any image views leftover from reusing views
        for (i in 0 until rootView.media_outer_layout.childCount) {
            val v = rootView.media_outer_layout.getChildAt(i)
            if (v is ImageView) {
                rootView.media_outer_layout.removeViewAt(i)
            }
        }
        // and remove user avatar image
        rootView.media_post_avatar.setImageDrawable(null)

        rootView.media_post_itemText.setOnClickListener { v ->
            if (this.canShowConversations) {
                postDetailIntent(v)
            }
        }

        rootView.media_post_itemText.text = item.getParsedContent(rootView.context)
        rootView.media_post_itemText.movementMethod = LinkMovementMethod.getInstance() // make links open in browser when tapped
        rootView.media_post_author.text = item.authorName
        rootView.media_post_username.text = "@${item.username}"

        rootView.media_post_timestamp.text = DateUtils.getRelativeTimeSpanString(rootView.context, item.date.time)

        val picasso = Picasso.get()
//            picasso.setIndicatorsEnabled(true) // Uncomment this line to see coloured corners on images, indicating where they're loading from
        // Red = network, blue = disk, green = memory
        picasso.load(item.authorAvatarURL).transform(CropCircleTransformation()).into(rootView.media_post_avatar)

        for (i in item.imageSources) {
            val imageView = LayoutInflater.from(rootView.context).inflate(
                    R.layout.layout_post_image,
                    null,
                    false
            )
            // using index 1 is going to put multiple images in the wrong order
            // but I'm not sure how to fix that just yet
            rootView.media_outer_layout.addView(imageView, 1)
            if (this.canShowConversations) {
                imageView.setOnClickListener {
                    postDetailIntent(it)
                }
            }
            picasso.load(i).into(imageView.post_image)
        }
    }
}