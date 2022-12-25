package com.czaplicki.eproba.ui.manage_exams

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.czaplicki.eproba.AuthStateManager
import com.czaplicki.eproba.EprobaApplication
import com.czaplicki.eproba.MainActivity
import com.czaplicki.eproba.R
import com.czaplicki.eproba.api.EprobaService
import com.czaplicki.eproba.databinding.FragmentManageExamsBinding
import com.czaplicki.eproba.db.Exam
import com.czaplicki.eproba.db.ExamDao
import com.czaplicki.eproba.db.Patrol
import com.czaplicki.eproba.db.Team
import com.czaplicki.eproba.db.User
import com.czaplicki.eproba.db.UserDao
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.launch
import net.openid.appauth.AuthorizationService
import java.util.Locale


class ManageExamsFragment : Fragment() {

    private var _binding: FragmentManageExamsBinding? = null

    private lateinit var mAuthStateManager: AuthStateManager
    private lateinit var authService: AuthorizationService
    private var recyclerView: RecyclerView? = null
    private lateinit var mSwipeRefreshLayout: SwipeRefreshLayout
    private var searchView: SearchView? = null
    private var originalExamList: MutableList<Exam> = mutableListOf()
    var examList: MutableList<Exam> = mutableListOf()
    private val binding get() = _binding!!
    private lateinit var userDao: UserDao
    private lateinit var examDao: ExamDao
    private val service: EprobaService = EprobaApplication.instance.service
    var users: MutableList<User> = mutableListOf()
    var teams: MutableList<Team> = mutableListOf()
    var selectedTeams: MutableList<Team> = mutableListOf()
    var patrols: MutableList<Patrol> = mutableListOf()
    var selectedPatrols: MutableList<Patrol> = mutableListOf()
    private val sharedPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(
            requireContext()
        )
    }
    var ignoreExamsLastSync = false

    class TeamFilterBottomSheet(val parent: ManageExamsFragment) : BottomSheetDialogFragment() {

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? = inflater.inflate(R.layout.modal_bottom_sheet_filter, container, false)

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            val teamCheckboxContainer =
                view.findViewById<LinearLayout>(R.id.team_checkbox_container)

            for (team in parent.teams.sortedBy { it.name }) {
                val checkBox = CheckBox(requireContext())
                checkBox.text = team.name
                checkBox.id = team.id.toInt()
                checkBox.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        if (!parent.selectedTeams.contains(team)) {
                            parent.selectedTeams.add(team)
                        }
                    } else {
                        parent.selectedTeams.remove(team)
                    }
                    parent.filterByTeams(parent.selectedTeams)
                }
                if (parent.selectedTeams.contains(team)) {
                    checkBox.isChecked = true
                }
                teamCheckboxContainer.addView(checkBox)
            }

        }

        companion object {
            const val TAG = "TeamFilterBottomSheet"
        }
    }


    class PatrolFilterBottomSheet(val parent: ManageExamsFragment) : BottomSheetDialogFragment() {

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            val view = inflater.inflate(R.layout.modal_bottom_sheet_filter, container, false)
            view.findViewById<TextView>(R.id.filter_title).text = getString(R.string.select_patrols)
            return view
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            val teamCheckboxContainer =
                view.findViewById<LinearLayout>(R.id.team_checkbox_container)

            for (patrol in parent.patrols.sortedBy { it.name }) {
                val checkBox = CheckBox(requireContext())
                checkBox.text = patrol.name
                checkBox.id = patrol.id.toInt()
                checkBox.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        if (!parent.selectedPatrols.contains(patrol)) {
                            parent.selectedPatrols.add(patrol)
                        }
                    } else {
                        parent.selectedPatrols.remove(patrol)
                    }
                    parent.filterByPatrols(parent.selectedPatrols)
                }
                if (parent.selectedPatrols.contains(patrol)) {
                    checkBox.isChecked = true
                }
                teamCheckboxContainer.addView(checkBox)
            }

        }

        companion object {
            const val TAG = "PatrolFilterBottomSheet"
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageExamsBinding.inflate(inflater, container, false)
        mAuthStateManager = AuthStateManager.getInstance(requireContext())
        authService = AuthorizationService(requireContext())
        userDao = (activity?.application as EprobaApplication).database.userDao()
        examDao = (activity?.application as EprobaApplication).database.examDao()
        recyclerView = binding.recyclerView
        recyclerView?.layoutManager = LinearLayoutManager(view?.context)
        recyclerView?.adapter = ManagedExamAdapter(examList, users, service, examDao)
        mSwipeRefreshLayout = binding.swipeRefreshLayout
        mSwipeRefreshLayout.setColorSchemeColors(
            MaterialColors.getColor(
                binding.root,
                com.google.android.material.R.attr.colorPrimary
            )
        )
        mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(
            MaterialColors.getColor(
                binding.root,
                com.google.android.material.R.attr.colorSurfaceVariant
            )
        )
        recyclerView!!.setOnScrollChangeListener { _, _, _, _, oldY ->
            if (oldY >= 40 || oldY == 0 || !recyclerView!!.canScrollVertically(-1)) {
                (activity as? MainActivity)?.fab?.extend()
            } else if (oldY < -40) {
                (activity as? MainActivity)?.fab?.shrink()
            }
        }
        sharedPreferences.getLong("lastSync", 0).let {
            lifecycleScope.launch {
                users.clear()
                users.addAll(userDao.getAllNow())
                recyclerView?.adapter?.notifyDataSetChanged()
            }
            if (it == 0L || System.currentTimeMillis() - it > 3600000) {
                getUsers()
                recyclerView?.adapter?.notifyDataSetChanged()
            }
        }

        val viewModel: ManagedExamsViewModel by viewModels { ManagedExamsViewModel.Factory }

        lifecycle.coroutineScope.launch {
            viewModel.exams.collect { exam ->
                examList.clear()
                examList.addAll(exam)
                if (examList.isEmpty()) {
                    examList.add(Exam(id = -1, name = "no_exams"))
                }
                if (sharedPreferences
                        .getBoolean("ads", true)
                ) {
                    for (i in 5..examList.size step 5) {
                        examList.add(i, Exam(id = -1, name = "ad"))
                    }
                    if (!(examList.last().name == "ad" && examList.last().id == -1L)) {
                        examList.add(Exam(id = -1, name = "ad"))
                    }
                }
                originalExamList = examList.toMutableList()
                if (searchView != null && searchView?.query.toString().isNotEmpty()) {
                    filter(searchView?.query.toString())
                } else {
                    recyclerView?.adapter?.notifyDataSetChanged()
                }

                for (_exam in examList) {
                    if (users.find { it.id == _exam.userId }?.scout?.teamId !in teams.map { it.id } && users.find { it.id == _exam.userId }?.scout?.teamId != null && users.find { it.id == _exam.userId }?.scout?.teamName != null) {
                        teams.add(
                            Team(
                                id = users.find { it.id == _exam.userId }?.scout?.teamId!!,
                                name = users.find { it.id == _exam.userId }?.scout?.teamName!!
                            )
                        )
                    }
                    if (users.find { it.id == _exam.userId }?.scout?.patrolId !in patrols.map { it.id } && users.find { it.id == _exam.userId }?.scout?.patrolId != null && users.find { it.id == _exam.userId }?.scout?.patrolName != null && users.find { it.id == _exam.userId }?.scout?.teamId != null) {
                        patrols.add(
                            Patrol(
                                id = users.find { it.id == _exam.userId }?.scout?.patrolId!!,
                                name = users.find { it.id == _exam.userId }?.scout?.patrolName!!,
                                teamId = users.find { it.id == _exam.userId }?.scout?.teamId!!
                            )
                        )
                    }
                }
                filterArchived(binding.chipArchive.isChecked)
            }
        }

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                searchView = menu.findItem(R.id.app_bar_search).actionView as SearchView
                searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        return false
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        Log.d("search", newText.toString())
                        filter(newText.toString())
                        return false
                    }
                })
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                // Handle the menu selection
                return false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)


        binding.chipTeam.setOnClickListener {
            val modalBottomSheet = TeamFilterBottomSheet(this)
            modalBottomSheet.show(parentFragmentManager, TeamFilterBottomSheet.TAG)

        }

        binding.chipPatrol.setOnClickListener {
            val modalBottomSheet = PatrolFilterBottomSheet(this)
            modalBottomSheet.show(parentFragmentManager, PatrolFilterBottomSheet.TAG)

        }

        binding.chipArchive.setOnCheckedChangeListener { _, isChecked ->
            selectedTeams.clear()
            selectedPatrols.clear()
            searchView?.setQuery("", false)
            searchView?.clearFocus()
            updateChips()
            if (isChecked) {
                lifecycleScope.launch {
                    try {
                        examDao.insert(*service.getArchivedExams().toTypedArray())
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                filterArchived(false)
            }
        }

        return binding.root
    }

    private fun RecyclerView.smoothSnapToPosition(
        position: Int,
        snapMode: Int = LinearSmoothScroller.SNAP_TO_START
    ) {
        val smoothScroller = object : LinearSmoothScroller(this.context) {
            override fun getVerticalSnapPreference(): Int = snapMode
            override fun getHorizontalSnapPreference(): Int = snapMode
        }
        smoothScroller.targetPosition = position
        layoutManager?.startSmoothScroll(smoothScroller)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        mSwipeRefreshLayout.setOnRefreshListener {
            lifecycleScope.launch {
                mSwipeRefreshLayout.isRefreshing = true
                EprobaApplication.instance.apiHelper.getExams(ignoreLastSync = ignoreExamsLastSync)
                mSwipeRefreshLayout.isRefreshing = false
                ignoreExamsLastSync = !ignoreExamsLastSync
            }
            getUsers()
        }
        lifecycleScope.launch {
            mSwipeRefreshLayout.isRefreshing = true
            EprobaApplication.instance.apiHelper.getExams()
            mSwipeRefreshLayout.isRefreshing = false
        }
        (activity as? MainActivity)?.bottomNavigation?.setOnItemReselectedListener {
            recyclerView?.smoothSnapToPosition(0)
        }
    }

    private fun updateChips(team: Boolean = true, patrol: Boolean = true) {
        if (team) {
            if (selectedTeams.isEmpty()) {
                binding.chipTeam.text = this.getString(R.string.team)
                binding.chipTeam.isSelected = false
            } else {
                binding.chipTeam.isSelected = true
                if (selectedTeams.size == 1) {
                    binding.chipTeam.text =
                        selectedTeams[0].shortName ?: if (selectedTeams[0].name.length > 10) {
                            selectedTeams[0].name.substring(0, 7) + "..."
                        } else {
                            selectedTeams[0].name
                        }
                } else {
                    binding.chipTeam.text =
                        selectedTeams[0].shortName ?: ((if (selectedTeams[0].name.length > 10) {
                            selectedTeams[0].name.substring(0, 7) + "..."
                        } else {
                            selectedTeams[0].name
                        }) + " +" + (selectedTeams.size - 1))
                }
            }
        }
        if (patrol) {
            if (selectedPatrols.isEmpty()) {
                binding.chipPatrol.text = this.getString(R.string.patrol)
                binding.chipPatrol.isSelected = false
            } else {
                binding.chipPatrol.isSelected = true
                if (selectedPatrols.size == 1) {
                    binding.chipPatrol.text = if (selectedPatrols[0].name.length > 17) {
                        selectedPatrols[0].name.substring(0, 10) + "..."
                    } else {
                        selectedPatrols[0].name
                    }
                } else {
                    binding.chipPatrol.text = if (selectedPatrols[0].name.length > 17) {
                        selectedPatrols[0].name.substring(0, 10) + "..."
                    } else {
                        selectedPatrols[0].name
                    } + " +" + (selectedPatrols.size - 1)
                }
            }
        }
    }


    private fun getUsers() {
        lifecycleScope.launch {
            mSwipeRefreshLayout.isRefreshing = true
            users.clear()
            users.addAll(EprobaApplication.instance.apiHelper.getUsers())
            recyclerView?.adapter?.notifyDataSetChanged()
            mSwipeRefreshLayout.isRefreshing = false
        }
    }


    private fun filter(text: String) {
        val filteredList: ArrayList<Exam> = ArrayList<Exam>()

        for (item in originalExamList.filter { it.id != -1L }) {
            if ((item.name + " - " + users.find { it.id == item.userId }?.nicknameWithRank).lowercase(
                    Locale.ROOT
                ).contains(text.lowercase(Locale.getDefault()))
            ) {
                filteredList.add(item)
            }
        }
        if (filteredList.isEmpty()) {
            filteredList.add(Exam(id = -1, name = "no_exams"))
        }
        if (sharedPreferences
                .getBoolean("ads", true)
        ) filteredList.add(Exam(id = -1, name = "ad"))
        (recyclerView?.adapter as ManagedExamAdapter).filterList(filteredList)
    }

    private fun filterArchived(isArchived: Boolean = false) {
        val filteredList: ArrayList<Exam> = ArrayList<Exam>()

        for (item in originalExamList.filter { it.id != -1L }) {
            if (item.isArchived == isArchived) {
                filteredList.add(item)
            }
        }
        if (filteredList.isEmpty()) {
            filteredList.add(Exam(id = -1, name = "no_exams"))
        }
        if (sharedPreferences.getBoolean("ads", true)) {
            for (i in 5..filteredList.size step 5) {
                filteredList.add(i, Exam(id = -1, name = "ad"))
            }
            if (!(filteredList.last().name == "ad" && filteredList.last().id == -1L)) {
                filteredList.add(Exam(id = -1, name = "ad"))
            }
        }
        (recyclerView?.adapter as ManagedExamAdapter).filterList(filteredList)
    }

    private fun filterByTeams(teams: MutableList<Team>) {
        val filteredList: ArrayList<Exam> = ArrayList<Exam>()

        updateChips(team = true, patrol = false)

        if (teams.isEmpty()) {
            filteredList.addAll(originalExamList.filter { it.id != -1L })
        } else {
            for (item in originalExamList.filter { it.id != -1L }) {
                if (users.find { it.id == item.userId }?.scout?.teamId in teams.map { it.id }) {
                    filteredList.add(item)
                }
            }
            if (selectedPatrols.isNotEmpty()) {
                for (item in filteredList.filter { it.id != -1L }) {
                    if (users.find { it.id == item.userId }?.scout?.patrolId !in selectedPatrols.map { it.id }) {
                        filteredList.remove(item)
                    }
                }
            }
        }
        if (filteredList.isEmpty()) {
            filteredList.add(Exam(id = -1, name = "no_exams"))
        }
        if (sharedPreferences.getBoolean("ads", true)) {
            for (i in 5..filteredList.size step 5) {
                filteredList.add(i, Exam(id = -1, name = "ad"))
            }
            if (!(filteredList.last().name == "ad" && filteredList.last().id == -1L)) {
                filteredList.add(Exam(id = -1, name = "ad"))
            }
        }
        (recyclerView?.adapter as ManagedExamAdapter).filterList(filteredList)
    }


    private fun filterByPatrols(patrols: MutableList<Patrol>) {
        val filteredList: ArrayList<Exam> = ArrayList<Exam>()

        updateChips(team = false, patrol = true)

        if (patrols.isEmpty()) {
            filteredList.addAll(originalExamList.filter { it.id != -1L })
        } else if (selectedTeams.isNotEmpty()) {
            for (item in originalExamList.filter { exam -> exam.id != -1L && users.find { it.id == exam.userId }?.scout?.teamId in selectedTeams.map { it.id } }) {
                if (users.find { it.id == item.userId }?.scout?.patrolId in patrols.map { it.id }) {
                    filteredList.add(item)
                }
            }
        } else {
            for (item in originalExamList.filter { exam -> exam.id != -1L }) {
                if (users.find { it.id == item.userId }?.scout?.patrolId in patrols.map { it.id }) {
                    filteredList.add(item)
                }
            }
        }

        if (filteredList.isEmpty()) {
            filteredList.add(Exam(id = -1, name = "no_exams"))
        }
        if (sharedPreferences.getBoolean("ads", true)) {

            for (i in 5..filteredList.size step 5) {
                filteredList.add(i, Exam(id = -1, name = "ad"))
            }
            if (!(filteredList.last().name == "ad" && filteredList.last().id == -1L)) {
                filteredList.add(Exam(id = -1, name = "ad"))
            }
        }
        (recyclerView?.adapter as ManagedExamAdapter).filterList(filteredList)
    }

}