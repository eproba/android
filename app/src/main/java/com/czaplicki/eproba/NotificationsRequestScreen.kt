package com.czaplicki.eproba

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.fragment.app.DialogFragment
import com.czaplicki.eproba.databinding.FragmentRequestNotificationsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class NotificationsRequestScreen : DialogFragment() {

    private lateinit var binding: FragmentRequestNotificationsBinding
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NORMAL, com.google.android.material.R.style.MaterialAlertDialog_Material3)
        requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    dialog?.dismiss()
                } else {
                    dialog?.dismiss()
                    MaterialAlertDialogBuilder(
                        requireContext(),
                        com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
                    )
                        .setIcon(R.drawable.notifications_off_24px)
                        .setTitle(R.string.permission_denied)
                        .setMessage(R.string.notifications_permission_denied_message)
                        .setPositiveButton(android.R.string.ok) { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                }
            }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRequestNotificationsBinding.inflate(inflater, container, false)

        binding.skip.setOnClickListener {
            dialog?.dismiss()
        }

        binding.enable.setOnClickListener {
            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        return binding.root
    }
}