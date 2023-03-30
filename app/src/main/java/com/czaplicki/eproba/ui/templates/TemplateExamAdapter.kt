package com.czaplicki.eproba.ui.templates

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.czaplicki.eproba.R
import com.czaplicki.eproba.db.Exam
import com.google.android.material.divider.MaterialDividerItemDecoration
import com.google.gson.Gson


class TemplateExamAdapter(private var dataSet: MutableList<Exam>) :
    RecyclerView.Adapter<TemplateExamAdapter.ViewHolder>() {


    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView
        val taskList: RecyclerView
        val useTemplateButton: Button

        init {
            name = view.findViewById(R.id.name)
            taskList = view.findViewById(R.id.task_list) as RecyclerView
            useTemplateButton = view.findViewById(R.id.use_template_button)
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.template_exam_item, viewGroup, false)
        val viewHolder = ViewHolder(view)

        viewHolder.taskList.layoutManager = LinearLayoutManager(viewHolder.itemView.context)
        val mDividerItemDecoration = MaterialDividerItemDecoration(
            viewHolder.taskList.context,
            LinearLayoutManager(viewHolder.itemView.context).orientation
        )
        viewHolder.taskList.addItemDecoration(mDividerItemDecoration)
        return viewHolder
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        if (dataSet[position].id == -1L && dataSet[position].name == "no_exams") {
            viewHolder.name.text = viewHolder.itemView.context.getString(R.string.no_exams)
            viewHolder.taskList.visibility = View.GONE
            return
        }
        viewHolder.name.text = dataSet[position].name
        viewHolder.taskList.visibility = View.VISIBLE
        viewHolder.taskList.adapter = TemplateTaskAdapter(dataSet[position])

        viewHolder.useTemplateButton.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("initialData", Gson().toJson(dataSet[position]))
            findNavController(viewHolder.itemView).navigate(R.id.navigation_compose, bundle)
        }

    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size


    fun filterList(filterList: List<Exam>) {
        dataSet.clear()
        dataSet.addAll(filterList)
        notifyDataSetChanged()
    }
}