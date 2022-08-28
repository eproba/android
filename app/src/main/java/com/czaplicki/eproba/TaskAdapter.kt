package com.czaplicki.eproba

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.time.format.DateTimeFormatter

class TaskAdapter(private val dataSet: List<Task>, private val users: List<User>) :
    RecyclerView.Adapter<TaskAdapter.ViewHolder>() {

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val task: TextView
        val description: TextView
        val status: ImageView

        init {
            // Define click listener for the ViewHolder's View.
            task = view.findViewById(R.id.task)
            description = view.findViewById(R.id.description)
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

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        viewHolder.task.text = dataSet[position].task
        if (dataSet[position].description.isNotEmpty()) {
            viewHolder.description.visibility = View.VISIBLE
            viewHolder.description.text = dataSet[position].description
        }
        viewHolder.status.setImageIcon(dataSet[position].statusIcon)
        val approver =
            if (dataSet[position].approver != null) users.find { it.id == dataSet[position].approver } else null
        when (dataSet[position].status) {
            Task.Status.AWAITING_APPROVAL -> {
                viewHolder.status.tooltipText = viewHolder.itemView.context.getString(
                    R.string.awaiting_approval,
                    dataSet[position].approvalDate?.format(DateTimeFormatter.ofPattern("dd LLLL, yyyy")),
                    dataSet[position].approvalDate?.format(DateTimeFormatter.ofPattern("HH:MM")),
                    approver?.nickname ?: viewHolder.itemView.context.getString(R.string.someone)
                )
            }
            Task.Status.APPROVED -> {
                viewHolder.status.tooltipText = viewHolder.itemView.context.getString(
                    R.string.approved,
                    dataSet[position].approvalDate?.format(DateTimeFormatter.ofPattern("dd LLLL, yyyy")),
                    dataSet[position].approvalDate?.format(DateTimeFormatter.ofPattern("HH:MM")),
                    approver?.nickname ?: viewHolder.itemView.context.getString(R.string.someone)
                )
            }
            Task.Status.REJECTED -> {
                viewHolder.status.tooltipText = viewHolder.itemView.context.getString(
                    R.string.rejected,
                    dataSet[position].approvalDate?.format(DateTimeFormatter.ofPattern("dd LLLL, yyyy")),
                    dataSet[position].approvalDate?.format(DateTimeFormatter.ofPattern("HH:MM")),
                    approver?.nickname ?: viewHolder.itemView.context.getString(R.string.someone)
                )
            }
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size
}