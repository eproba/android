package com.czaplicki.eproba

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.czaplicki.eproba.databinding.ActivityCreateExamBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class CreateExamActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateExamBinding
    lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCreateExamBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        bottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_create_exam)
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


}