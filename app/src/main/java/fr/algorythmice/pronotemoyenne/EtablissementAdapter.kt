package fr.algorythmice.pronotemoyenne

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EtablissementAdapter(
    private var list: List<Establishment>,
    private val onItemClick: (Establishment) -> Unit
) : RecyclerView.Adapter<EtablissementAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameText: TextView = itemView.findViewById(R.id.nameText)
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
            .inflate(R.layout.item_etablissement, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.nameText.text = list[position].officialName
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newList: List<Establishment>) {
        list = newList
        notifyDataSetChanged()
    }
}
