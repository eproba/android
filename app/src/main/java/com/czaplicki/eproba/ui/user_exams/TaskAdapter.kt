package com.czaplicki.eproba.ui.user_exams

import android.graphics.drawable.Icon
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.czaplicki.eproba.EprobaApplication
import com.czaplicki.eproba.R
import com.czaplicki.eproba.api.EprobaService
import com.czaplicki.eproba.db.Exam
import com.czaplicki.eproba.db.Task
import com.czaplicki.eproba.db.User
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class TaskAdapter(
    private val exam: Exam,
    private val users: List<User>,
    var service: EprobaService
) :
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
        val user = Gson().fromJson(
            PreferenceManager.getDefaultSharedPreferences(
                viewHolder.itemView.context
            ).getString("user", null),
            User::class.java
        )
        viewHolder.task.text = exam.tasks[position].task
        if (exam.tasks[position].description.isNotEmpty()) {
            viewHolder.description.visibility = View.VISIBLE
            viewHolder.description.text = exam.tasks[position].description
        }
        viewHolder.status.setImageIcon(exam.tasks[position].statusIcon)
        val approver =
            if (exam.tasks[position].approver != null) users.find { it.id == exam.tasks[position].approver } else null
        when (exam.tasks[position].status) {
            Task.Status.AWAITING_APPROVAL -> {
                viewHolder.status.tooltipText = viewHolder.itemView.context.getString(
                    R.string.awaiting_approval,
                    exam.tasks[position].approvalDate?.let {
                        DateTimeFormatter.ofPattern("dd LLLL, yyyy").format(it)
                    } ?: viewHolder.itemView.context.getString(R.string.sometime),
                    exam.tasks[position].approvalDate?.let {
                        DateTimeFormatter.ofPattern("HH:mm").format(it)
                    } ?: viewHolder.itemView.context.getString(R.string.sometime),
                    approver?.nickname ?: viewHolder.itemView.context.getString(R.string.someone)
                )
            }

            Task.Status.APPROVED -> {
                viewHolder.status.tooltipText = viewHolder.itemView.context.getString(
                    R.string.approved,
                    exam.tasks[position].approvalDate?.let {
                        DateTimeFormatter.ofPattern("dd LLLL, yyyy").format(it)
                    } ?: viewHolder.itemView.context.getString(R.string.sometime),
                    exam.tasks[position].approvalDate?.let {
                        DateTimeFormatter.ofPattern("HH:mm").format(it)
                    } ?: viewHolder.itemView.context.getString(R.string.sometime),
                    approver?.nickname ?: viewHolder.itemView.context.getString(R.string.someone)
                )
            }

            Task.Status.REJECTED -> {
                viewHolder.status.tooltipText = viewHolder.itemView.context.getString(
                    R.string.rejected,
                    exam.tasks[position].approvalDate?.let {
                        DateTimeFormatter.ofPattern("dd LLLL, yyyy").format(it)
                    } ?: viewHolder.itemView.context.getString(R.string.sometime),
                    exam.tasks[position].approvalDate?.let {
                        DateTimeFormatter.ofPattern("HH:mm").format(it)
                    } ?: viewHolder.itemView.context.getString(R.string.sometime),
                    approver?.nickname ?: viewHolder.itemView.context.getString(R.string.someone)
                )
            }
        }
        viewHolder.itemView.setOnClickListener {
            var approverCandidates: Array<String>? = null
            val builder = MaterialAlertDialogBuilder(
                viewHolder.itemView.context,
                com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
            )
                .setTitle(exam.name + " - " + users.find { it.id == exam.userId }?.nicknameWithRank)
                .setMessage(exam.tasks[position].task + if (exam.tasks[position].description.isNotEmpty()) "\n\n" + exam.tasks[position].description else "")
                .setIcon(viewHolder.status.drawable)
                .setNeutralButton(R.string.close) { dialog, _ ->
                    dialog.dismiss()
                }
            if (exam.tasks[position].status == Task.Status.AWAITING_APPROVAL) {
                builder.setNegativeButton(R.string.unsubmit_task) { dialog, _ ->
                    GlobalScope.launch {
                        try {
                            service.unsubmitTask(exam.id, exam.tasks[position].id)
                            exam.tasks[viewHolder.adapterPosition].status =
                                Task.Status.PENDING
                            exam.tasks[viewHolder.adapterPosition].approver = user.id
                            exam.tasks[viewHolder.adapterPosition].approvalDate = null
                            viewHolder.status.setImageIcon(
                                Icon.createWithResource(
                                    "com.czaplicki.eproba",
                                    R.drawable.radio_button_unchecked_24px
                                )
                            )
                            viewHolder.status.tooltipText = null
                            dialog.dismiss()
                        } catch (e: Exception) {
                            EprobaApplication.instance.currentActivity?.runOnUiThread {
                                MaterialAlertDialogBuilder(
                                    viewHolder.itemView.context,
                                    com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
                                )
                                    .setTitle(R.string.error_dialog_title)
                                    .setIcon(R.drawable.ic_error)
                                    .setMessage(R.string.task_unsubmission_error_message)
                                    .setPositiveButton(android.R.string.ok, null)
                                    .show()
                            }
                            dialog.dismiss()
                        }
                    }
                }
            } else if (exam.tasks[position].status == Task.Status.PENDING || exam.tasks[position].status == Task.Status.REJECTED) {
                approverCandidates = users.filter {
                    !it.nickname.isNullOrBlank() && ((it.id != user.id && it.scout.function > 2 && it.scout.teamId == user.scout.teamId && it.scout.function > user.scout.function) || exam.supervisor == it.id)
                }.map { it.nicknameWithRank }.toTypedArray()
                builder.setPositiveButton(R.string.submit_task) { dialog, _ ->
                    dialog.dismiss()
                    val approverSelect: View = LayoutInflater.from(viewHolder.itemView.context)
                        .inflate(
                            R.layout.select_task_approver_alert,
                            viewHolder.itemView.parent as? ViewGroup,
                            false
                        )

                    (approverSelect.findViewById<TextInputLayout>(R.id.approver_select).editText as MaterialAutoCompleteTextView)
                        .setSimpleItems(approverCandidates)
                    if (approverCandidates.size == 1) {
                        (approverSelect.findViewById<TextInputLayout>(R.id.approver_select).editText as MaterialAutoCompleteTextView)
                            .setText(approverCandidates[0], false)
                    }
                    val approverDialogBuilder =
                        MaterialAlertDialogBuilder(viewHolder.itemView.context)
                            .setTitle(R.string.select_approver)
                            .setView(approverSelect)
                            .setPositiveButton(R.string.submit) Submit@{ _, _ ->
                                val selectedApprover = users.find {
                                    it.nicknameWithRank.lowercase() == (approverSelect.findViewById<TextInputLayout>(
                                        R.id.approver_select
                                    ).editText as MaterialAutoCompleteTextView).text.toString()
                                        .lowercase()
                                } ?: users.find {
                                    it.nickname?.lowercase() == (approverSelect.findViewById<TextInputLayout>(
                                        R.id.approver_select
                                    ).editText as MaterialAutoCompleteTextView).text.toString()
                                        .lowercase()
                                }
                                if (selectedApprover == null) {
                                    MaterialAlertDialogBuilder(
                                        viewHolder.itemView.context,
                                        com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
                                    )
                                        .setTitle(R.string.error_dialog_title)
                                        .setIcon(R.drawable.ic_error)
                                        .setMessage(R.string.task_submission_error_message)
                                        .setPositiveButton(android.R.string.ok, null)
                                        .show()
                                    return@Submit
                                }
                                GlobalScope.launch {
                                    try {
                                        service.submitTask(
                                            exam.id,
                                            exam.tasks[position].id,
                                            "{\"approver\": ${selectedApprover.id}}".toRequestBody("application/json".toMediaTypeOrNull())
                                        )
                                        exam.tasks[viewHolder.adapterPosition].status =
                                            Task.Status.AWAITING_APPROVAL
                                        exam.tasks[viewHolder.adapterPosition].approver =
                                            user.id
                                        exam.tasks[viewHolder.adapterPosition].approvalDate =
                                            ZonedDateTime.now()
                                        viewHolder.status.setImageIcon(
                                            Icon.createWithResource(
                                                "com.czaplicki.eproba",
                                                R.drawable.schedule_24px
                                            )
                                        )
                                        viewHolder.status.tooltipText =
                                            viewHolder.itemView.context.getString(
                                                R.string.awaiting_approval,
                                                DateTimeFormatter.ofPattern("dd LLLL, yyyy")
                                                    .format(ZonedDateTime.now()),
                                                DateTimeFormatter.ofPattern("HH:mm")
                                                    .format(ZonedDateTime.now()),
                                                selectedApprover.nickname
                                            )
                                    } catch (e: Exception) {
                                        EprobaApplication.instance.currentActivity?.runOnUiThread {
                                            MaterialAlertDialogBuilder(
                                                viewHolder.itemView.context,
                                                com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
                                            )
                                                .setTitle(R.string.error_dialog_title)
                                                .setIcon(R.drawable.ic_error)
                                                .setMessage(R.string.task_submission_error_message)
                                                .setPositiveButton(android.R.string.ok, null)
                                                .show()
                                        }
                                        dialog.dismiss()
                                    }
                                }
                            }
                            .setNeutralButton(R.string.cancel) { dialog_2, _ ->
                                dialog_2.dismiss()
                            }
                            .show()
                    approverDialogBuilder.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled =
                        approverCandidates.size == 1
                    (approverSelect.findViewById<TextInputLayout>(R.id.approver_select).editText as MaterialAutoCompleteTextView).addTextChangedListener(
                        object : TextWatcher {
                            override fun beforeTextChanged(
                                s: CharSequence?,
                                start: Int,
                                count: Int,
                                after: Int
                            ) {
                            }

                            override fun onTextChanged(
                                s: CharSequence?,
                                start: Int,
                                before: Int,
                                count: Int
                            ) {
                            }

                            override fun afterTextChanged(s: Editable?) {
                                approverDialogBuilder.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled =
                                    users.find {
                                        !it.nickname.isNullOrBlank()
                                                && (it.nicknameWithRank.lowercase() == s.toString()
                                            .lowercase() || it.nickname.lowercase() == s.toString()
                                            .lowercase())
                                                && (it.id != user.id
                                                && it.scout.function > user.scout.function
                                                && it.scout.function >= 2
                                                || exam.supervisor == it.id)
                                    } != null
                            }
                        })
                }
            }
            builder.show().getButton(AlertDialog.BUTTON_POSITIVE).isEnabled =
                !((exam.tasks[position].status == Task.Status.PENDING || exam.tasks[position].status == Task.Status.REJECTED) && approverCandidates?.isEmpty() == true)
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = exam.tasks.size
}