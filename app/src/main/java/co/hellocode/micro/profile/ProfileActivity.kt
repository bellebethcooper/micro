package co.hellocode.micro.profile

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.design.widget.Snackbar
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.View
import co.hellocode.micro.R
import co.hellocode.micro.conversation.TimelineRecyclerAdapter
import co.hellocode.micro.models.Post
import co.hellocode.micro.newpost.NewPostActivity
import co.hellocode.micro.utils.NEW_POST_REQUEST_CODE
import co.hellocode.micro.utils.PREFS_FILENAME
import co.hellocode.micro.utils.TOKEN
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.TimeoutError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.CropCircleTransformation
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_profile_collapsing.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.ArrayList
import java.util.HashMap

class ProfileActivity : AppCompatActivity() {

    var url = "https://micro.blog/posts/"
    var title = ""
    private lateinit var linearLayoutManager: LinearLayoutManager
    open lateinit var adapter: TimelineRecyclerAdapter
    open var posts = ArrayList<Post>()
    private lateinit var refresh: SwipeRefreshLayout
    var following = false
    lateinit var username: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(contentView())
        setSupportActionBar(toolbar)
        supportActionBar?.title = title
        this.linearLayoutManager = LinearLayoutManager(this)
        profile_recyclerView.layoutManager = this.linearLayoutManager
        this.adapter = TimelineRecyclerAdapter(this.posts)
        profile_recyclerView.adapter = this.adapter
        Log.i("BaseTimeline", "recycler: $profile_recyclerView")
        collapsing_toolbar.setCollapsedTitleTextColor(resources.getColor(R.color.colorWhite))
        refresh = profile_refresher
        refresh.setOnRefreshListener { refresh() }
        initialLoad()
    }

    fun initialLoad() {
        this.username = intent.getStringExtra("username")
        this.url = this.url + this.username
        this.refresh.isRefreshing = true
        refresh()
    }

    private fun refresh() {
        Log.i("BaseTimeline", "refresh")
        getTimeline()
    }

    fun contentView(): Int {
        return R.layout.activity_profile_collapsing
    }

    fun prefs() : SharedPreferences {
        return getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
    }

    private fun getTimeline() {
        Log.i("BaseTimeline", "getTimeline")
        val rq = object : JsonObjectRequest(
                this.url,
                null,
                Response.Listener<JSONObject> { response ->
                    //                    Log.i("MainActivity", "resp: $response")
                    val items = response["items"] as JSONArray
                    createPosts(items)
                    getRequestComplete(response)
                    this.adapter.notifyDataSetChanged()
                    this.refresh.isRefreshing = false
                },
                Response.ErrorListener { error ->
                    Log.i("MainActivity", "err: $error msg: ${error.message}")
                    this.refresh.isRefreshing = false
                    if (error is TimeoutError) {
                        Snackbar.make(this.profile_recyclerView, "Request timed out; trying again", Snackbar.LENGTH_SHORT)
                        this.getTimeline()
                    }
                }) {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                val prefs = prefs()
                val token: String? = prefs.getString(TOKEN, null)
                headers["Authorization"] = "Bearer $token"
                return headers
            }
        }
        val queue = Volley.newRequestQueue(this)
        queue.add(rq)
    }

    fun createPosts(items: JSONArray) {
        this.posts.clear()
        for (i in 0 until items.length()) {
            val item = items[i] as JSONObject
            this.posts.add(Post(item))
        }
    }

    fun getRequestComplete(response: JSONObject) {
        val author = response.getJSONObject("author")
        val microBlog = response.getJSONObject("_microblog")
        setProfileData(author, microBlog)
        collapsing_profile_follow_button.setOnClickListener { followButtonTapped(this.username) }
        setToolbarTitle(this.username)
        setFABListener()
    }

    fun setProfileData(author: JSONObject, microBlogData: JSONObject) {
        collapsing_profile_name_view.text = author.getString("name")
        val website = author.getString("url")
        if (website.length > 0) {
            collapsing_profile_website.text = website
        } else {
            collapsing_profile_website.visibility = View.GONE
        }
        val avatarURL = author.getString("avatar")
        Picasso.get().load(avatarURL).transform(CropCircleTransformation()).into(collapsing_profile_avatar)
        collapsing_profile_username.text = microBlogData.getString("username")

        val bio = microBlogData.getString("bio")
        if (bio.length > 0) {
            collapsing_profile_bio.text = bio
        } else {
            collapsing_profile_bio.visibility = View.GONE
        }
        val isYou = microBlogData.getBoolean("is_you")
        if (isYou) {
            collapsing_profile_follow_button.visibility = View.GONE
        } else {
            following = microBlogData.getBoolean("is_following")
            collapsing_profile_follow_button.text = if (this.following == true) "Unfollow" else "Follow"
        }

    }

    fun setFABListener() {
        profile_fab.setOnClickListener {
            val intent = Intent(this, NewPostActivity::class.java)
            Log.i("ProfileAct", "author: $username")
            intent.putExtra("@string/reply_intent_extra_author", this.username)
            startActivityForResult(intent, NEW_POST_REQUEST_CODE)
        }
    }

    private fun setToolbarTitle(username: String) {
        collapsing_profile_appbar.addOnOffsetChangedListener(object : AppBarLayout.OnOffsetChangedListener {
            var isShown = false
            var scrollRange = -1

            override fun onOffsetChanged(appBarLayout: AppBarLayout, verticalOffset: Int) {
                if (scrollRange == -1) {
                    scrollRange = appBarLayout.totalScrollRange
                }
                Log.i("ProfileAct", "scroll range: $scrollRange offset: $verticalOffset")
                if (scrollRange + verticalOffset == 0) {
                    collapsing_profile_view.visibility = View.INVISIBLE
                    collapsing_toolbar.title = username
                    isShown = true
                } else {
                    collapsing_profile_view.visibility = View.VISIBLE
                    collapsing_toolbar.title = " "
                    isShown = false
                }

            }
        })
    }

    private fun followButtonTapped(username: String) {
        val followURL = if (this.following) {
            "https://micro.blog/users/unfollow"
        } else {
            "https://micro.blog/users/follow"
        }
        val rq = object : StringRequest(
                Request.Method.POST,
                followURL,
                Response.Listener<String> { response ->
                    if (this.following) {
                        // this.following is already true, so we just unfollowed
                        collapsing_profile_follow_button.text = "Follow"
                        this.following = false
                    } else {
                        // this.following is false, so we just followed
                        collapsing_profile_follow_button.text = "Unfollow"
                        this.following = true
                    }
                },
                Response.ErrorListener { error ->
                    Log.i("ProfileAct", "err following or unfollowing: $error msg: ${error.message}")
                }) {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                val prefs = prefs()
                val token: String? = prefs.getString(TOKEN, null)
                headers["Authorization"] = "Bearer $token"
                return headers
            }

            @Throws(AuthFailureError::class)
            override fun getParams(): Map<String, String> {
                Log.i("MainActivity", "getParams")
                val params = HashMap<String, String>()
                params["username"] = username
                return params
            }
        }
        val queue = Volley.newRequestQueue(this)
        queue.add(rq)
    }
}
