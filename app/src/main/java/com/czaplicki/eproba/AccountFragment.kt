package com.czaplicki.eproba

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.czaplicki.eproba.databinding.FragmentAccountBinding
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationService
import okhttp3.*
import java.io.IOException

class AccountFragment : Fragment() {

    private var _binding: FragmentAccountBinding? = null

    private val binding get() = _binding!!

    private lateinit var mAuthStateManager: AuthStateManager
    private lateinit var authService: AuthorizationService
    private val client = OkHttpClient()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mAuthStateManager = AuthStateManager.getInstance(requireContext())
        authService = AuthorizationService(requireContext())
        _binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.logoutButton.setOnClickListener {
            mAuthStateManager.replace(AuthState())
        }

        binding.progressBar.visibility = View.VISIBLE

        mAuthStateManager.current.performActionWithFreshTokens(
            authService
        ) { accessToken, _, _ ->
            val request = Request.Builder()
                .url("https://scouts-exams.herokuapp.com/api/user/")
                .header(
                    "Authorization",
                    "Bearer $accessToken"
                )
                .build()


            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                    activity?.runOnUiThread {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!response.isSuccessful) {
                            activity?.runOnUiThread {
                                binding.progressBar.visibility = View.GONE
                            }
                            when (response.code) {
                                401, 403 -> {
                                    activity?.runOnUiThread {
                                        binding.name.text = "Unauthorized"
                                    }
                                }
                                else -> {
                                    activity?.runOnUiThread {
                                        Snackbar.make(
                                            binding.root,
                                            "Error: ${response.code}",
                                            Snackbar.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                            return
                        }
                        val body = response.body.string()
                        val user = Gson().fromJson(body, User::class.java)
                        Log.d("AccountFragment", user.toString())
                        activity?.runOnUiThread {
                            binding.progressBar.visibility = View.GONE
                            binding.name.text = user.fullName
                        }
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
