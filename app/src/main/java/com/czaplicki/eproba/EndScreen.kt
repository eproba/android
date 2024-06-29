package com.czaplicki.eproba

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.czaplicki.eproba.databinding.FragmentTheEndBinding
import com.google.gson.Gson


class EndScreen() : DialogFragment() {

    private lateinit var binding: FragmentTheEndBinding
    private val sharedPreferences = EprobaApplication.instance.sharedPreferences


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false

        Log.d("EndScreen", "The end")

        setStyle(STYLE_NORMAL, com.google.android.material.R.style.MaterialAlertDialog_Material3)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTheEndBinding.inflate(inflater, container, false)

        val endMessages = Gson().fromJson(
            sharedPreferences.getString("endMessages", null),
            Array<String>::class.java
        )

        binding.button.setOnClickListener {
            activity?.finishAndRemoveTask()
        }

        if (endMessages?.isNotEmpty() == true) {
            binding.additionalEndMessage.text = endMessages.joinToString("\n")
            binding.additionalEndMessage.visibility = View.VISIBLE
        }

        return binding.root
    }

}