package co.hellocode.micro.conversation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import co.hellocode.micro.R
import co.hellocode.micro.models.Post
import co.hellocode.micro.newpost.NewPostActivity
import co.hellocode.micro.utils.NEW_POST_REQUEST_CODE
import co.hellocode.micro.utils.PREFS_FILENAME
import co.hellocode.micro.utils.TOKEN
import com.android.volley.AuthFailureError
import com.android.volley.Response
import com.android.volley.TimeoutError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.baselayout_timeline.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.*


abstract class BaseTimelineActivity : AppCompatActivity() {

    private lateinit var linearLayoutManager: LinearLayoutManager
    open lateinit var adapter: TimelineRecyclerAdapter
    open var posts = ArrayList<Post>()
    private lateinit var refresh: SwipeRefreshLayout
    open var url = "https://micro.blog/posts/all"
    open var title = "Timeline"

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("BaseTimeline", "oncreate")
        super.onCreate(savedInstanceState)
        setContentView(contentView())
        setSupportActionBar(toolbar)
        supportActionBar?.title = title
        this.linearLayoutManager = LinearLayoutManager(this)
        val recycler = recycler()
        recycler.layoutManager = this.linearLayoutManager
        this.adapter = TimelineRecyclerAdapter(this.posts)
        recycler.adapter = this.adapter

        if (fab != null) {
            fab.setOnClickListener {
                val intent = Intent(this, NewPostActivity::class.java)
                startActivityForResult(intent, NEW_POST_REQUEST_CODE)
            }
        }

        this.refresh = refresher
        this.refresh.setOnRefreshListener { refresh() }
        initialLoad()
    }

    open fun contentView(): Int {
        return R.layout.activity_timeline
    }

    open fun recycler(): RecyclerView {
        return recyclerView
    }

    open fun initialLoad() {
        Log.i("BaseTimeline", "initialLoad")
        this.refresh.isRefreshing = true
        refresh()
    }

    private fun refresh() {
        Log.i("BaseTimeline", "refresh")
        getTimeline()
    }

    // TODO: Remove this, because the API is aggressively cached anyway, so this isn't useful
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == NEW_POST_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            refresh()
        }
    }

    fun prefs(): SharedPreferences {
        return getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
    }

    private fun getTimeline() {
        Log.i("BaseTimeline", "getTimeline: ${this.url}")
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
                    Log.i("BaseTimelineAct", "err: $error msg: ${error.message}")
                    if (error.networkResponse != null) {
                        Log.i("BaseTimelineAct", "err: $error network resp: ${error.networkResponse}")
                    }
                    if (error is TimeoutError) {
                        Snackbar.make(this.refresh, "Request timed out; trying again", Snackbar.LENGTH_SHORT)
                        this.getTimeline()
                    } else {
                        this.refresh.isRefreshing = false
                    }
                    // TODO: Handle error
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

    open fun getRequestComplete(response: JSONObject) {

    }

    open fun createPosts(items: JSONArray) {
        this.posts.clear()
        for (i in 0 until items.length()) {
            val item = items[i] as JSONObject
            this.posts.add(Post(item))
        }
    }
}
