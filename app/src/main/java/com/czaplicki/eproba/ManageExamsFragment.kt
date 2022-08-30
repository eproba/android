package com.czaplicki.eproba

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.czaplicki.eproba.api.EprobaApi
import com.czaplicki.eproba.api.EprobaService
import com.czaplicki.eproba.databinding.FragmentManageExamsBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import net.openid.appauth.AuthorizationService


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class ManageExamsFragment : Fragment() {

    private var _binding: FragmentManageExamsBinding? = null

    private lateinit var mAuthStateManager: AuthStateManager
    private lateinit var authService: AuthorizationService
    private var recyclerView: RecyclerView? = null
    private val mSwipeRefreshLayout by lazy { _binding!!.swipeRefreshLayout }
    private val api: EprobaApi = EprobaApi()
    var examList: MutableList<Exam> = mutableListOf()
    private val binding get() = _binding!!
    private val userDao: UserDao by lazy { (activity?.application as EprobaApplication).database.userDao() }
    var users: MutableList<User> = mutableListOf()
    val sharedPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(
            requireContext()
        )
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
        recyclerView?.adapter = ManagedExamAdapter(examList, users)
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

        return binding.root

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        updateExams()
        (activity as? MainActivity)?.bottomNavigation?.setOnItemReselectedListener {
            recyclerView?.smoothScrollToPosition(0)
        }
    }

    private fun updateExams() {
        mAuthStateManager.current.performActionWithFreshTokens(
            authService
        ) { accessToken, _, _ ->
            if (accessToken == null) {
                requireActivity().recreate()
                return@performActionWithFreshTokens
            }
            mAuthStateManager.updateSavedState()
            mSwipeRefreshLayout.isRefreshing = true
            api.getRetrofitInstance(requireContext(), accessToken)!!
                .create(EprobaService::class.java).getExams()
                .enqueue(object : retrofit2.Callback<List<Exam>> {
                    override fun onFailure(call: retrofit2.Call<List<Exam>>, t: Throwable) {
                        Snackbar.make(
                            binding.root,
                            "Błąd połączenia z serwerem",
                            Snackbar.LENGTH_LONG
                        ).show()
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
                            if (sharedPreferences
                                    .getBoolean("ads", true)
                            ) examList.add(Exam(id = -1, name = "ad"))
                        } else {
                            Snackbar.make(
                                binding.root,
                                "Błąd połączenia z serwerem",
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                        recyclerView?.adapter?.notifyDataSetChanged()
                        mSwipeRefreshLayout.isRefreshing = false
                        val userIds: MutableSet<Int> = mutableSetOf()
                        examList.forEach {
                            if (it.userId != null) userIds.add(it.userId!!)
                            if (it.supervisor != null) userIds.add(it.supervisor!!)
                            if (it.tasks.isNotEmpty()) it.tasks.forEach { task ->
                                if (task.approver != null) userIds.add(task.approver!!)
                            }
                        }
                        userIds.filter { id -> users.find { it.id == id } == null }.forEach { id ->
                            api.getRetrofitInstance(requireContext(), accessToken)!!
                                .create(EprobaService::class.java).getUserInfo(id)
                                .enqueue(object : retrofit2.Callback<User> {
                                    override fun onFailure(
                                        call: retrofit2.Call<User>,
                                        t: Throwable
                                    ) {
                                        Snackbar.make(
                                            binding.root,
                                            "Błąd połączenia z serwerem",
                                            Snackbar.LENGTH_SHORT
                                        ).show()
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
    }

    private fun getUsers() {
        mAuthStateManager.current.performActionWithFreshTokens(
            authService
        ) { accessToken, _, _ ->
            if (accessToken == null) {
                requireActivity().recreate()
                return@performActionWithFreshTokens
            }
            mAuthStateManager.updateSavedState()
            api.getRetrofitInstance(requireContext(), accessToken)!!
                .create(EprobaService::class.java).getUsersPublicInfo()
                .enqueue(object : retrofit2.Callback<List<User>> {
                    override fun onFailure(call: retrofit2.Call<List<User>>, t: Throwable) {
                        Snackbar.make(
                            binding.root,
                            "Błąd połączenia z serwerem",
                            Snackbar.LENGTH_LONG
                        ).show()
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
                                .putLong("lastUsersUpdate", System.currentTimeMillis()).apply()
                            recyclerView?.adapter?.notifyDataSetChanged()
                        } else {
                            Snackbar.make(
                                binding.root,
                                "Błąd połączenia z serwerem",
                                Snackbar.LENGTH_LONG
                            ).show()
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

}