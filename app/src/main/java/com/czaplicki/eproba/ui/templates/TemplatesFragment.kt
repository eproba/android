package com.czaplicki.eproba.ui.templates

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.czaplicki.eproba.CreateWorksheetActivity
import com.czaplicki.eproba.EprobaApplication
import com.czaplicki.eproba.databinding.FragmentTemplatesBinding
import com.czaplicki.eproba.db.Worksheet
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.launch
import java.util.UUID

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
        _binding = FragmentTemplatesBinding.inflate(inflater, container, false)
        val root: View = binding.root


        val recyclerView = binding.recyclerView
        val worksheetLists: MutableList<Worksheet> = mutableListOf(Worksheet(UUID.fromString("00000000-0000-0000-0000-000000000000"), "no_worksheets"))
        lifecycleScope.launch {
            worksheetLists.clear()
            worksheetLists.addAll(EprobaApplication.instance.service.getTemplates())
            recyclerView.adapter?.notifyDataSetChanged()
        }
        recyclerView.layoutManager = LinearLayoutManager(view?.context)
        recyclerView.adapter = TemplateWorksheetAdapter(worksheetLists)
        val swipeRefreshLayout = binding.swipeRefreshLayout
        swipeRefreshLayout.setColorSchemeColors(
            MaterialColors.getColor(
                binding.root,
                com.google.android.material.R.attr.colorPrimary
            )
        )
        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(
            MaterialColors.getColor(
                binding.root,
                com.google.android.material.R.attr.colorSurfaceVariant
            )
        )

        (requireActivity() as CreateWorksheetActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        return root
    }

    override fun onResume() {
        super.onResume()
        (activity as? CreateWorksheetActivity)?.bottomNavigationView?.setOnItemReselectedListener {
//            binding.scrollView.fullScroll(View.FOCUS_UP)
            (activity as? CreateWorksheetActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(
                true
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}