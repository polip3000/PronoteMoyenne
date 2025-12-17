package fr.algorythmice.pronotemoyenne

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EtablissementAdapter(
    private var list: List<Etablissement>,
    private val onItemClick: (Etablissement) -> Unit
) : RecyclerView.Adapter<EtablissementAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameText: TextView = itemView.findViewById(android.R.id.text1)
        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(list[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.nameText.text = list[position].appellationOfficielle
    }

    fun updateList(newList: List<Etablissement>) {
        list = newList
        notifyDataSetChanged()
    }
}
