package app.vbt.hyperupnp

import android.view.LayoutInflater
import android.view.SoundEffectConstants
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import app.vbt.hyperupnp.models.CustomListItem
import coil.load
import java.util.*


class CustomListAdapter(
    private var customListItems: ArrayList<CustomListItem>,
    private val onItemClick: (CustomListItem) -> Unit,
    private val onItemLongClick: (CustomListItem) -> Boolean,
) : RecyclerView.Adapter<CustomListAdapter.ViewHolder>(),
    Filterable {

    var isListView: Boolean = false
    var customListFilterList = ArrayList(customListItems)
    private var lastQuery: String = ""

    private fun matches(item: CustomListItem, query: String): Boolean =
        query.isEmpty() || item.title?.lowercase(Locale.ROOT)
            ?.contains(query.lowercase(Locale.ROOT)) == true

    private fun computeFilteredList(query: String): ArrayList<CustomListItem> =
        customListItems.filterTo(ArrayList()) { matches(it, query) }

    /** Re-syncs the displayed list after the source list was mutated. */
    fun refreshList() {
        val newList = computeFilteredList(lastQuery)
        val diffResult =
            DiffUtil.calculateDiff(CustomListDiffCallback(customListFilterList, newList))
        customListFilterList = newList
        diffResult.dispatchUpdatesTo(this)
    }

    /** Call after appending one item to the source list. */
    fun notifyItemAppended() {
        val item = customListItems.lastOrNull() ?: return
        if (matches(item, lastQuery)) {
            customListFilterList.add(item)
            notifyItemInserted(customListFilterList.size - 1)
        }
    }

    class ViewHolder(
        ItemView: View,
        onItemClick: (CustomListItem) -> Unit,
        onItemLongClick: (CustomListItem) -> Boolean
    ) :
        RecyclerView.ViewHolder(ItemView) {
        lateinit var entry: CustomListItem

        init {
            ItemView.setOnClickListener {
                onItemClick(entry)
            }
            ItemView.setOnLongClickListener {
                onItemLongClick(entry)
            }
            ItemView.setOnFocusChangeListener { view, hasFocus ->
                val scale = if (hasFocus) 1.06f else 1.0f
                val elevation = if (hasFocus) 20f else 3f
                view.animate()
                    .scaleX(scale)
                    .scaleY(scale)
                    .z(elevation)
                    .setDuration(160)
                    .setInterpolator(DecelerateInterpolator(1.5f))
                    .start()
                if (hasFocus) {
                    view.playSoundEffect(SoundEffectConstants.NAVIGATION_UP)
                }
            }
        }

        var titleView: TextView = ItemView.findViewById(R.id.title)
        var containerView: RelativeLayout = ItemView.findViewById(R.id.container)
        var descriptionView: TextView = ItemView.findViewById(R.id.description)
        var description2View: TextView = ItemView.findViewById(R.id.description2)
        var imageView: ImageView = ItemView.findViewById(R.id.icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutRes = if (isListView) R.layout.hyperlist else R.layout.hypergrids
        val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        return ViewHolder(view, onItemClick, onItemLongClick)
    }

    override fun getItemViewType(position: Int): Int {
        return if (isListView) 1 else 0
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.titleView.isSelected = true
        holder.entry = customListFilterList[position]

        // Recycled views may hold the full-bleed thumbnail state; restore the
        // padded icon presentation before loading.
        val density = holder.itemView.resources.displayMetrics.density
        val iconPadding = ((if (isListView) 8 else 22) * density).toInt()
        holder.imageView.setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
        holder.imageView.scaleType = ImageView.ScaleType.FIT_CENTER

        holder.imageView.load(holder.entry.iconUrl) {
            crossfade(true)
            placeholder(holder.entry.icon)
            error(holder.entry.icon)
            listener(onSuccess = { _, _ ->
                // Real artwork: drop the icon inset and fill the well edge to edge
                holder.imageView.setPadding(0, 0, 0, 0)
                holder.imageView.scaleType = ImageView.ScaleType.CENTER_CROP
            })
        }

        holder.imageView.visibility = View.VISIBLE
        holder.titleView.text = holder.entry.title

        val description = holder.entry.description
        if (description == null) holder.descriptionView.visibility = View.GONE
        else {
            holder.descriptionView.visibility = View.VISIBLE
            holder.descriptionView.text = description
        }

        val description2 = holder.entry.description2
        if (description2 == null) holder.description2View.visibility = View.GONE
        else {
            holder.description2View.visibility = View.VISIBLE
            holder.description2View.text = description2
        }

        holder.itemView.contentDescription = buildString {
            append(holder.entry.title)
            if (description != null) {
                append(", ")
                append(description)
            }
            if (description2 != null) {
                append(", ")
                append(description2)
            }
        }
    }

    override fun getItemCount(): Int = customListFilterList.size

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val query = constraint?.toString() ?: ""
                lastQuery = query
                val filterResults = FilterResults()
                filterResults.values = computeFilteredList(query)
                return filterResults
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                val newList = results?.values as ArrayList<CustomListItem>
                val diffResult = DiffUtil.calculateDiff(CustomListDiffCallback(customListFilterList, newList))
                customListFilterList = newList
                diffResult.dispatchUpdatesTo(this@CustomListAdapter)
            }
        }
    }

    private class CustomListDiffCallback(
        private val oldList: List<CustomListItem>,
        private val newList: List<CustomListItem>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            return oldItem.id == newItem.id && oldItem.title == newItem.title
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            return oldItem.title == newItem.title
                    && oldItem.description == newItem.description
                    && oldItem.iconUrl == newItem.iconUrl
        }
    }
}
