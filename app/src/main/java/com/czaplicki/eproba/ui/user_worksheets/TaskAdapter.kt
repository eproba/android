package com.czaplicki.eproba.ui.user_worksheets

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
import com.czaplicki.eproba.db.Worksheet
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
    private val worksheet: Worksheet,
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
        viewHolder.task.text = worksheet.tasks[position].task
        if (worksheet.tasks[position].description.isNotEmpty()) {
            viewHolder.description.visibility = View.VISIBLE
            viewHolder.description.text = worksheet.tasks[position].description
        }
        viewHolder.status.setImageIcon(worksheet.tasks[position].statusIcon)
        val approver =
            if (worksheet.tasks[position].approver != null) users.find { it.id == worksheet.tasks[position].approver } else null
        when (worksheet.tasks[position].status) {
            Task.Status.AWAITING_APPROVAL -> {
                viewHolder.status.tooltipText = viewHolder.itemView.context.getString(
                    R.string.awaiting_approval,
                    worksheet.tasks[position].approvalDate?.let {
                        DateTimeFormatter.ofPattern("dd LLLL, yyyy").format(it)
                    } ?: viewHolder.itemView.context.getString(R.string.sometime),
                    worksheet.tasks[position].approvalDate?.let {
                        DateTimeFormatter.ofPattern("HH:mm").format(it)
                    } ?: viewHolder.itemView.context.getString(R.string.sometime),
                    approver?.nickname ?: viewHolder.itemView.context.getString(R.string.someone)
                )
            }

            Task.Status.APPROVED -> {
                viewHolder.status.tooltipText = viewHolder.itemView.context.getString(
                    R.string.approved,
                    worksheet.tasks[position].approvalDate?.let {
                        DateTimeFormatter.ofPattern("dd LLLL, yyyy").format(it)
                    } ?: viewHolder.itemView.context.getString(R.string.sometime),
                    worksheet.tasks[position].approvalDate?.let {
                        DateTimeFormatter.ofPattern("HH:mm").format(it)
                    } ?: viewHolder.itemView.context.getString(R.string.sometime),
                    approver?.nickname ?: viewHolder.itemView.context.getString(R.string.someone)
                )
            }

            Task.Status.REJECTED -> {
                viewHolder.status.tooltipText = viewHolder.itemView.context.getString(
                    R.string.rejected,
                    worksheet.tasks[position].approvalDate?.let {
                        DateTimeFormatter.ofPattern("dd LLLL, yyyy").format(it)
                    } ?: viewHolder.itemView.context.getString(R.string.sometime),
                    worksheet.tasks[position].approvalDate?.let {
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
                .setTitle(worksheet.name + " - " + users.find { it.id == worksheet.userId }?.nicknameWithRank)
                .setMessage(worksheet.tasks[position].task + if (worksheet.tasks[position].description.isNotEmpty()) "\n\n" + worksheet.tasks[position].description else "")
                .setIcon(viewHolder.status.drawable)
                .setNeutralButton(R.string.close) { dialog, _ ->
                    dialog.dismiss()
                }
            if (worksheet.tasks[position].status == Task.Status.AWAITING_APPROVAL) {
                builder.setNegativeButton(R.string.unsubmit_task) { dialog, _ ->
                    GlobalScope.launch {
                        try {
                            service.unsubmitTask(worksheet.id, worksheet.tasks[position].id)
                            worksheet.tasks[viewHolder.adapterPosition].status =
                                Task.Status.PENDING
                            worksheet.tasks[viewHolder.adapterPosition].approver = user.id
                            worksheet.tasks[viewHolder.adapterPosition].approvalDate = null
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
            } else if (worksheet.tasks[position].status == Task.Status.PENDING || worksheet.tasks[position].status == Task.Status.REJECTED) {
                approverCandidates = users.filter {
                    !it.nickname.isNullOrBlank() && ((it.id != user.id && it.function > 2 && it.teamId == user.teamId && it.function > user.function) || worksheet.supervisor == it.id)
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
                                            worksheet.id,
                                            worksheet.tasks[position].id,
                                            "{\"approver\": \"${selectedApprover.id}\"}".toRequestBody("application/json".toMediaTypeOrNull())
                                        )
                                        worksheet.tasks[viewHolder.adapterPosition].status =
                                            Task.Status.AWAITING_APPROVAL
                                        worksheet.tasks[viewHolder.adapterPosition].approver =
                                            user.id
                                        worksheet.tasks[viewHolder.adapterPosition].approvalDate =
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
                                                && it.function > user.function
                                                && it.function >= 2
                                                || worksheet.supervisor == it.id)
                                    } != null
                            }
                        })
                }
            }
            builder.show().getButton(AlertDialog.BUTTON_POSITIVE).isEnabled =
                !((worksheet.tasks[position].status == Task.Status.PENDING || worksheet.tasks[position].status == Task.Status.REJECTED) && approverCandidates?.isEmpty() == true)
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = worksheet.tasks.size
}