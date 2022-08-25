package com.czaplicki.eproba

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.czaplicki.eproba.databinding.FragmentLoginBinding
import net.openid.appauth.AuthorizationService


class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private lateinit var mAuthStateManager: AuthStateManager
    private lateinit var authService: AuthorizationService
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        mAuthStateManager = AuthStateManager.getInstance(requireContext())
        authService = AuthorizationService(requireContext())
        binding.loginButton.setOnClickListener {
            (activity as? MainActivity)?.startAuth()
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