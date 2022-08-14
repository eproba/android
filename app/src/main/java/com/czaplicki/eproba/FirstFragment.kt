package com.czaplicki.eproba

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.czaplicki.eproba.api.EprobaApi
import com.czaplicki.eproba.api.EprobaService
import com.czaplicki.eproba.databinding.FragmentFirstBinding
import net.openid.appauth.AuthorizationService

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    private lateinit var mAuthStateManager: AuthStateManager
    private lateinit var authService: AuthorizationService

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        var examList: List<Exam>
        mAuthStateManager = AuthStateManager.getInstance(requireContext())
        authService = AuthorizationService(requireContext())
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        binding.progressBar.visibility = View.VISIBLE
        mAuthStateManager.current.performActionWithFreshTokens(
            authService
        ) { accessToken, _, _ ->
            mAuthStateManager.updateSavedState()
            val apiService: EprobaService =
                EprobaApi().getRetrofitInstance(accessToken!!)!!
                    .create(EprobaService::class.java)
            apiService.getUserExams().enqueue(object : retrofit2.Callback<List<Exam>> {
                override fun onFailure(call: retrofit2.Call<List<Exam>>, t: Throwable) {
                    binding.progressBar.visibility = View.GONE
                    binding.textviewFirst.text = t.message
                    t.message?.let { Log.e("FirstFragment", it) }
                }

                override fun onResponse(
                    call: retrofit2.Call<List<Exam>>,
                    response: retrofit2.Response<List<Exam>>
                ) {
                    binding.progressBar.visibility = View.GONE
                    binding.textviewFirst.text = response.body().toString()
                    examList = response.body()!!
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