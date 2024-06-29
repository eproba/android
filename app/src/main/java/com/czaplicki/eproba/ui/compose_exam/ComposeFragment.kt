package com.czaplicki.eproba.ui.compose_exam

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.czaplicki.eproba.CreateExamActivity
import com.czaplicki.eproba.EprobaApplication
import com.czaplicki.eproba.databinding.FragmentComposeBinding
import com.czaplicki.eproba.db.Exam
import com.czaplicki.eproba.db.Task
import com.czaplicki.eproba.db.User
import com.google.android.material.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.gson.Gson
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.net.HttpURLConnection.HTTP_FORBIDDEN


class ComposeFragment : Fragment() {

    private var _binding: FragmentComposeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val user: User = EprobaApplication.instance.apiHelper.user!!
    private var users: List<User> = listOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentComposeBinding.inflate(inflater, container, false)

        lifecycleScope.launch {
            users = EprobaApplication.instance.database.userDao().getAll()
        }

        val examNameFieldTextWatcher: TextWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if (s.toString().isBlank()) {
                    binding.examName.error = getString(com.czaplicki.eproba.R.string.required_field)
                } else {
                    binding.examName.error = null
                }
            }
        }

        val userSelectFieldTextWatcher: TextWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if (s.toString().isBlank()) {
                    binding.userSelect.error =
                        getString(com.czaplicki.eproba.R.string.required_field)
                } else {
                    if (users.any { it.nickname?.lowercase() == s.toString().lowercase() }) {
                        binding.userSelect.error = null
                    } else {
                        binding.userSelect.error =
                            getString(com.czaplicki.eproba.R.string.user_does_not_exist)
                    }
                }
            }
        }

        if (EprobaApplication.instance.apiHelper.user!!.scout.function < 2) {
            binding.userSelect.editText?.setText(user.nickname)
        }

        val initialData: Exam? = Gson().fromJson(
            arguments?.getString("initialData"),
            Exam::class.java
        )

        val root: View = binding.root

        var touchHelper: ItemTouchHelper? = null
        val taskRecyclerView = binding.tasksRecyclerView
        taskRecyclerView.layoutManager = object : LinearLayoutManager(view?.context) {
            override fun canScrollVertically() = false
        }
        if (initialData != null) {
            binding.examName.editText?.setText(initialData.name)
            if (EprobaApplication.instance.apiHelper.user!!.scout.function >= 2) {
                (binding.userSelect.editText as? MaterialAutoCompleteTextView)?.setText(
                    arguments?.getString(
                        "initialDataNickname"
                    ), false
                )
            }
            taskRecyclerView.adapter =
                EditTaskAdapter(
                    initialData.tasks.map { it.task }.toMutableList(),
                    object : StartDragListener {
                        override fun requestDrag(viewHolder: RecyclerView.ViewHolder?) {
                            if (viewHolder != null) {
                                touchHelper?.startDrag(viewHolder)
                            }
                        }
                    })
        } else {
            taskRecyclerView.adapter =
                EditTaskAdapter(mutableListOf("", "", "", "", ""), object : StartDragListener {
                    override fun requestDrag(viewHolder: RecyclerView.ViewHolder?) {
                        if (viewHolder != null) {
                            touchHelper?.startDrag(viewHolder)
                        }
                    }
                })
        }
        val callback: ItemTouchHelper.Callback =
            ItemMoveCallback(taskRecyclerView.adapter as EditTaskAdapter)
        touchHelper = ItemTouchHelper(callback)
        touchHelper.attachToRecyclerView(taskRecyclerView)

        lifecycleScope.launch {
            users = EprobaApplication.instance.apiHelper.getUsers()
            (_binding?.userSelect?.editText as? MaterialAutoCompleteTextView)?.setSimpleItems(
                users.filter { it.scout.teamId == user.scout.teamId }.map { it.nickname }
                    .toTypedArray()
            )
        }

        (requireActivity() as CreateExamActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.addTaskLayout.setOnClickListener {
            (taskRecyclerView.adapter as EditTaskAdapter).addTask()
        }

        binding.clearButton.setOnClickListener {
            taskRecyclerView.adapter =
                EditTaskAdapter(mutableListOf("", "", "", "", ""), object : StartDragListener {
                    override fun requestDrag(viewHolder: RecyclerView.ViewHolder?) {
                        if (viewHolder != null) {
                            touchHelper.startDrag(viewHolder)
                        }
                    }
                })
            binding.examName.editText?.setText("")
            binding.userSelect.editText?.setText("")
        }

        binding.submitButton.setOnClickListener {
            val tasks: List<String> = (taskRecyclerView.adapter as EditTaskAdapter).getTasks()
            val name = binding.examName.editText?.text.toString()
            val nickname = binding.userSelect.editText?.text.toString()
            if (users.isEmpty()) {
                Toast.makeText(
                    context,
                    getString(com.czaplicki.eproba.R.string.users_list_download_error),
                    Toast.LENGTH_SHORT
                )
                    .show()
                return@setOnClickListener
            }
            val userId: Long? =
                if (user.scout.function >= 2 && binding.userSelect.editText?.text.toString()
                        .isNotBlank()
                ) {
                    users.find { it.nickname == binding.userSelect.editText?.text.toString() }?.id
                        ?: users.find {
                            it.nickname?.lowercase() == binding.userSelect.editText?.text.toString()
                                .lowercase()
                        }?.id
                } else {
                    user.id
                }
            if (name.isBlank() || userId == null || tasks.isEmpty()) {
                if (tasks.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        getString(com.czaplicki.eproba.R.string.at_least_one_task_is_required),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                if (name.isBlank()) {
                    binding.examName.error = getString(com.czaplicki.eproba.R.string.required_field)
                    binding.examName.editText?.addTextChangedListener(examNameFieldTextWatcher)
                }
                if (nickname.isBlank()) {
                    binding.userSelect.error =
                        getString(com.czaplicki.eproba.R.string.required_field)
                    binding.userSelect.editText?.addTextChangedListener(userSelectFieldTextWatcher)
                } else if (userId == null) {
                    binding.userSelect.error =
                        getString(com.czaplicki.eproba.R.string.user_does_not_exist)
                    binding.userSelect.editText?.addTextChangedListener(userSelectFieldTextWatcher)
                }
                Toast.makeText(
                    requireContext(),
                    getString(com.czaplicki.eproba.R.string.fill_in_all_fields),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                lifecycleScope.launch {
                    try {
                        EprobaApplication.instance.database.examDao().insert(
                            EprobaApplication.instance.service.createExam(
                                Exam(
                                    name = name,
                                    userId = userId,
                                    tasks = tasks.map { Task(-1L, it) }.toMutableList()
                                ).toJson().toRequestBody("application/json".toMediaType())
                            )
                        )
                        activity?.runOnUiThread {
                            MaterialAlertDialogBuilder(
                                requireContext(),
                                R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
                            )
                                .setTitle(com.czaplicki.eproba.R.string.exam_created)
                                .setIcon(com.czaplicki.eproba.R.drawable.ic_success)
                                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                                    dialog.dismiss()
                                    activity?.finish()
                                }
                                .show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        if (e is HttpException && e.code() == HTTP_FORBIDDEN) {
                            activity?.runOnUiThread {
                                MaterialAlertDialogBuilder(
                                    requireContext(),
                                    R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
                                )
                                    .setTitle(com.czaplicki.eproba.R.string.error_dialog_title)
                                    .setMessage(com.czaplicki.eproba.R.string.exam_creating_unauthorized_error)
                                    .setIcon(com.czaplicki.eproba.R.drawable.ic_error)
                                    .setPositiveButton(android.R.string.ok, null)
                                    .show()

                            }
                        } else {
                            activity?.runOnUiThread {
                                MaterialAlertDialogBuilder(
                                    requireContext(),
                                    R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
                                )
                                    .setTitle(com.czaplicki.eproba.R.string.error_dialog_title)
                                    .setMessage(
                                        getString(
                                            com.czaplicki.eproba.R.string.exam_creating_error,
                                            e
                                        )
                                    )
                                    .setIcon(com.czaplicki.eproba.R.drawable.ic_error)
                                    .setPositiveButton(android.R.string.ok, null)
                                    .show()
                            }
                        }
                    }
                }

            }
        }

        return root
    }

    override fun onResume() {
        super.onResume()
        (activity as? CreateExamActivity)?.bottomNavigationView?.setOnItemReselectedListener {
            binding.scrollView.fullScroll(View.FOCUS_UP)
            (activity as? CreateExamActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(
                true
            )
        }

        if (EprobaApplication.instance.apiHelper.user!!.scout.function >= 2) {
            binding.userSelect.isEnabled = true
        } else {
            binding.userSelect.isEnabled = false
            binding.userSelect.editText?.setText(user.nickname)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}