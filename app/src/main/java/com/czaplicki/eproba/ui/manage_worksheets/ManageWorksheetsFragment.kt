package com.czaplicki.eproba.ui.manage_worksheets

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
import com.czaplicki.eproba.EOLScreen
import com.czaplicki.eproba.EprobaApplication
import com.czaplicki.eproba.MainActivity
import com.czaplicki.eproba.MaintenanceScreen
import com.czaplicki.eproba.R
import com.czaplicki.eproba.api.APIState
import com.czaplicki.eproba.api.EprobaService
import com.czaplicki.eproba.databinding.FragmentManageWorksheetsBinding
import com.czaplicki.eproba.db.Worksheet
import com.czaplicki.eproba.db.WorksheetDao
import com.czaplicki.eproba.db.Patrol
import com.czaplicki.eproba.db.Team
import com.czaplicki.eproba.db.User
import com.czaplicki.eproba.db.UserDao
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.launch
import net.openid.appauth.AuthorizationService
import java.util.Locale
import java.util.UUID


class ManageWorksheetsFragment : Fragment() {

    private var _binding: FragmentManageWorksheetsBinding? = null

    private lateinit var mAuthStateManager: AuthStateManager
    private lateinit var authService: AuthorizationService
    private var recyclerView: RecyclerView? = null
    private lateinit var mSwipeRefreshLayout: SwipeRefreshLayout
    private var searchView: SearchView? = null
    private var originalWorksheetList: MutableList<Worksheet> = mutableListOf()
    var worksheetList: MutableList<Worksheet> = mutableListOf()
    private val binding get() = _binding!!
    private lateinit var userDao: UserDao
    private lateinit var worksheetDao: WorksheetDao
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
    var ignoreWorksheetsLastSync = false

    class TeamFilterBottomSheet(val parent: ManageWorksheetsFragment) : BottomSheetDialogFragment() {

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


    class PatrolFilterBottomSheet(val parent: ManageWorksheetsFragment) : BottomSheetDialogFragment() {

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
        _binding = FragmentManageWorksheetsBinding.inflate(inflater, container, false)
        mAuthStateManager = AuthStateManager.getInstance(requireContext())
        authService = AuthorizationService(requireContext())
        userDao = (activity?.application as EprobaApplication).database.userDao()
        worksheetDao = (activity?.application as EprobaApplication).database.worksheetDao()
        recyclerView = binding.recyclerView
        recyclerView?.layoutManager = LinearLayoutManager(view?.context)
        recyclerView?.adapter = ManagedWorksheetAdapter(worksheetList, users, service, worksheetDao)
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
                users.addAll(userDao.getAll())
                recyclerView?.adapter?.notifyDataSetChanged()
            }
            if (it == 0L || System.currentTimeMillis() - it > 3600000) {
                getUsers()
                recyclerView?.adapter?.notifyDataSetChanged()
            }
        }

        val viewModel: ManagedWorksheetsViewModel by viewModels { ManagedWorksheetsViewModel.Factory }

