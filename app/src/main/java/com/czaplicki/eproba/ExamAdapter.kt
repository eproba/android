package com.czaplicki.eproba

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


class ExamAdapter(private val dataSet: List<Exam>) :
    RecyclerView.Adapter<ExamAdapter.ViewHolder>() {

    private var taskList: RecyclerView? = null

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView
        val supervisor: TextView
        val progressPercentage: TextView

        init {
            // Define click listener for the ViewHolder's View.
            name = view.findViewById(R.id.name)
            supervisor = view.findViewById(R.id.supervisor)
            progressPercentage = view.findViewById(R.id.progressPercentage)
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.exam_item, viewGroup, false)


        taskList = view.findViewById(R.id.task_list) as RecyclerView
        taskList!!.layoutManager = LinearLayoutManager(viewGroup.context)
        val mDividerItemDecoration = DividerItemDecoration(
            taskList!!.context,
            LinearLayoutManager(viewGroup.context).orientation
        )
        taskList!!.addItemDecoration(mDividerItemDecoration)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        viewHolder.name.text = dataSet[position].name
        if (dataSet[position].supervisor != null) {
            viewHolder.supervisor.visibility = View.VISIBLE
            viewHolder.supervisor.text = dataSet[position].supervisor.toString()
        }
        viewHolder.progressPercentage.text = dataSet[position].tasks.size.toString() + "%"
        taskList!!.adapter = TaskAdapter(dataSet[position].tasks)
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size
}