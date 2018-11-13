package co.hellocode.micro.tabs.recyclers

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import co.hellocode.micro.utils.inflate

abstract class BaseViewHolder<in T>(parent: ViewGroup, layoutRes: Int,
                                    val rootView: View = parent.inflate(layoutRes))
    : RecyclerView.ViewHolder(rootView) {

    abstract fun bindItem(item: T)
}

class BaseRecyclerAdapter<T>(private val viewHolderFactory: ((parent: ViewGroup) -> BaseViewHolder<T>),
                             private val items: ArrayList<T>)
    : RecyclerView.Adapter<BaseViewHolder<T>>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<T> {
        return viewHolderFactory(parent)
    }

    override fun onBindViewHolder(holder: BaseViewHolder<T>, position: Int) {
        val item = items[position]
        holder.bindItem(item)

    }

    override fun getItemCount(): Int = items.size

}