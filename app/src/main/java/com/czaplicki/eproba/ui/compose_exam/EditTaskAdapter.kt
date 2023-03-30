package com.czaplicki.eproba.ui.compose_exam

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.graphics.Color
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.czaplicki.eproba.R
import com.google.android.material.textfield.TextInputLayout
import java.util.Collections


class EditTaskAdapter(
    private val dataSet: MutableList<String>,
    private val mStartDragListener: StartDragListener
) :
    RecyclerView.Adapter<EditTaskAdapter.ViewHolder>(), ItemMoveCallback.ItemTouchHelperContract {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val taskIndex: TextView
        val taskField: TextInputLayout
        val rowView: View
        val dragHandle: View
        val deleteTaskIcon: ImageView

        init {
            // Define click listener for the ViewHolder's View
            rowView = view
            taskIndex = view.findViewById(R.id.task_index)
            taskField = view.findViewById(R.id.task_name)
            dragHandle = view.findViewById(R.id.drag_handle)
            deleteTaskIcon = view.findViewById(R.id.delete_task_icon)
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.edit_task_item, viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        val taskTextWatcher: TextWatcher = object : TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                dataSet[viewHolder.adapterPosition] = s.toString()
            }

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        }

        viewHolder.taskField.editText?.addTextChangedListener(taskTextWatcher)

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        viewHolder.taskIndex.text = "${position + 1}."
        viewHolder.taskField.editText?.setText(dataSet[position])

        viewHolder.dragHandle.setOnLongClickListener {
            mStartDragListener.requestDrag(viewHolder)
            true
        }

        viewHolder.deleteTaskIcon.setOnClickListener {
            viewHolder.taskField.editText?.removeTextChangedListener(taskTextWatcher)
            dataSet.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, dataSet.size)
        }

    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size


    override fun onRowMoved(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(dataSet, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(dataSet, i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition)
        notifyItemChanged(fromPosition)
        notifyItemChanged(toPosition)
    }

    override fun onRowSelected(myViewHolder: ViewHolder?) {
        val colorFrom: Int = Color.TRANSPARENT
        val colorTo: Int = Color.parseColor("#66888888")
        val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)
        colorAnimation.duration = 100

        colorAnimation.addUpdateListener { animator ->
            myViewHolder?.rowView?.setBackgroundColor(animator.animatedValue as Int)
        }
        colorAnimation.start()
    }

    override fun onRowClear(myViewHolder: ViewHolder?) {
        val colorFrom: Int = Color.parseColor("#66888888")
        val colorTo: Int = Color.TRANSPARENT
        val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)
        colorAnimation.duration = 100

        colorAnimation.addUpdateListener { animator ->
            myViewHolder?.rowView?.setBackgroundColor(animator.animatedValue as Int)
        }
        colorAnimation.start()
    }

    fun addTask() {
        dataSet.add("")
        notifyItemInserted(dataSet.size - 1)
    }

    fun getTasks(): List<String> = dataSet.filter { it.isNotBlank() }


}
