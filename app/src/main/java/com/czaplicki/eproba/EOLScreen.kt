package com.czaplicki.eproba

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.czaplicki.eproba.api.ApiConfig
import com.czaplicki.eproba.databinding.FragmentEolBinding
import java.time.Instant
import androidx.core.net.toUri
import java.time.format.DateTimeFormatter


class EOLScreen(private val apiConfig: ApiConfig) : DialogFragment() {

    private lateinit var binding: FragmentEolBinding
    private val allowSkip = Instant.now().isBefore(apiConfig.eolDate.toInstant())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = allowSkip

        setStyle(STYLE_NORMAL, com.google.android.material.R.style.MaterialAlertDialog_Material3)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentEolBinding.inflate(inflater, container, false)

        binding.title.text = apiConfig.eolScreenTitle
        binding.description.text = apiConfig.eolScreenDescription
        binding.downloadButton.text = apiConfig.eolScreenButtonText

        binding.downloadButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = apiConfig.eolScreenButtonUrl.toUri()
            }
            startActivity(intent)
        }

        binding.skipButton.visibility = if (allowSkip) View.VISIBLE else View.GONE
        binding.skipButton.setOnClickListener {
            dismiss()
        }
        binding.skipText.visibility = if (allowSkip) View.VISIBLE else View.GONE
        val formattedDate = apiConfig.eolDate.toInstant()
            .atZone(java.time.ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))

        binding.skipText.text = "Będziesz mógł korzystać z aplikacji do ${
            formattedDate
        }, ale nie będzie ona już aktualizowana."

        return binding.root
    }

}