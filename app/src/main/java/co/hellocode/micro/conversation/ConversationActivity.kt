package co.hellocode.micro.conversation

import android.content.Context
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import co.hellocode.micro.extensions.onChange
import co.hellocode.micro.services.APIService
import co.hellocode.micro.services.GetEndpoint
import co.hellocode.micro.services.PostEndpoint
import co.hellocode.micro.R
import co.hellocode.micro.models.Post
import co.hellocode.micro.utils.PREFS_FILENAME
import co.hellocode.micro.utils.TOKEN
import com.android.volley.AuthFailureError
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import kotlinx.android.synthetic.main.activity_conversation.*
import org.json.JSONArray
import org.json.JSONObject

class ConversationActivity() : BaseTimelineActivity() {
    override var url = "https://micro.blog/posts/conversation?id="
    override var title = "Conversation"
    var startText = ""
    private var postID: Int = 0

    override fun initialLoad() {
        this.postID = intent.getIntExtra("@string/reply_intent_extra_postID", 0)
        Log.i("ConversationAct", "id: ${this.postID}")
        if (this.postID == 0) {
            // Don't try to load if the postID couldn't be found and is still the default of zero
            this.finish()
        }
        this.url = this.url + postID.toString()
        super.initialLoad()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.adapter = TimelineRecyclerAdapter(this.posts, canShowConversations = false)
        conversation_recyclerView.adapter = this.adapter

        reply_button.isEnabled = false
        reply_view.onChange {
            reply_button.isEnabled = it.isNotEmpty()
        }
        reply_button.setOnClickListener {
            submitPost(reply_button)
        }

        // set up start text for reply box
        val author = intent.getStringExtra("@string/reply_intent_extra_author")
        this.startText += "@$author "
        val mentions = intent.getStringArrayListExtra("@string/reply_intent_extra_mentions")
        if (mentions != null) {
            for (mention in mentions) {
                this.startText += "$mention "
            }
        }
        reply_view.setOnFocusChangeListener { v, hasFocus ->
            if (v === reply_view && hasFocus && (v.text == null || v.text.toString() == "")) {
                v.setText(this.startText)
            }
        }
    }

    override fun createPosts(items: JSONArray) {
        this.posts.clear()
        for (i in 0 until items.length()) {
            val item = items[i] as JSONObject
            this.posts.add(Post(item))
        }
        this.posts.reverse()
    }

    override fun contentView(): Int {
        return R.layout.activity_conversation
    }

    override fun recycler() : RecyclerView {
        return conversation_recyclerView
    }

    private fun submitPost(view: View) {
        if (this.postID == null) {
            Log.i("ConversationActivity", "submitPost - postID is null")
            Snackbar.make(view, "Sorry! Something went wrong.", Snackbar.LENGTH_SHORT).show()
            return
        }

        spinner.visibility = View.VISIBLE
        reply_button.visibility = View.GONE

        val text = reply_view.text.toString()
        val service = APIService()
        service.postTo(PostEndpoint.Reply(this.postID, text),
                Response.Listener { response ->
                    Snackbar.make(view, "Success!", Snackbar.LENGTH_SHORT).show()
                    reply_view.setText("")
                    getConversation(view)
                },
                Response.ErrorListener { error ->
                    Log.i("ConversationActivity", "err posting reply: $error msg: ${error.message}")
                    Snackbar.make(view, "Error: $error", Snackbar.LENGTH_LONG).show()
                    spinner.visibility = View.GONE
                    reply_button.visibility = View.VISIBLE
                },
                this)
    }

    private fun getConversation(view: View) {
        if (this.postID == null) {
            Log.i("ConversationActivity", "submitPost - postID is null")
            Snackbar.make(view, "Sorry! Something went wrong.", Snackbar.LENGTH_SHORT).show()
            return
        }

        val service = APIService()
        service.get(GetEndpoint.Conversation(this.postID),
                Response.Listener { response ->
                    spinner.visibility = View.GONE
                    reply_button.visibility = View.VISIBLE
                },
                Response.ErrorListener { error ->
                    Log.i("ConversationActivity", "err getting convo: $error msg: ${error.message}")
                    Snackbar.make(view, "Error: $error", Snackbar.LENGTH_LONG).show()
                    spinner.visibility = View.GONE
                    reply_button.visibility = View.VISIBLE
                },
                this)
    }
}
