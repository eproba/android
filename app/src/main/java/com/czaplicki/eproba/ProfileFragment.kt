package com.czaplicki.eproba

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.czaplicki.eproba.databinding.FragmentProfileBinding
import com.czaplicki.eproba.db.User
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null

    private val binding get() = _binding!!

    private lateinit var mAuthStateManager: AuthStateManager
    private lateinit var authService: AuthorizationService

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mAuthStateManager = AuthStateManager.getInstance(requireContext())
        authService = AuthorizationService(requireContext())
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.actionButton.text = getString(R.string.button_logout)
        binding.actionButton.setOnClickListener {
            mAuthStateManager.replace(AuthState())
            PreferenceManager.getDefaultSharedPreferences(requireContext()).edit().clear().apply()
            lifecycleScope.launch((Dispatchers.IO)) {
                EprobaApplication.instance.database.clearAllTables()
            }
            Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show()
            requireActivity().finish()
        }
        binding.avatar.setImageDrawable(
            ContextCompat.getDrawable(
                requireActivity(),
                R.drawable.ic_account
            )
        )
        getUserInfo()
    }

    private fun getUserInfo() {
        binding.progressBar.visibility = View.VISIBLE
        EprobaApplication.instance.service.getUserInfo().enqueue(object : Callback<User> {
            override fun onResponse(call: Call<User>, response: Response<User>) {
                when (response.code()) {
                    200 -> {
                        if (response.body() == null) {
                            Toast.makeText(
                                requireContext(),
                                "Response body is empty, try again later",
                                Toast.LENGTH_LONG
                            )
                                .show()
                            return
                        }
                        val user: User = response.body()!!
                        PreferenceManager.getDefaultSharedPreferences(requireContext())
                            .edit()
                            .putString("user", Gson().toJson(response.body()))
                            .apply()
                        activity?.runOnUiThread {
                            binding.progressBar.visibility = View.GONE
                            binding.name.text = user.fullName
                        }
                    }
                    else -> {
                        activity?.runOnUiThread {
                            Snackbar.make(
                                binding.root,
                                "Error: ${response.code()}",
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }

            override fun onFailure(call: Call<User>, t: Throwable) {
                activity?.runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, t.message, Toast.LENGTH_SHORT).show()
                }
            }
        })
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
