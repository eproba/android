package com.czaplicki.eproba

import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.czaplicki.eproba.databinding.ActivityCreateExamBinding
import com.czaplicki.eproba.db.User
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson

class CreateExamActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateExamBinding
    lateinit var bottomNavigationView: BottomNavigationView
    private val navController by lazy { findNavController(R.id.nav_host_fragment_activity_create_exam) }

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityCreateExamBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        bottomNavigationView = binding.navView

        if (Gson().fromJson(
                PreferenceManager.getDefaultSharedPreferences(this).getString("user", null),
                User::class.java
            ).scout.function < 2
        ) {
            bottomNavigationView.menu.getItem(2).isVisible = false
        }


        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_scan, R.id.navigation_compose, R.id.navigation_templates
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        bottomNavigationView.setupWithNavController(navController)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun finish() {
        val view = window.decorView as ViewGroup
        view.removeAllViews()
        super.finish()
    }
}