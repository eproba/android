package com.czaplicki.eproba.ui.templates

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.czaplicki.eproba.CreateExamActivity
import com.czaplicki.eproba.databinding.FragmentTemplatesBinding

class TemplatesFragment : Fragment() {

    private var _binding: FragmentTemplatesBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val templatesViewModel =
            ViewModelProvider(this)[TemplatesViewModel::class.java]

        _binding = FragmentTemplatesBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textNotifications
        templatesViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        (requireActivity() as CreateExamActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        return root
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as CreateExamActivity).bottomNavigationView.setOnItemReselectedListener {
//            binding.scrollView.fullScroll(View.FOCUS_UP)
            (requireActivity() as CreateExamActivity).supportActionBar?.setDisplayHomeAsUpEnabled(
                true
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}