package co.hellocode.micro.models

import android.content.Context
import android.text.Html
import android.text.Spanned
import android.util.Log
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import android.text.Spannable
import co.hellocode.micro.utils.CustomQuoteSpan
import co.hellocode.micro.utils.HtmlTagHandler
import co.hellocode.micro.utils.URLSpanNoUnderline
import java.util.regex.Pattern
import kotlin.collections.ArrayList


class Post(val item: JSONObject) {
    val ID: Int
    val url: String
    val content: String
    val html: Spanned
    val authorName: String
    val authorAvatarURL: String
    val username: String
    var isConversation: Boolean = false
    val date: Date
    val mentions: ArrayList<String>
    val imageSources: ArrayList<String> = ArrayList()

    init {
        this.ID = (item["id"] as String).toInt()
        this.url = item["url"] as String
        val text = item["content_html"] as String
        content = text.trim()
        this.html = Html.fromHtml(content)
        val mentions: ArrayList<String> = arrayListOf()
        val regex = Regex("[@]\\w+")
        val all = regex.findAll(text)
        for (match in all) {
            mentions.add(match.value)
        }
        this.mentions = mentions
        val datePublished = item["date_published"] as String
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ssZ", Locale.ENGLISH)
        this.date = dateFormat.parse(datePublished)
        val author = (item["author"] as JSONObject)
        this.authorName = author.getString("name")
        this.authorAvatarURL = author.getString("avatar")
        this.username = (author["_microblog"] as JSONObject).getString("username")
        val microblogData = (item["_microblog"] as JSONObject)
        this.isConversation = microblogData.getBoolean("is_conversation")

    }

    fun getParsedContent(c: Context) : Spannable {
        // remove images for now
        var parsed = content //.replaceAll("<img .*/>", "");

        // find images and store sources to add and download
        val patt2 = Pattern.compile("<img\\s*(\\w+=\"([^\"]+)\"[^>]\\s*)*src=\"([^\"]+)\"[^>]*>", Pattern.DOTALL or Pattern.CASE_INSENSITIVE)
        val m2 = patt2.matcher(parsed)
        imageSources.clear()
        while (m2.find()) {
            Log.i("image parsing", m2.group(0))
            imageSources.add(Html.fromHtml(m2.group(3)).toString())
            parsed = parsed.replace(m2.group(0), "")

        }

        // replace tag links with coloured tags
//        val patt = Pattern.compile("<a class=\"tag\" [^>]*><span class=\"hash\">#</span>([^<]*)</a>")
//        val m = patt.matcher(parsed)
//        while (m.find()) {
//            Log.i("parsing", m.group(0))
//            parsed = parsed.replace(m.group(0), "<font color=\"#a05b7f\">#</font>" + m.group(1))
//        }
        imageSources.reverse()

        var newContent: Spannable

        try {
            newContent = Html.fromHtml(parsed, null, HtmlTagHandler(c)) as Spannable
        } catch (e: RuntimeException) {
            Log.e("html", "failed to parse", e)
            Log.d("html", parsed)
            newContent = Html.fromHtml("<p>Error parsing content</p>", null, HtmlTagHandler(c)) as Spannable
        }

        newContent = trimTrailingWhitespace(newContent) as Spannable
        newContent = URLSpanNoUnderline.removeUnderlines(newContent, c)
        CustomQuoteSpan.replaceQuoteSpans(newContent, c)


        return newContent
    }

    fun trimTrailingWhitespace(source: CharSequence?): CharSequence {

        if (source == null)
            return ""

        var i = source.length

        // loop back to the first non-whitespace character
        while (--i >= 0 && Character.isWhitespace(source[i])) {
        }

        return source.subSequence(0, i + 1)
    }

//    val imageGetter = Html.ImageGetter {
//        for (img in it.getSpans(0,
//                it.length(), ImageSpan::class.java)) {
//            Picasso.get().load(img.source)
//        }
//    }
}