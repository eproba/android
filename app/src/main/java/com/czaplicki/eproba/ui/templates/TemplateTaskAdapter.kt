package com.czaplicki.eproba.ui.templates

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.czaplicki.eproba.R
import com.czaplicki.eproba.db.Worksheet

class TemplateTaskAdapter(private val worksheet: Worksheet) :
    RecyclerView.Adapter<TemplateTaskAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val task: TextView
        val status: ImageView

        init {
            // Define click listener for the ViewHolder's View.
            task = view.findViewById(R.id.task)
            status = view.findViewById(R.id.status)
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.task_item, viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.task.text = worksheet.tasks[position].task
        viewHolder.status.visibility = View.GONE
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = worksheet.tasks.size
}