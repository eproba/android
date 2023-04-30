package com.czaplicki.eproba

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.czaplicki.eproba.databinding.FragmentMaintenanceBinding


class MaintenanceScreen() : DialogFragment() {

    private lateinit var binding: FragmentMaintenanceBinding

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
        binding = FragmentMaintenanceBinding.inflate(inflater, container, false)

        binding.button.setOnClickListener {
            val refresh = Intent(activity, MainActivity::class.java)
            refresh.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(refresh)
        }

        return binding.root
    }

}