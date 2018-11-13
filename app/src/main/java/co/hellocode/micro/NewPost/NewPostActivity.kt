package co.hellocode.micro.newpost

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import co.hellocode.micro.LoadingImagePreview
import co.hellocode.micro.extensions.onChange
import co.hellocode.micro.R
import co.hellocode.micro.utils.PREFS_FILENAME
import co.hellocode.micro.utils.TOKEN
import com.android.volley.*
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_new_post.*
import org.json.JSONObject
import uk.me.hardill.volley.multipart.MultipartRequest
import java.io.ByteArrayOutputStream

const private val PICK_IMAGE = 1

class NewPostActivity : AppCompatActivity() {

    var progress: ProgressDialog? = null
    var replyPostID: Int? = null
    var imagesUploaded: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_post)
        setSupportActionBar(toolbar)
        sendButton.isEnabled = false

        // Report that the user started a new post, so the new post shortcut gets shown to them by the OS
        val mgr = this.getSystemService(ShortcutManager::class.java)
        mgr.reportShortcutUsed("newpost")
        // grab any data shared from other apps or by user choosing to reply to a post
        checkForIntentData(intent)
        editText.requestFocus()
        // set send button to only be enabled if editText has text in it after change
        editText.onChange {
            sendButton.isEnabled = it.isNotEmpty()
        }

        // Open the user's photo gallery app to let them choose an image
        photoButton.setOnClickListener {
            chooseImage()
        }

        sendButton.setOnClickListener { view ->
            submitPost(view)
        }
    }

    private fun chooseImage() {
        val getIntent = Intent(Intent.ACTION_GET_CONTENT)
        getIntent.type = "image/*"
        val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickIntent.type = "image/*"
        val chooserIntent = Intent.createChooser(getIntent, "Select image")
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, Array(1) { pickIntent })
        startActivityForResult(chooserIntent, PICK_IMAGE)
    }

    private fun checkForIntentData(intent: Intent) {
        when {
            intent?.action == Intent.ACTION_SEND -> {
                if ("text/plain" == intent.type) {
                    handleSendText(intent) // Handle text being sent
                } else if (intent.type?.startsWith("image/") == true) {
                    handleSendImage(intent) // Handle single image being sent
                }
            }
            intent?.action == Intent.ACTION_SEND_MULTIPLE
                    && intent.type?.startsWith("image/") == true -> {
                handleSendMultipleImages(intent) // Handle multiple images being sent
            }
            else -> {
                // Check for a post passed by an intent
                // This will happen if the New Post activity was opened to create a reply
                // Use the post data passed by the intent to populate the text box
                // with usernames to reply to, and to store the post ID to send to the API
                // when submitting the reply
                val author = intent.getStringExtra("@string/reply_intent_extra_author")
                Log.i("NewPostAct", "author: $author")
                if (author != null) {
                    // this must be a reply, because we have an author to reply to
                    val postID = intent.getIntExtra("@string/reply_intent_extra_postID", 0)
                    if (postID != 0) {
                        // postID could still be null, because there's a reply action on profile pages
                        // that lets the user "reply" to the person whose profile they're looking at
                        // but not to any particular post of theirs
                        this.replyPostID = postID
                    }
                    var startText = ""
                    startText += "@$author "
                    val mentions = intent.getStringArrayListExtra("mentions")
                    if (mentions != null) {
                        for (mention in mentions) {
                            startText += "$mention "
                        }
                    }
                    Log.i("NewPost", "id: ${this.replyPostID}")
                    editText.setText(startText)
                }
            }
        }
    }

    private fun handleSendText(intent: Intent) {
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
        intent.getStringExtra(Intent.EXTRA_SUBJECT)?.let {
            editText.setText("[$it]($text)")
            return
        }
        editText.setText(text)
    }

    private fun handleSendImage(intent: Intent) {
        (intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)?.let {
            val image = imageFrom(it)
            val loadingImg = LoadingImagePreview(this, it)
            image_previews_layout.addView(loadingImg.view, 0)
            postImage(image, loadingImg)
        }
    }

    private fun handleSendMultipleImages(intent: Intent) {
        intent.getParcelableArrayListExtra<Parcelable>(Intent.EXTRA_STREAM)?.let {
            val uris: List<Uri> = it.filterIsInstance<Uri>()
            var imagesLoaded = 0
            for (uri in uris) {
                val image = imageFrom(uri)
                val loadingImg = LoadingImagePreview(this, uri)
                image_previews_layout.addView(loadingImg.view, 0)
                postImage(image, loadingImg)
                imagesLoaded += 1
                if (imagesLoaded > 3) {
                    break
                }
            }
        }
    }

    private fun submitPost(view: View) {
        this.progress = spinner("Posting...")
        this.progress?.show()

        val text = editText.text.toString()
        val queue = Volley.newRequestQueue(this)
        val postUrl = "https://micro.blog/micropub"
        val replyUrl = "https://micro.blog/posts/reply"
        var url = postUrl

        // Use the reply URL with post ID appended if this post is a reply
        if (this.replyPostID != null) {
            url = replyUrl + "?id=$replyPostID"
        }

        val rq = object : StringRequest(
                Request.Method.POST,
                url,
                Response.Listener<String> { response ->
                    Log.i("MainActivity", "resp: $response")
                    this.progress?.hide()
                    Snackbar.make(view, "Success!", Snackbar.LENGTH_LONG).show()
                    editText.setText("")
                    this.progress?.dismiss()
                    val intent = Intent()
                    setResult(Activity.RESULT_OK, intent)
                    this.finish()
                },
                Response.ErrorListener { error ->
                    Log.i("MainActivity", "err: $error msg: ${error.message}")
                    this.progress?.hide()
                    Snackbar.make(view, "Error: $error", Snackbar.LENGTH_LONG).show()
                    // TODO: Handle error
                }) {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                val prefs = this@NewPostActivity.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
                val token: String? = prefs?.getString(TOKEN, null)
                headers["Authorization"] = "Bearer $token"
                return headers
            }

            @Throws(AuthFailureError::class)
            override fun getParams(): Map<String, String> {
                Log.i("MainActivity", "getParams")
                val params = HashMap<String, String>()
                params["h"] = "entry"
                // If this is a reply, the content param name has to be "text" instead of "content" like a normal post
                if (this@NewPostActivity.replyPostID != null) {
                    params["text"] = text
                } else {
                    params["content"] = text
                }
                return params
            }
        }
        // set timeout to zero so Volley won't send multiple of the same request
        // seems like a Volley bug: https://groups.google.com/forum/#!topic/volley-users/8PE9dBbD6iA
        rq.retryPolicy = DefaultRetryPolicy(0, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        queue.add(rq)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            val uri = data.data
            val image = imageFrom(uri)
            val loadingImg = LoadingImagePreview(this, uri)
            buttons_layout.addView(loadingImg.view, 0)
            postImage(image, loadingImg)
        }
    }

    private fun imageFrom(uri: Uri): ByteArray {
        val stream = contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(stream)
        stream.close()
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val image = baos.toByteArray()
        return image
    }

    fun spinner(message: String): ProgressDialog {
        val spinner = ProgressDialog(this)
        spinner.setMessage(message)
        spinner.isIndeterminate = true
        return spinner
    }

    private fun postImage(image: ByteArray, imgPreview: LoadingImagePreview) {
//        this.progress = spinner("Uploading...")
//        this.progress?.show()
        Log.i("NewPostAct", "about to upload image")
        getMediaEndpoint(image, imgPreview)
    }

    fun uploadImage(endpoint: String, image: ByteArray, imgPreview: LoadingImagePreview) {
        val headers = HashMap<String, String>()
        val prefs = this@NewPostActivity.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
        val token: String? = prefs?.getString(TOKEN, null)
        headers["Authorization"] = "Bearer $token"

        val rq = MultipartRequest(endpoint,
                headers,
                Response.Listener {
                    if (it != null) {
                        val data = String(it.data)
                        Log.i("MainActivity", "Success! Resp: $data")
                        val obj = JSONObject(data)
                        if (obj["url"] != null) {
                            this.imagesUploaded += 1
                            sendButton.isEnabled = true
                            val imgURL = obj["url"] as String
                            editText.append("\n\n![]($imgURL)")
                            Log.i("NewPostAct", "done uploading image, progress: ${this.progress}")
                            imgPreview.stopLoading()
                            Snackbar.make(editText.rootView, "Attached image to your post.", Snackbar.LENGTH_SHORT).show()
                            editText.requestFocus()
                        }
                    }
                },
                Response.ErrorListener {
                    this.progress?.hide()
                    if (it.networkResponse != null) {
                        Log.i("MainActivity", "Error: ${String(it.networkResponse.data)}")
                    } else {
                        Log.i("MainActivity", "Error without network response: ${it.message}")
                    }
                })
        rq.addPart(MultipartRequest.FilePart("file", "image/jpeg", "file", image))
        val queue = Volley.newRequestQueue(this)
        queue.add(rq)
    }

    private fun getMediaEndpoint(image: ByteArray, imgPreview: LoadingImagePreview) {
        val url = "https://micro.blog/micropub?q=config"
        val rq = object : StringRequest(
                Request.Method.GET,
                url,
                Response.Listener<String> { response ->
                    Log.i("MainActivity", "resp: $response")
                    val json = JSONObject(response)
                    val endpoint = json["media-endpoint"] as String
                    uploadImage(endpoint, image, imgPreview)
                },
                Response.ErrorListener { error ->
                    Log.i("MainActivity", "err: $error msg: ${error.message}")
                    this.progress?.hide()
                    // TODO: Handle error
                }) {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                val prefs = this@NewPostActivity.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
                val token: String? = prefs?.getString(TOKEN, null)
                headers["Authorization"] = "Bearer $token"
                return headers
            }

            @Throws(AuthFailureError::class)
            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["h"] = "entry"
                return params
            }
        }
        val queue = Volley.newRequestQueue(this)
        queue.add(rq)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
//        menuInflater.inflate(R.menu.menu_main, menu)
//        return true
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}
