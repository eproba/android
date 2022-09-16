package com.czaplicki.eproba

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.czaplicki.eproba.api.EprobaApi
import com.czaplicki.eproba.api.EprobaService
import com.czaplicki.eproba.databinding.FragmentProfileBinding
import com.czaplicki.eproba.db.User
import com.google.android.material.snackbar.Snackbar
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
            val server = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getString("server", "https://dev.eproba.pl")
            PreferenceManager.getDefaultSharedPreferences(requireContext()).edit().clear().apply()
            PreferenceManager.getDefaultSharedPreferences(requireContext()).edit()
                .putString("server", server).apply()
            requireContext().deleteDatabase("eproba.db")
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

    private fun getUserInfo(isAfterUnauthorized: Boolean = false) {
        binding.progressBar.visibility = View.VISIBLE
        mAuthStateManager.current.performActionWithFreshTokens(
            authService
        ) { accessToken, _, _ ->
            mAuthStateManager.updateSavedState()
            val apiService: EprobaService =
                EprobaApi().create(
                    requireContext(),
                    accessToken!!
                )!!
                    .create(EprobaService::class.java)
            apiService.getUserInfo().enqueue(object : Callback<User> {
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
                                .putInt("userId", user.id)
                                .apply()
                            activity?.runOnUiThread {
                                binding.progressBar.visibility = View.GONE
                                binding.name.text = user.fullName
                            }
                        }
                        401, 403 -> {
                            binding.progressBar.visibility = View.GONE
                            activity?.runOnUiThread {
                                binding.name.text = "Unauthorized"
                            }
                            mAuthStateManager.current.needsTokenRefresh = true
                            if (isAfterUnauthorized) {
                                activity?.runOnUiThread {
                                    Snackbar.make(
                                        binding.root,
                                        "Unauthorized",
                                        Snackbar.LENGTH_SHORT
                                    ).setAction(R.string.button_logout) {
                                        mAuthStateManager.replace(AuthState())
                                        Toast.makeText(
                                            requireContext(),
                                            "Logged out",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        requireActivity().finish()
                                    }.show()
                                }
                            } else {
                                activity?.runOnUiThread {
                                    getUserInfo(isAfterUnauthorized = true)
                                }
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
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