        lifecycle.coroutineScope.launch {
            viewModel.worksheets.collect { worksheet ->
                worksheetList.clear()
                worksheetList.addAll(worksheet)
                if (worksheetList.isEmpty()) {
                    worksheetList.add(Worksheet(id = UUID.fromString("00000000-0000-0000-0000-000000000000"), name = "no_worksheets"))
                }
                if (sharedPreferences
                        .getBoolean("ads", true)
                ) {
                    for (i in 5..worksheetList.size step 5) {
                        worksheetList.add(i, Worksheet(id = UUID.fromString("00000000-0000-0000-0000-000000000000"), name = "ad"))
                    }
                    if (!(worksheetList.last().name == "ad" && worksheetList.last().id == UUID.fromString("00000000-0000-0000-0000-000000000000"))) {
                        worksheetList.add(Worksheet(id = UUID.fromString("00000000-0000-0000-0000-000000000000"), name = "ad"))
                    }
                }
                originalWorksheetList = worksheetList.toMutableList()
                if (searchView != null && searchView?.query.toString().isNotEmpty()) {
                    filter(searchView?.query.toString())
                } else {
                    recyclerView?.adapter?.notifyDataSetChanged()
                }

                for (_worksheet in worksheetList) {
                    if (users.find { it.id == _worksheet.userId }?.teamId !in teams.map { it.id } && users.find { it.id == _worksheet.userId }?.teamId != null && users.find { it.id == _worksheet.userId }?.teamName != null) {
                        teams.add(
                            Team(
                                id = users.find { it.id == _worksheet.userId }?.teamId!!,
                                name = users.find { it.id == _worksheet.userId }?.teamName!!
                            )
                        )
                    }
                    if (users.find { it.id == _worksheet.userId }?.patrolId !in patrols.map { it.id } && users.find { it.id == _worksheet.userId }?.patrolId != null && users.find { it.id == _worksheet.userId }?.patrolName != null && users.find { it.id == _worksheet.userId }?.teamId != null) {
                        patrols.add(
                            Patrol(
                                id = users.find { it.id == _worksheet.userId }?.patrolId!!,
                                name = users.find { it.id == _worksheet.userId }?.patrolName!!,
                                teamId = users.find { it.id == _worksheet.userId }?.teamId!!
                            )
                        )
                    }
                }
                if (_binding != null) {
                    filterArchived(binding.chipArchive.isChecked)
                }
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
                        val worksheetsInArchive = service.getArchivedWorksheets().toTypedArray()
                        worksheetDao.insert(*worksheetsInArchive)
                        if (worksheetsInArchive.isEmpty()) {
                            filterArchived(true)
                        }
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
                EprobaApplication.instance.apiHelper.getWorksheets(ignoreLastSync = ignoreWorksheetsLastSync)
                mSwipeRefreshLayout.isRefreshing = false
                ignoreWorksheetsLastSync = !ignoreWorksheetsLastSync
                when (EprobaApplication.instance.apiHelper.getAndProcessAppConfig()) {

                    APIState.EOL -> {
                        val eolScreen = EOLScreen(service.getAppConfig())
                        eolScreen.show(parentFragmentManager, "eol")
                    }

                    APIState.MAINTENANCE -> {
                        val maintenanceScreen = MaintenanceScreen()
                        maintenanceScreen.show(parentFragmentManager, "maintenance")
                    }

                    else -> {
                        // Do nothing
                    }
                }
            }
            getUsers()
        }
        lifecycleScope.launch {
            mSwipeRefreshLayout.isRefreshing = true
            EprobaApplication.instance.apiHelper.getWorksheets()
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
        val filteredList: ArrayList<Worksheet> = ArrayList<Worksheet>()

        for (item in originalWorksheetList.filter { it.id != UUID.fromString("00000000-0000-0000-0000-000000000000") }) {
            if ((item.name + " - " + users.find { it.id == item.userId }?.nicknameWithRank).lowercase(
                    Locale.ROOT
                ).contains(text.lowercase(Locale.getDefault()))
            ) {
                filteredList.add(item)
            }
        }
        if (filteredList.isEmpty()) {
            filteredList.add(Worksheet(id = UUID.fromString("00000000-0000-0000-0000-000000000000"), name = "no_worksheets"))
        }
        if (sharedPreferences
                .getBoolean("ads", true)
        ) filteredList.add(Worksheet(id = UUID.fromString("00000000-0000-0000-0000-000000000000"), name = "ad"))
        (recyclerView?.adapter as ManagedWorksheetAdapter).filterList(filteredList)
    }

    private fun filterArchived(isArchived: Boolean = false) {
        val filteredList: ArrayList<Worksheet> = ArrayList<Worksheet>()

        for (item in originalWorksheetList.filter { it.id != UUID.fromString("00000000-0000-0000-0000-000000000000") }) {
            if (item.isArchived == isArchived) {
                filteredList.add(item)
            }
        }
        if (filteredList.isEmpty()) {
            filteredList.add(Worksheet(id = UUID.fromString("00000000-0000-0000-0000-000000000000"), name = "no_worksheets"))
        }
        if (sharedPreferences.getBoolean("ads", true)) {
            for (i in 5..filteredList.size step 5) {
                filteredList.add(i, Worksheet(id = UUID.fromString("00000000-0000-0000-0000-000000000000"), name = "ad"))
            }
            if (!(filteredList.last().name == "ad" && filteredList.last().id == UUID.fromString("00000000-0000-0000-0000-000000000000"))) {
                filteredList.add(Worksheet(id = UUID.fromString("00000000-0000-0000-0000-000000000000"), name = "ad"))
            }
        }
        (recyclerView?.adapter as ManagedWorksheetAdapter).filterList(filteredList)
    }

