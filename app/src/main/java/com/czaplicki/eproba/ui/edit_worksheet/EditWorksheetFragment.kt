package com.czaplicki.eproba.ui.edit_worksheet

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.czaplicki.eproba.CreateWorksheetActivity
import com.czaplicki.eproba.EprobaApplication
import com.czaplicki.eproba.MainActivity
import com.czaplicki.eproba.databinding.FragmentEditWorksheetBinding
import com.czaplicki.eproba.db.Worksheet
import com.czaplicki.eproba.db.Task
import com.czaplicki.eproba.ui.compose_worksheet.EditTaskAdapter
import com.czaplicki.eproba.ui.compose_worksheet.ItemMoveCallback
import com.czaplicki.eproba.ui.compose_worksheet.StartDragListener
import com.google.android.material.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.net.HttpURLConnection.HTTP_FORBIDDEN
import java.util.UUID


class EditWorksheetFragment : Fragment() {

    private var _binding: FragmentEditWorksheetBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditWorksheetBinding.inflate(inflater, container, false)

        val worksheetNameFieldTextWatcher: TextWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if (s.toString().isBlank()) {
                    binding.worksheetName.error = getString(com.czaplicki.eproba.R.string.required_field)
                } else {
                    binding.worksheetName.error = null
                }
            }
        }


        val worksheet: Worksheet = Gson().fromJson(
            arguments?.getString("initialData"), Worksheet::class.java
        )

        val root: View = binding.root

        var touchHelper: ItemTouchHelper? = null
        val taskRecyclerView = binding.tasksRecyclerView
        taskRecyclerView.layoutManager = object : LinearLayoutManager(view?.context) {
            override fun canScrollVertically() = false
        }
        binding.worksheetName.editText?.setText(worksheet.name)
        binding.user.editText?.setText(
            arguments?.getString(
                "initialDataNickname"
            ) ?: getString(com.czaplicki.eproba.R.string.someone)
        )

        taskRecyclerView.adapter =
            EditTaskAdapter(worksheet.tasks.map { it.task }.toMutableList(), object : StartDragListener {
                override fun requestDrag(viewHolder: RecyclerView.ViewHolder?) {
                    if (viewHolder != null) {
                        touchHelper?.startDrag(viewHolder)
                    }
                }
            })
        val callback: ItemTouchHelper.Callback =
            ItemMoveCallback(taskRecyclerView.adapter as EditTaskAdapter)
        touchHelper = ItemTouchHelper(callback)
        touchHelper.attachToRecyclerView(taskRecyclerView)


        (requireActivity() as MainActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.addTaskLayout.setOnClickListener {
            (taskRecyclerView.adapter as EditTaskAdapter).addTask()
        }

        binding.cancelButton.setOnClickListener {
            Navigation.findNavController(root).popBackStack()
        }

        binding.submitButton.setOnClickListener {
            val tasks: List<String> = (taskRecyclerView.adapter as EditTaskAdapter).getTasks()
            val name = binding.worksheetName.editText?.text.toString()
            if (name.isBlank() || tasks.isEmpty()) {
                if (tasks.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        getString(com.czaplicki.eproba.R.string.at_least_one_task_is_required),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                if (name.isBlank()) {
                    binding.worksheetName.error = getString(com.czaplicki.eproba.R.string.required_field)
                    binding.worksheetName.editText?.addTextChangedListener(worksheetNameFieldTextWatcher)
                }
                Toast.makeText(
                    requireContext(),
                    getString(com.czaplicki.eproba.R.string.fill_in_all_fields),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                lifecycleScope.launch {
                    try {
                        EprobaApplication.instance.database.worksheetDao().update(
                            EprobaApplication.instance.service.updateWorksheet(
                                worksheet.id, Worksheet(
                                    id = worksheet.id,
                                    name = name,
                                    userId = worksheet.userId,
                                    tasks = tasks.map { Task(UUID.fromString("00000000-0000-0000-0000-000000000000"), it) }.toMutableList()
                                ).toJson().toRequestBody("application/json".toMediaType())
                            )
                        )
                        activity?.runOnUiThread {
                            MaterialAlertDialogBuilder(
                                requireContext(),
                                R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
                            ).setTitle(com.czaplicki.eproba.R.string.worksheet_saved_dialog_title)
                                .setIcon(com.czaplicki.eproba.R.drawable.ic_success)
                                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                                    dialog.dismiss()
                                    Navigation.findNavController(root).popBackStack()
                                }.show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        if (e is HttpException && e.code() == HTTP_FORBIDDEN) {
                            activity?.runOnUiThread {
                                MaterialAlertDialogBuilder(
                                    requireContext(),
                                    R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
                                ).setTitle(com.czaplicki.eproba.R.string.error_dialog_title)
                                    .setMessage(com.czaplicki.eproba.R.string.worksheet_editing_unauthorized_error)
                                    .setIcon(com.czaplicki.eproba.R.drawable.ic_error)
                                    .setPositiveButton(android.R.string.ok, null).show()

                            }
                        } else {
                            activity?.runOnUiThread {
                                MaterialAlertDialogBuilder(
                                    requireContext(),
                                    R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
                                ).setTitle(com.czaplicki.eproba.R.string.error_dialog_title)
                                    .setMessage(
                                        getString(
                                            com.czaplicki.eproba.R.string.worksheet_editing_error, e
                                        )
                                    ).setIcon(com.czaplicki.eproba.R.drawable.ic_error)
                                    .setPositiveButton(android.R.string.ok, null).show()
                            }
                        }
                    }
                }

            }
        }

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menu.findItem(com.czaplicki.eproba.R.id.app_bar_search).isVisible = false
                menu.findItem(com.czaplicki.eproba.R.id.settings).isVisible = false
                menu.findItem(com.czaplicki.eproba.R.id.account).isVisible = false
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
        return root
    }

    override fun onResume() {
        super.onResume()
        (activity as? CreateWorksheetActivity)?.bottomNavigationView?.setOnItemReselectedListener {
            binding.scrollView.fullScroll(View.FOCUS_UP)
            (activity as? CreateWorksheetActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(
                true
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}