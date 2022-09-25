package com.czaplicki.eproba

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.czaplicki.eproba.databinding.FragmentLoginBinding
import net.openid.appauth.AuthorizationService


class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private lateinit var mAuthStateManager: AuthStateManager
    private lateinit var authService: AuthorizationService
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        mAuthStateManager = AuthStateManager.getInstance(requireContext())
        authService = AuthorizationService(requireContext())
        binding.loginButton.setOnClickListener {
            binding.progressBar.visibility = View.VISIBLE
            (activity as? MainActivity)?.startAuth()
        }
        binding.settingsButton.setOnClickListener {
            val navController = findNavController()
            navController.navigate(R.id.action_global_settingsActivity)
        }
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            activity?.finish()
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity?)!!.supportActionBar!!.hide()
    }

    override fun onStop() {
        super.onStop()
        (activity as AppCompatActivity?)!!.supportActionBar!!.show()
    }


}