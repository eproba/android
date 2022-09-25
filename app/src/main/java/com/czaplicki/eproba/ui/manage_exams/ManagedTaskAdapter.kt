package com.czaplicki.eproba.ui.manage_exams

import android.graphics.drawable.Icon
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.czaplicki.eproba.AuthStateManager
import com.czaplicki.eproba.R
import com.czaplicki.eproba.api.EprobaApi
import com.czaplicki.eproba.api.EprobaService
import com.czaplicki.eproba.db.Exam
import com.czaplicki.eproba.db.Task
import com.czaplicki.eproba.db.User
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import net.openid.appauth.AuthorizationService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class ManagedTaskAdapter(
    private val exam: Exam,
    private val users: List<User>,
    val progressPercentage: TextView
) :
    RecyclerView.Adapter<ManagedTaskAdapter.ViewHolder>() {

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
                    } ?: viewHolder.itemView.context.getString(R.string.some_time),
                    exam.tasks[position].approvalDate?.let {
                        DateTimeFormatter.ofPattern("HH:mm").format(it)
                    } ?: viewHolder.itemView.context.getString(R.string.some_time),
                    approver?.nickname ?: viewHolder.itemView.context.getString(R.string.someone)
                )
            }
            Task.Status.APPROVED -> {
                viewHolder.status.tooltipText = viewHolder.itemView.context.getString(
                    R.string.approved,
                    exam.tasks[position].approvalDate?.let {
                        DateTimeFormatter.ofPattern("dd LLLL, yyyy").format(it)
                    } ?: viewHolder.itemView.context.getString(R.string.some_time),
                    exam.tasks[position].approvalDate?.let {
                        DateTimeFormatter.ofPattern("HH:mm").format(it)
                    } ?: viewHolder.itemView.context.getString(R.string.some_time),
                    approver?.nickname ?: viewHolder.itemView.context.getString(R.string.someone)
                )
            }
            Task.Status.REJECTED -> {
                viewHolder.status.tooltipText = viewHolder.itemView.context.getString(
                    R.string.rejected,
                    exam.tasks[position].approvalDate?.let {
                        DateTimeFormatter.ofPattern("dd LLLL, yyyy").format(it)
                    } ?: viewHolder.itemView.context.getString(R.string.some_time),
                    exam.tasks[position].approvalDate?.let {
                        DateTimeFormatter.ofPattern("HH:mm").format(it)
                    } ?: viewHolder.itemView.context.getString(R.string.some_time),
                    approver?.nickname ?: viewHolder.itemView.context.getString(R.string.someone)
                )
            }
        }
        viewHolder.itemView.setOnClickListener {
            if (PreferenceManager.getDefaultSharedPreferences(viewHolder.itemView.context)
                    .getString(
                        "server",
                        ""
                    ) == "https://eproba.pl" && PreferenceManager.getDefaultSharedPreferences(
                    viewHolder.itemView.context
                ).getString("server_key", "") != "47"
            ) {
                Toast.makeText(
                    viewHolder.itemView.context,
                    "You can't manage tasks on main server",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            MaterialAlertDialogBuilder(
                viewHolder.itemView.context,
                com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
            )
                .setTitle(exam.name + " - " + users.find { it.id == exam.userId }?.nicknameWithRank)
                .setMessage(exam.tasks[position].task + if (exam.tasks[position].description.isNotEmpty()) "\n\n" + exam.tasks[position].description else "")
                .setIcon(viewHolder.status.drawable)
                .setNeutralButton(R.string.cancel) { dialog, _ ->
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.reject) { dialog, _ ->
                    val mAuthStateManager =
                        AuthStateManager.getInstance(viewHolder.itemView.context)
                    val authService = AuthorizationService(viewHolder.itemView.context)
                    mAuthStateManager.current.performActionWithFreshTokens(
                        authService
                    ) { accessToken, _, _ ->
                        if (accessToken == null) return@performActionWithFreshTokens
                        mAuthStateManager.updateSavedState()
                        EprobaApi().create(viewHolder.itemView.context, accessToken)!!
                            .create(
                                EprobaService::class.java
                            ).updateTaskStatus(
                                exam.id!!, exam.tasks[position].id,
                                "{\"status\": ${Task.Status.REJECTED}, \"approver\": ${
                                    Gson().fromJson(
                                        PreferenceManager.getDefaultSharedPreferences(viewHolder.itemView.context)
                                            .getString("user", null),
                                        User::class.java
                                    ).id
                                }, \"approval_date\": \"${
                                    DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now())
                                }\"}"
                                    .toRequestBody("application/json".toMediaTypeOrNull())
                            ).enqueue(object : Callback<Task> {
                                override fun onResponse(
                                    call: Call<Task>,
                                    response: Response<Task>
                                ) {
                                    if (response.isSuccessful) {
                                        exam.tasks[viewHolder.adapterPosition].status =
                                            Task.Status.REJECTED
                                        exam.tasks[viewHolder.adapterPosition].approver =
                                            Gson().fromJson(
                                                PreferenceManager.getDefaultSharedPreferences(
                                                    viewHolder.itemView.context
                                                )
                                                    .getString("user", null), User::class.java
                                            ).id
                                        exam.tasks[viewHolder.adapterPosition].approvalDate =
                                            ZonedDateTime.now()
                                        progressPercentage.text =
                                            viewHolder.itemView.context.getString(
                                                R.string.progress_percentage,
                                                exam.tasks.count { it.status == Task.Status.APPROVED } * 100 / exam.tasks.size)
                                        viewHolder.status.setImageIcon(
                                            Icon.createWithResource(
                                                "com.czaplicki.eproba",
                                                R.drawable.cancel_24px
                                            )
                                        )
                                        viewHolder.status.tooltipText =
                                            viewHolder.itemView.context.getString(
                                                R.string.rejected,
                                                DateTimeFormatter.ofPattern("dd LLLL, yyyy")
                                                    .format(ZonedDateTime.now()),
                                                DateTimeFormatter.ofPattern("HH:mm")
                                                    .format(ZonedDateTime.now()),
                                                Gson().fromJson(
                                                    PreferenceManager.getDefaultSharedPreferences(
                                                        viewHolder.itemView.context
                                                    )
                                                        .getString("user", null), User::class.java
                                                ).nickname
                                            )
                                        dialog.dismiss()
                                    } else {
                                        MaterialAlertDialogBuilder(
                                            viewHolder.itemView.context,
                                            com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
                                        )
                                            .setTitle(R.string.error_dialog_title)
                                            .setIcon(R.drawable.ic_error)
                                            .setMessage(R.string.task_rejection_error_message)
                                            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                                                dialog.dismiss()
                                            }
                                            .show()
                                        dialog.dismiss()
                                    }
                                }

                                override fun onFailure(call: Call<Task>, t: Throwable) {
                                    MaterialAlertDialogBuilder(
                                        viewHolder.itemView.context,
                                        com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
                                    )
                                        .setTitle(R.string.error_dialog_title)
                                        .setIcon(R.drawable.ic_error)
                                        .setMessage(R.string.task_rejection_error_message)
                                        .setPositiveButton(android.R.string.ok) { dialog, _ ->
                                            dialog.dismiss()
                                        }
                                        .show()
                                    dialog.dismiss()
                                }
                            })
                    }
                }
                .setPositiveButton(R.string.accept) { dialog, _ ->
                    val mAuthStateManager =
                        AuthStateManager.getInstance(viewHolder.itemView.context)
                    val authService = AuthorizationService(viewHolder.itemView.context)
                    mAuthStateManager.current.performActionWithFreshTokens(
                        authService
                    ) { accessToken, _, _ ->
                        if (accessToken == null) return@performActionWithFreshTokens
                        mAuthStateManager.updateSavedState()
                        EprobaApi().create(viewHolder.itemView.context, accessToken)!!
                            .create(
                                EprobaService::class.java
                            ).updateTaskStatus(
                                exam.id!!, exam.tasks[position].id,
                                "{\"status\": ${Task.Status.APPROVED}, \"approver\": ${
                                    Gson().fromJson(
                                        PreferenceManager.getDefaultSharedPreferences(viewHolder.itemView.context)
                                            .getString("user", null),
                                        User::class.java
                                    ).id
                                }, \"approval_date\": \"${
                                    DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now())
                                }\"}"
                                    .toRequestBody("application/json".toMediaTypeOrNull())
                            ).enqueue(object : Callback<Task> {
                                override fun onResponse(
                                    call: Call<Task>,
                                    response: Response<Task>
                                ) {
                                    if (response.isSuccessful) {
                                        exam.tasks[viewHolder.adapterPosition].status =
                                            Task.Status.APPROVED
                                        exam.tasks[viewHolder.adapterPosition].approver =
                                            Gson().fromJson(
                                                PreferenceManager.getDefaultSharedPreferences(
                                                    viewHolder.itemView.context
                                                )
                                                    .getString("user", null), User::class.java
                                            ).id
                                        exam.tasks[viewHolder.adapterPosition].approvalDate =
                                            ZonedDateTime.now()
                                        progressPercentage.text =
                                            viewHolder.itemView.context.getString(
                                                R.string.progress_percentage,
                                                exam.tasks.count { it.status == Task.Status.APPROVED } * 100 / exam.tasks.size)
                                        viewHolder.status.setImageIcon(
                                            Icon.createWithResource(
                                                "com.czaplicki.eproba",
                                                R.drawable.check_circle_24px
                                            )
                                        )
                                        viewHolder.status.tooltipText =
                                            viewHolder.itemView.context.getString(
                                                R.string.approved,
                                                DateTimeFormatter.ofPattern("dd LLLL, yyyy")
                                                    .format(ZonedDateTime.now()),
                                                DateTimeFormatter.ofPattern("HH:mm")
                                                    .format(ZonedDateTime.now()),
                                                Gson().fromJson(
                                                    PreferenceManager.getDefaultSharedPreferences(
                                                        viewHolder.itemView.context
                                                    )
                                                        .getString("user", null), User::class.java
                                                ).nickname
                                            )
                                        dialog.dismiss()
                                    } else {
                                        MaterialAlertDialogBuilder(
                                            viewHolder.itemView.context,
                                            com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
                                        )
                                            .setTitle(R.string.error_dialog_title)
                                            .setIcon(R.drawable.ic_error)
                                            .setMessage(R.string.task_approval_error_message)
                                            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                                                dialog.dismiss()
                                            }
                                            .show()
                                        dialog.dismiss()
                                    }
                                }

                                override fun onFailure(call: Call<Task>, t: Throwable) {
                                    MaterialAlertDialogBuilder(
                                        viewHolder.itemView.context,
                                        com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
                                    )
                                        .setTitle(R.string.error_dialog_title)
                                        .setIcon(R.drawable.ic_error)
                                        .setMessage(R.string.task_approval_error_message)
                                        .setPositiveButton(android.R.string.ok) { dialog, _ ->
                                            dialog.dismiss()
                                        }
                                        .show()
                                    dialog.dismiss()
                                }
                            })
                    }
                }
                .show()
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = exam.tasks.size
}