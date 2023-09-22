package com.barbenheimer.sign

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HistoryDataAdapter(private val cardDataList: List<HistoryData>) : RecyclerView.Adapter<HistoryDataAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.textViewTitle)
        // Add other views as needed
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.history_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val cardData = cardDataList[position]

        holder.titleTextView.text = cardData.translation
        // Bind other data to views as needed
    }

    override fun getItemCount(): Int {
        return cardDataList.size
    }
}