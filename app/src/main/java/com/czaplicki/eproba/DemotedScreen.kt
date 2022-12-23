package com.czaplicki.eproba

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.czaplicki.eproba.databinding.FragmentDemotedBinding


class DemotedScreen : DialogFragment() {

    private lateinit var binding: FragmentDemotedBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NORMAL, com.google.android.material.R.style.MaterialAlertDialog_Material3)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDemotedBinding.inflate(inflater, container, false)

        binding.button.setOnClickListener {
            dialog?.dismiss()
        }

        return binding.root
    }
}