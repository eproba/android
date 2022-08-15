package com.czaplicki.eproba

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.czaplicki.eproba.api.EprobaApi
import com.czaplicki.eproba.api.EprobaService
import com.czaplicki.eproba.databinding.FragmentFirstBinding
import com.google.android.material.snackbar.Snackbar
import net.openid.appauth.AuthorizationService


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    private lateinit var mAuthStateManager: AuthStateManager
    private lateinit var authService: AuthorizationService
    private var recyclerView: RecyclerView? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        var examList: List<Exam> = listOf()
        mAuthStateManager = AuthStateManager.getInstance(requireContext())
        authService = AuthorizationService(requireContext())
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        binding.progressBar.visibility = View.VISIBLE
        recyclerView = binding.recyclerView
        recyclerView?.layoutManager = LinearLayoutManager(view?.context)
        recyclerView?.adapter = ExamAdapter(examList)
        mAuthStateManager.current.performActionWithFreshTokens(
            authService
        ) { accessToken, _, _ ->
            if (accessToken == null) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(
                    requireContext(),
                    "No access token",
                    Toast.LENGTH_SHORT
                ).show()
                return@performActionWithFreshTokens
            }
            mAuthStateManager.updateSavedState()
            val apiService: EprobaService =
                EprobaApi().getRetrofitInstance(accessToken)!!
                    .create(EprobaService::class.java)
            apiService.getUserExams().enqueue(object : retrofit2.Callback<List<Exam>> {
                override fun onFailure(call: retrofit2.Call<List<Exam>>, t: Throwable) {
                    binding.progressBar.visibility = View.GONE
                    Snackbar.make(
                        binding.root,
                        "Błąd połączenia z serwerem",
                        Snackbar.LENGTH_LONG
                    ).show()
                    t.message?.let { Log.e("FirstFragment", it) }
                }

                override fun onResponse(
                    call: retrofit2.Call<List<Exam>>,
                    response: retrofit2.Response<List<Exam>>
                ) {
                    binding.progressBar.visibility = View.GONE
                    if (response.body() != null) {
                        examList = response.body()!!
                    } else {
                        Snackbar.make(
                            binding.root,
                            "Błąd połączenia z serwerem",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                    recyclerView?.adapter = ExamAdapter(examList)
                }
            })
        }


        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonFirst.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_CameraFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}