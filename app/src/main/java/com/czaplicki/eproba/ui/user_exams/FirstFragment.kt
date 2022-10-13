package com.czaplicki.eproba.ui.user_exams

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.*
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
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.czaplicki.eproba.AuthStateManager
import com.czaplicki.eproba.EprobaApplication
import com.czaplicki.eproba.MainActivity
import com.czaplicki.eproba.R
import com.czaplicki.eproba.api.EprobaApi
import com.czaplicki.eproba.api.EprobaService
import com.czaplicki.eproba.databinding.FragmentFirstBinding
import com.czaplicki.eproba.db.Exam
import com.czaplicki.eproba.db.ExamDao
import com.czaplicki.eproba.db.User
import com.czaplicki.eproba.db.UserDao
import com.google.android.material.color.MaterialColors
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import net.openid.appauth.AuthorizationService
import java.util.*


class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    private lateinit var mAuthStateManager: AuthStateManager
    private lateinit var authService: AuthorizationService
    private var recyclerView: RecyclerView? = null
    private lateinit var mSwipeRefreshLayout: SwipeRefreshLayout
    private val api: EprobaApi = EprobaApi()
    private var searchView: SearchView? = null
    var originalExamList: MutableList<Exam> = mutableListOf()
    var examList: MutableList<Exam> = mutableListOf()
    private val binding get() = _binding!!
    private val userDao: UserDao by lazy { (activity?.application as EprobaApplication).database.userDao() }
    private val examDao: ExamDao by lazy { (activity?.application as EprobaApplication).database.examDao() }
    private lateinit var service: EprobaService
    var users: MutableList<User> = mutableListOf()
    val viewModel: ExamsViewModel by viewModels { ExamsViewModel.Factory }
    val sharedPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(
            requireContext()
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        service = (activity?.application as EprobaApplication).service()
        mAuthStateManager = AuthStateManager.getInstance(requireContext())
        authService = AuthorizationService(requireContext())
        recyclerView = binding.recyclerView
        recyclerView?.layoutManager = LinearLayoutManager(view?.context)
        recyclerView?.adapter = ExamAdapter(examList, users)
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


        (activity as MainActivity).user?.let { viewModel.setUserId(it.id) }
        lifecycle.coroutineScope.launch {
            viewModel.exams.collect {
                examList.clear()
                examList.addAll(it)
                if (examList.isEmpty()) {
                    examList.add(Exam(id = -1, name = "no_exams"))
                }
                if (sharedPreferences
                        .getBoolean("ads", true)
                ) examList.add(Exam(id = -1, name = "ad"))
                originalExamList = examList.toMutableList()
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
        sharedPreferences.getLong("lastUsersUpdate", 0).let {
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        service = (activity?.application as EprobaApplication).service()
        Log.d("onResume", "onResume")
        Log.d("onResume", (activity as MainActivity).user.toString())
        (activity as MainActivity).user?.let {
            Log.d("onResume", "user not null")
            Log.d("onResume", it.toString())
            if (viewModel.savedStateHandle.get<Long>("user_id") != it.id) {
                viewModel.setUserId(it.id)
            }
        }
        (activity as MainActivity).user?.let { viewModel.setUserId(it.id) }
        if (mAuthStateManager.current.isAuthorized) {
            updateExams()
        }
        (activity as? MainActivity)?.bottomNavigation?.setOnItemReselectedListener {
            recyclerView?.smoothScrollToPosition(0)
        }
    }

    private fun updateExams() {
        mSwipeRefreshLayout.isRefreshing = true
        (activity as MainActivity).user?.let { viewModel.setUserId(it.id) }
        service.getUserExams()
            .enqueue(object : retrofit2.Callback<List<Exam>> {
                override fun onFailure(call: retrofit2.Call<List<Exam>>, t: Throwable) {
                    view?.let {
                        Snackbar.make(
                            it,
                            "Błąd połączenia z serwerem",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                    mSwipeRefreshLayout.isRefreshing = false
                }

                override fun onResponse(
                    call: retrofit2.Call<List<Exam>>,
                    response: retrofit2.Response<List<Exam>>
                ) {
                    if (response.body() != null) {
                        lifecycleScope.launch {
                            examDao.deleteExams()
                            examDao.insertExams(*response.body()!!.toTypedArray())
                        }
                    } else {
                        view?.let {
                            Snackbar.make(
                                it,
                                "Błąd połączenia z serwerem",
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                    }
                    mSwipeRefreshLayout.isRefreshing = false
                    val userIds: MutableSet<Long> = mutableSetOf()
                    examList.forEach {
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
                                            "lastUsersUpdate",
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


    private fun getUsers(previousResponseCode: Int = 0) {
        service.getUsersPublicInfo()
            .enqueue(object : retrofit2.Callback<List<User>> {
                override fun onFailure(call: retrofit2.Call<List<User>>, t: Throwable) {
                    view?.let {
                        Snackbar.make(
                            it,
                            "Błąd połączenia z serwerem",
                            Toast.LENGTH_SHORT
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
                    if (response.code() == 403) {
                        if (previousResponseCode == 403) {
                            view?.let {
                                Snackbar.make(
                                    it,
                                    "Nie jesteś zalogowany",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            service = api.create(requireContext(), null)!!
                                .create(EprobaService::class.java)
                            return getUsers(403)
                        }
                    } else if (response.body() != null) {
                        users.clear()
                        users.addAll(response.body()!!)
                        lifecycleScope.launch {
                            userDao.insertUsers(*users.toTypedArray())
                        }
                        sharedPreferences.edit()
                            .putLong("lastUsersUpdate", System.currentTimeMillis()).apply()
                        recyclerView?.adapter?.notifyDataSetChanged()
                    } else {
                        view?.let {
                            Snackbar.make(
                                it,
                                "Błąd połączenia z serwerem",
                                Toast.LENGTH_LONG
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


    private fun filter(text: String) {
        val filteredList: ArrayList<Exam> = ArrayList<Exam>()

        for (item in originalExamList.filter { it.id != -1L }) {
            if (item.name!!.lowercase(Locale.ROOT).contains(text.lowercase(Locale.getDefault()))) {
                filteredList.add(item)
            }
        }
        if (filteredList.isEmpty()) {
            filteredList.add(Exam(id = -1, name = "no_exams"))
        }
        if (sharedPreferences
                .getBoolean("ads", true)
        ) filteredList.add(Exam(id = -1, name = "ad"))
        (recyclerView?.adapter as ExamAdapter).filterList(filteredList)
    }

}