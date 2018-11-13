package co.hellocode.micro

import android.app.ActionBar
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import com.squareup.picasso.Picasso

class LoadingImagePreview(context: Context, image: Uri) {
    val spinner = ProgressBar(context)
    val view = RelativeLayout(context)
    val imgView = ImageView(context)

    init {
        this.view.addView(imgView)
        this.imgView.imageAlpha = 160
        val p = imgView.layoutParams as ViewGroup.MarginLayoutParams
        p.setMargins(10, 10, 10, 10)
        this.view.addView(this.spinner, 70, 70)
        val params = RelativeLayout.LayoutParams(70, 70)
        params.addRule(RelativeLayout.CENTER_IN_PARENT)
        this.spinner.indeterminateDrawable.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
        this.spinner.layoutParams = params
        this.spinner.id = View.generateViewId()
        this.spinner.bringToFront()
        this.spinner.isIndeterminate = true
        Picasso.get().load(image).resize(110, 110).into(this.imgView)
    }

    fun stopLoading() {
        this.imgView.imageAlpha = 255
        this.spinner.visibility = View.GONE
    }
}