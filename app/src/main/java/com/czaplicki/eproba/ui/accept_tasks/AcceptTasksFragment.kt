package com.czaplicki.eproba.ui.accept_tasks

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.czaplicki.eproba.AuthStateManager
import com.czaplicki.eproba.EprobaApplication
import com.czaplicki.eproba.MainActivity
import com.czaplicki.eproba.api.EprobaService
import com.czaplicki.eproba.databinding.FragmentManageExamsBinding
import com.czaplicki.eproba.db.Exam
import com.czaplicki.eproba.db.Task
import com.czaplicki.eproba.db.User
import com.czaplicki.eproba.db.UserDao
import com.google.android.material.R
import com.google.android.material.color.MaterialColors
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import kotlinx.coroutines.launch
import net.openid.appauth.AuthorizationService

class AcceptTasksFragment : Fragment() {

    private var _binding: FragmentManageExamsBinding? = null

    private lateinit var mAuthStateManager: AuthStateManager
    private lateinit var authService: AuthorizationService
    private var recyclerView: RecyclerView? = null
    private lateinit var mSwipeRefreshLayout: SwipeRefreshLayout
    var examList: MutableList<Exam> = mutableListOf()
    private val binding get() = _binding!!
    private val userDao: UserDao by lazy { (activity?.application as EprobaApplication).database.userDao() }
    private val service: EprobaService = EprobaApplication.instance.service
    var users: MutableList<User> = mutableListOf()
    val sharedPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(
            requireContext()
        )
    }
    val user: User by lazy {
        Gson().fromJson(sharedPreferences.getString("user", ""), User::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageExamsBinding.inflate(inflater, container, false)
        mAuthStateManager = AuthStateManager.getInstance(requireContext())
        authService = AuthorizationService(requireContext())
        recyclerView = binding.recyclerView
        recyclerView?.layoutManager = LinearLayoutManager(view?.context)
        recyclerView?.adapter = AcceptTasksAdapter(examList, users, service)
        mSwipeRefreshLayout = binding.swipeRefreshLayout
        mSwipeRefreshLayout.setColorSchemeColors(
            MaterialColors.getColor(
                binding.root,
                R.attr.colorPrimary
            )
        )
        mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(
            MaterialColors.getColor(
                binding.root,
                R.attr.colorSurfaceVariant
            )
        )
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menu.findItem(com.czaplicki.eproba.R.id.app_bar_search).isVisible = false

            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                // Handle the menu selection
                return false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
        mSwipeRefreshLayout.setOnRefreshListener {
            updateExams()
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
                getUsers()
                recyclerView?.adapter?.notifyDataSetChanged()
            } else {
                lifecycleScope.launch {
                    users.clear()
                    users.addAll(userDao.getAll())
                    recyclerView?.adapter?.notifyDataSetChanged()
                }
            }
        }

        binding.chipGroup.visibility = View.GONE

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
        mSwipeRefreshLayout = binding.swipeRefreshLayout
        mSwipeRefreshLayout.setOnRefreshListener {
            updateExams()
            getUsers()
        }
        updateExams()
        (activity as? MainActivity)?.bottomNavigation?.setOnItemReselectedListener {
            recyclerView?.smoothSnapToPosition(0)
        }
    }

    private fun updateExams() {
        mSwipeRefreshLayout.isRefreshing = true
        service.getTasksTBC()
            .enqueue(object : retrofit2.Callback<List<Exam>> {
                override fun onFailure(call: retrofit2.Call<List<Exam>>, t: Throwable) {
                    view?.let {
                        Snackbar.make(
                            it,
                            "Błąd połączenia z serwerem",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                    t.message?.let { Log.e("FirstFragment", it) }
                    mSwipeRefreshLayout.isRefreshing = false
                }

                override fun onResponse(
                    call: retrofit2.Call<List<Exam>>,
                    response: retrofit2.Response<List<Exam>>
                ) {
                    if (response.body() != null) {
                        examList.clear()
                        examList.addAll(response.body()!!)
                        for (exam in examList) {
                            exam.tasks.removeIf { it.status != Task.Status.AWAITING_APPROVAL || it.approver != user.id }
                        }
                        if (examList.isEmpty()) {
                            examList.add(Exam(id = -1, name = "no_exams"))
                        }
                        if (sharedPreferences
                                .getBoolean("ads", true)
                        ) examList.add(Exam(id = -1, name = "ad"))
                    } else {
                        view?.let {
                            Snackbar.make(
                                it,
                                "Błąd połączenia z serwerem",
                                Snackbar.LENGTH_SHORT
                            ).show()
                        }
                    }
                    recyclerView?.adapter?.notifyDataSetChanged()
                    mSwipeRefreshLayout.isRefreshing = false
                    val userIds: MutableSet<Long> = mutableSetOf()
                    response.body()?.forEach {
                        if (it.userId != null) userIds.add(it.userId!!)
                        if (it.supervisor != null) userIds.add(it.supervisor!!)
                        if (it.tasks.isNotEmpty()) it.tasks.forEach { task ->
                            if (task.approver != null) userIds.add(task.approver!!)
                        }
                    }
                    userIds.filter { id -> users.find { it.id == id } == null }.forEach { id ->
                        service.getUserInfo(id)
                            .enqueue(object : retrofit2.Callback<User> {
                                override fun onFailure(
                                    call: retrofit2.Call<User>,
                                    t: Throwable
                                ) {
                                    view?.let {
                                        Snackbar.make(
                                            it,
                                            "Błąd połączenia z serwerem",
                                            Snackbar.LENGTH_SHORT
                                        ).show()
                                    }
                                }

                                override fun onResponse(
                                    call: retrofit2.Call<User>,
                                    response: retrofit2.Response<User>
                                ) {
                                    if (response.body() != null) {
                                        users.add(response.body()!!)
                                        lifecycleScope.launch {
                                            userDao.insertUsers(response.body()!!)
                                        }
                                        sharedPreferences.edit().putLong(
                                            "lastSync",
                                            System.currentTimeMillis()
                                        ).apply()
                                        recyclerView?.adapter?.notifyDataSetChanged()
                                    }
                                }
                            })

                    }
                }
            })

    }

    private fun getUsers() {
        service.getUsersPublicInfo()
            .enqueue(object : retrofit2.Callback<List<User>> {
                override fun onFailure(call: retrofit2.Call<List<User>>, t: Throwable) {
                    view?.let {
                        Snackbar.make(
                            it,
                            "Błąd połączenia z serwerem",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                    lifecycleScope.launch {
                        users.clear()
                        users.addAll(userDao.getAll())
                        recyclerView?.adapter?.notifyDataSetChanged()
                    }
                    t.message?.let { Log.e("FirstFragment", it) }
                }

                override fun onResponse(
                    call: retrofit2.Call<List<User>>,
                    response: retrofit2.Response<List<User>>
                ) {
                    if (response.body() != null) {
                        users.clear()
                        users.addAll(response.body()!!)
                        lifecycleScope.launch {
                            userDao.insertUsers(*users.toTypedArray())
                        }
                        sharedPreferences.edit()
                            .putLong("lastSync", System.currentTimeMillis()).apply()
                        recyclerView?.adapter?.notifyDataSetChanged()
                    } else {
                        view?.let {
                            Snackbar.make(
                                it,
                                "Błąd połączenia z serwerem",
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                        lifecycleScope.launch {
                            users.clear()
                            users.addAll(userDao.getAll())
                            recyclerView?.adapter?.notifyDataSetChanged()
                        }
                    }
                }
            })
    }

}