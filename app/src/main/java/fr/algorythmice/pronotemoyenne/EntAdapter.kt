package fr.algorythmice.pronotemoyenne

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter

class EntAdapter(
    context: Context,
    layoutRes: Int,
    private val allItems: List<String>
) : ArrayAdapter<String>(context, layoutRes, ArrayList(allItems)) {

    private val filteredItems = ArrayList<String>()

    override fun getFilter(): Filter {
        return object : Filter() {

            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()
                filteredItems.clear()

                if (constraint.isNullOrBlank()) {
                    filteredItems.addAll(allItems)
                } else {
                    val query = constraint.toString().lowercase()
                    filteredItems.addAll(
                        allItems.filter {
                            it.lowercase().contains(query)
                        }
                    )
                }

                results.values = filteredItems
                results.count = filteredItems.size
                return results
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                clear()
                if (results?.values is List<*>) {
                    @Suppress("UNCHECKED_CAST")
                    addAll(results.values as List<String>)
                }
                notifyDataSetChanged()
            }
        }
    }
}
