package com.czaplicki.eproba

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.czaplicki.eproba.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import net.openid.appauth.*
import net.openid.appauth.AuthorizationService.TokenResponseCallback


class MainActivity : AppCompatActivity() {
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private val CLIENT_ID = "57wXiwkX1865qziVedFEXXum01m9QHJ6MDMVD03i"
    private val CLIENT_SECRET = "t7QhnuI55YKu0Mk3cmkrv9r96Bp0jvtvxROPckuBUhNyPdLZFTDHc6zK11MTwn96bsYg60iLZ1y2O7MofK055FtGXscsAbmM66GYF0FpNb2OFKG2zEL4R6QNI9UVytYH"

    lateinit var authState: AuthState
    lateinit var authService: AuthorizationService

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.fab.setOnClickListener {
            navController.navigate(R.id.action_FirstFragment_to_CameraFragment)
        }
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.FirstFragment) {
                binding.fab.show()
            } else {
                binding.fab.hide()
            }
        }

        val serviceConfig = AuthorizationServiceConfiguration(
            Uri.parse("https://scouts-exams.herokuapp.com/oauth2/authorize/"), // authorization endpoint
            Uri.parse("https://scouts-exams.herokuapp.com/oauth2/token") // token endpoint
        )

        authState = AuthState(serviceConfig)

        val redirectUri = Uri.parse("com.czaplicki.eproba://oauth2redirect")
        val builder = AuthorizationRequest.Builder(
            serviceConfig,
            CLIENT_ID,
            ResponseTypeValues.CODE,
            redirectUri
        )
        builder.setScopes("read write")

        val authRequest = builder.build()
        authService = AuthorizationService(this)
        val authIntent = authService.getAuthorizationRequestIntent(authRequest)
        getAuthorizationCode.launch(authIntent)
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    private val getAuthorizationCode = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            val resp = AuthorizationResponse.fromIntent(it.data!!)
            val ex = AuthorizationException.fromIntent(it.data)
            Log.d("MainActivity", "resp: $resp")
            if (resp != null) {
                Snackbar.make(binding.root, "Authorization Code: ${resp.authorizationCode}", Snackbar.LENGTH_LONG).show()
            }
        }
    }

}