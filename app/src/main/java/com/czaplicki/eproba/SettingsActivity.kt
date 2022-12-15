package com.czaplicki.eproba

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import net.openid.appauth.AuthState

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            val serverPreference = findPreference<ListPreference>("server")
            val oldServerValue = serverPreference?.value
            serverPreference?.setOnPreferenceChangeListener { _, newValue ->
                if (newValue != oldServerValue) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Zmiana serwera")
                        .setMessage("Zmiana serwera spowoduje wylogowanie")
                        .setPositiveButton("OK") { _, _ ->
                            EprobaApplication.instance.authStateManager.replace(AuthState())
                            val refresh = Intent(requireContext(), MainActivity::class.java)
                            refresh.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            startActivity(refresh)
                        }
                        .setNegativeButton("Anuluj") { _, _ ->
                            serverPreference.value = oldServerValue
                        }
                        .show()
                }
                true
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}