    private fun filterByTeams(teams: MutableList<Team>) {
        val filteredList: ArrayList<Worksheet> = ArrayList<Worksheet>()

        updateChips(team = true, patrol = false)

        if (teams.isEmpty()) {
            filteredList.addAll(originalWorksheetList.filter { it.id != UUID.fromString("00000000-0000-0000-0000-000000000000") })
        } else {
            for (item in originalWorksheetList.filter { it.id != UUID.fromString("00000000-0000-0000-0000-000000000000") }) {
                if (users.find { it.id == item.userId }?.teamId in teams.map { it.id }) {
                    filteredList.add(item)
                }
            }
            if (selectedPatrols.isNotEmpty()) {
                for (item in filteredList.filter { it.id != UUID.fromString("00000000-0000-0000-0000-000000000000") }) {
                    if (users.find { it.id == item.userId }?.patrolId !in selectedPatrols.map { it.id }) {
                        filteredList.remove(item)
                    }
                }
            }
        }
        if (filteredList.isEmpty()) {
            filteredList.add(Worksheet(id = UUID.fromString("00000000-0000-0000-0000-000000000000"), name = "no_worksheets"))
        }
        if (sharedPreferences.getBoolean("ads", true)) {
            for (i in 5..filteredList.size step 5) {
                filteredList.add(i, Worksheet(id = UUID.fromString("00000000-0000-0000-0000-000000000000"), name = "ad"))
            }
            if (!(filteredList.last().name == "ad" && filteredList.last().id == UUID.fromString("00000000-0000-0000-0000-000000000000"))) {
                filteredList.add(Worksheet(id = UUID.fromString("00000000-0000-0000-0000-000000000000"), name = "ad"))
            }
        }
        (recyclerView?.adapter as ManagedWorksheetAdapter).filterList(filteredList)
    }


    private fun filterByPatrols(patrols: MutableList<Patrol>) {
        val filteredList: ArrayList<Worksheet> = ArrayList<Worksheet>()

        updateChips(team = false, patrol = true)

        if (patrols.isEmpty()) {
            filteredList.addAll(originalWorksheetList.filter { it.id != UUID.fromString("00000000-0000-0000-0000-000000000000") })
        } else if (selectedTeams.isNotEmpty()) {
            for (item in originalWorksheetList.filter { worksheet -> worksheet.id != UUID.fromString("00000000-0000-0000-0000-000000000000") && users.find { it.id == worksheet.userId }?.teamId in selectedTeams.map { it.id } }) {
                if (users.find { it.id == item.userId }?.patrolId in patrols.map { it.id }) {
                    filteredList.add(item)
                }
            }
        } else {
            for (item in originalWorksheetList.filter { worksheet -> worksheet.id != UUID.fromString("00000000-0000-0000-0000-000000000000") }) {
                if (users.find { it.id == item.userId }?.patrolId in patrols.map { it.id }) {
                    filteredList.add(item)
                }
            }
        }

        if (filteredList.isEmpty()) {
            filteredList.add(Worksheet(id = UUID.fromString("00000000-0000-0000-0000-000000000000"), name = "no_worksheets"))
        }
        if (sharedPreferences.getBoolean("ads", true)) {

            for (i in 5..filteredList.size step 5) {
                filteredList.add(i, Worksheet(id = UUID.fromString("00000000-0000-0000-0000-000000000000"), name = "ad"))
            }
            if (!(filteredList.last().name == "ad" && filteredList.last().id == UUID.fromString("00000000-0000-0000-0000-000000000000"))) {
                filteredList.add(Worksheet(id = UUID.fromString("00000000-0000-0000-0000-000000000000"), name = "ad"))
            }
        }
        (recyclerView?.adapter as ManagedWorksheetAdapter).filterList(filteredList)
    }

}