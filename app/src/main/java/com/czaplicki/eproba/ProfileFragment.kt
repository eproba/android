package com.czaplicki.eproba

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.czaplicki.eproba.api.EprobaApi
import com.czaplicki.eproba.api.EprobaService
import com.czaplicki.eproba.databinding.FragmentProfileBinding
import com.google.android.material.snackbar.Snackbar
import net.openid.appauth.*
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
        binding.actionButton.text =
            if (mAuthStateManager.current.isAuthorized) getString(R.string.button_logout) else getString(
                R.string.button_login
            )
        binding.actionButton.setOnClickListener {
            if (mAuthStateManager.current.isAuthorized) {
                mAuthStateManager.replace(AuthState())
                Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show()
                requireActivity().finish()
            } else {
                startAuth()
            }
        }
        binding.avatar.setImageDrawable(
            if (mAuthStateManager.current.isAuthorized)
                ContextCompat.getDrawable(requireActivity(), R.drawable.ic_account)
            else
                ContextCompat.getDrawable(requireActivity(), R.drawable.ic_help)
        )
        binding.deleteDB.setOnClickListener {
            requireContext().deleteDatabase("eproba.db")
        }
        if (mAuthStateManager.current.isAuthorized) getUserInfo()
    }

    private fun startAuth() {
        val redirectUri = Uri.parse("com.czaplicki.eproba://oauth2redirect")
        val clientId = "57wXiwkX1865qziVedFEXXum01m9QHJ6MDMVD03i"
        val baseUrl = PreferenceManager.getDefaultSharedPreferences(requireContext())
            .getString("server", "https://dev.eproba.pl")
        val builder = AuthorizationRequest.Builder(
            AuthorizationServiceConfiguration(
                Uri.parse("$baseUrl/oauth2/authorize/"), // authorization endpoint
                Uri.parse("$baseUrl/oauth2/token/") // token endpoint
            ),
            clientId,
            ResponseTypeValues.CODE,
            redirectUri
        )
        builder.setScopes("read write")

        val authRequest = builder.build()
        authService = AuthorizationService(requireContext())
        val authIntent = authService.getAuthorizationRequestIntent(authRequest)
        getAuthorizationCode.launch(authIntent)
    }

    private val getAuthorizationCode =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val resp = AuthorizationResponse.fromIntent(it.data!!)
                val ex = AuthorizationException.fromIntent(it.data)
                if (resp != null) {
                    mAuthStateManager.updateAfterAuthorization(resp, ex)
                    exchangeCodeForToken()
                }
            }
        }

    // exchange authorization code for access token
    private fun exchangeCodeForToken() {
        if (mAuthStateManager.current.lastAuthorizationResponse == null) {
            Toast.makeText(requireContext(), "Authorization response is null", Toast.LENGTH_LONG)
                .show()
            return
        }
        authService.performTokenRequest(
            mAuthStateManager.current.lastAuthorizationResponse!!.createTokenExchangeRequest()
        ) { resp, ex ->
            if (resp != null) {
                mAuthStateManager.updateAfterTokenResponse(resp, ex)
                requireActivity().recreate()
            } else {
                if (ex != null) {
                    Toast.makeText(requireContext(), "Error: ${ex.error}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun getUserInfo(isAfterUnauthorized: Boolean = false) {
        binding.progressBar.visibility = View.VISIBLE
        mAuthStateManager.current.performActionWithFreshTokens(
            authService
        ) { accessToken, _, _ ->
            mAuthStateManager.updateSavedState()
            val apiService: EprobaService =
                EprobaApi().getRetrofitInstance(requireContext(), accessToken!!)!!
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
