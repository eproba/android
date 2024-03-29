package com.czaplicki.eproba

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.czaplicki.eproba.databinding.FragmentErrorBinding


class ErrorScreen(private val message: String? = null) : DialogFragment() {

    private lateinit var binding: FragmentErrorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false

        setStyle(STYLE_NORMAL, com.google.android.material.R.style.MaterialAlertDialog_Material3)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentErrorBinding.inflate(inflater, container, false)

        binding.button.setOnClickListener {
            val refresh = Intent(activity, MainActivity::class.java)
            refresh.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(refresh)
        }

        if (message != null) {
            binding.errorMessage.text = message
        } else {
            binding.errorMessage.visibility = View.GONE
        }

        return binding.root
    }

}