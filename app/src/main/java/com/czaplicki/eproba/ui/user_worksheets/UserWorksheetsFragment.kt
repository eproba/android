package com.czaplicki.eproba.ui.user_worksheets

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
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
import com.czaplicki.eproba.databinding.FragmentFirstBinding
import com.czaplicki.eproba.db.Worksheet
import com.czaplicki.eproba.db.WorksheetDao
import com.czaplicki.eproba.db.User
import com.czaplicki.eproba.db.UserDao
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.launch
import net.openid.appauth.AuthorizationService
import java.util.Locale
import java.util.UUID


class UserWorksheetsFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    private lateinit var mAuthStateManager: AuthStateManager
    private lateinit var authService: AuthorizationService
    private var recyclerView: RecyclerView? = null
    private lateinit var mSwipeRefreshLayout: SwipeRefreshLayout
    private var searchView: SearchView? = null
    var originalWorksheetList: MutableList<Worksheet> = mutableListOf()
    var worksheetList: MutableList<Worksheet> = mutableListOf()
    private val binding get() = _binding!!
    private val app = EprobaApplication.instance
    private val service: EprobaService = app.service
    private val userDao: UserDao = app.database.userDao()
    private val worksheetDao: WorksheetDao = app.database.worksheetDao()
    var users: MutableList<User> = mutableListOf()
    private val viewModel: WorksheetsViewModel by viewModels { WorksheetsViewModel.Factory }
    private val sharedPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(
            requireContext()
        )
    }
    var ignoreWorksheetsLastSync = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        mAuthStateManager = AuthStateManager.getInstance(requireContext())
        authService = AuthorizationService(requireContext())
        recyclerView = binding.recyclerView
        recyclerView?.layoutManager = LinearLayoutManager(view?.context)
        recyclerView?.adapter = WorksheetAdapter(worksheetList, users, service)
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


        (activity as MainActivity).user?.let { viewModel.userId = it.id }
        lifecycle.coroutineScope.launch {
            viewModel.worksheets.collect {
                worksheetList.clear()
                worksheetList.addAll(it)
                if (worksheetList.isEmpty()) {
                    worksheetList.add(Worksheet(id = UUID.fromString("00000000-0000-0000-0000-000000000000"), name = "no_worksheets"))
                }
                if (sharedPreferences
                        .getBoolean("ads", true)
                ) worksheetList.add(Worksheet(id = UUID.fromString("00000000-0000-0000-0000-000000000000"), name = "ad"))
                originalWorksheetList = worksheetList.toMutableList()
                if (searchView != null && searchView?.query.toString().isNotEmpty()) {
                    filter(searchView?.query.toString())
                } else {
                    recyclerView?.adapter?.notifyDataSetChanged()
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
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mSwipeRefreshLayout.setOnRefreshListener {
            lifecycleScope.launch {
                mSwipeRefreshLayout.isRefreshing = true
                app.apiHelper.getWorksheets(userOnly = true, ignoreLastSync = ignoreWorksheetsLastSync)
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
            (activity as MainActivity).user?.let { viewModel.userId = it.id }
            getUsers()
        }
        recyclerView!!.setOnScrollChangeListener { _, _, _, _, oldY ->
            if (oldY >= 40 || oldY == 0 || !recyclerView!!.canScrollVertically(-1)) {
                (activity as? MainActivity)?.fab?.extend()
            } else if (oldY < -40) {
                (activity as? MainActivity)?.fab?.shrink()
            }
        }
        sharedPreferences.getLong("lastSync", 0).let {
            if (it == 0L || System.currentTimeMillis() - it > 3600000) {
                if (mAuthStateManager.current.isAuthorized) {
                    getUsers()
                }
                recyclerView?.adapter?.notifyDataSetChanged()
            } else {
                lifecycleScope.launch {
                    users.clear()
                    users.addAll(userDao.getAll())
                    recyclerView?.adapter?.notifyDataSetChanged()
                }
            }
        }
        Log.e("user", "${EprobaApplication.instance.sharedPreferences.getString("user", "null")}")
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
        Log.d("onResume", "onResume")
        Log.d("onResume", (activity as MainActivity).user.toString())
        (activity as MainActivity).user?.let {
            Log.d("onResume", "user not null")
            Log.d("onResume", it.toString())
            if (viewModel.savedStateHandle.get<UUID>("user_id") != it.id) {
                viewModel.userId = it.id
            }
        }
        (activity as MainActivity).user?.let { viewModel.userId = it.id }
        if (mAuthStateManager.current.isAuthorized) {
            lifecycleScope.launch {
                mSwipeRefreshLayout.isRefreshing = true
                app.apiHelper.getWorksheets(userOnly = true)
                mSwipeRefreshLayout.isRefreshing = false
            }
            (activity as MainActivity).user?.let { viewModel.userId = it.id }
        }
        (activity as? MainActivity)?.bottomNavigation?.setOnItemReselectedListener {
            recyclerView?.smoothSnapToPosition(0)
        }
    }


    private fun getUsers() {
        lifecycleScope.launch {
            mSwipeRefreshLayout.isRefreshing = true
            users.clear()
            users.addAll(app.apiHelper.getUsers())
            recyclerView?.adapter?.notifyDataSetChanged()
            mSwipeRefreshLayout.isRefreshing = false
        }
    }


    private fun filter(text: String) {
        val filteredList: ArrayList<Worksheet> = ArrayList<Worksheet>()

        for (item in originalWorksheetList.filter { it.id != UUID.fromString("00000000-0000-0000-0000-000000000000") }) {
            if (item.name!!.lowercase(Locale.ROOT).contains(text.lowercase(Locale.getDefault()))) {
                filteredList.add(item)
            }
        }
        if (filteredList.isEmpty()) {
            filteredList.add(Worksheet(id = UUID.fromString("00000000-0000-0000-0000-000000000000"), name = "no_worksheets"))
        }
        if (sharedPreferences
                .getBoolean("ads", true)
        ) filteredList.add(Worksheet(id = UUID.fromString("00000000-0000-0000-0000-000000000000"), name = "ad"))
        (recyclerView?.adapter as WorksheetAdapter).filterList(filteredList)
    }

}