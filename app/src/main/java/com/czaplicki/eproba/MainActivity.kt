package com.czaplicki.eproba

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.core.view.WindowCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.czaplicki.eproba.api.EprobaApi
import com.czaplicki.eproba.api.EprobaService
import com.czaplicki.eproba.databinding.ActivityMainBinding
import com.czaplicki.eproba.db.User
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.gson.Gson
import net.openid.appauth.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class MainActivity : AppCompatActivity() {
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    lateinit var fab: ExtendedFloatingActionButton
    lateinit var bottomNavigation: BottomNavigationView
    private val navController by lazy { findNavController(R.id.nav_host_fragment_content_main) }
    private lateinit var authService: AuthorizationService
    private lateinit var mAuthStateManager: AuthStateManager
    val user: User? by lazy {
        Gson().fromJson(
            PreferenceManager.getDefaultSharedPreferences(this).getString("user", null),
            User::class.java
        )
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)


        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)
        fab = binding.fab
        bottomNavigation = binding.bottomNavigation
        bottomNavigation.setupWithNavController(navController)

        fab.setOnClickListener {
            navController.navigate(R.id.action_global_createExamActivity)
        }
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.navigation_your_exams, R.id.navigation_manage_exams -> {
                    fab.show()
                    if (user == null) {
                        bottomNavigation.visibility = View.GONE
                    } else if (user!!.scout.function < 2) {
                        bottomNavigation.visibility = View.GONE
                    } else {
                        bottomNavigation.visibility = View.VISIBLE
                    }
                }
                R.id.navigation_accept_tasks -> {
                    fab.hide()
                    if (user == null) {
                        bottomNavigation.visibility = View.GONE
                    } else if (user!!.scout.function < 2) {
                        bottomNavigation.visibility = View.GONE
                    } else {
                        bottomNavigation.visibility = View.VISIBLE
                    }
                }
                else -> {
                    fab.hide()
                    bottomNavigation.visibility = View.GONE
                }
            }
        }
        mAuthStateManager = AuthStateManager.getInstance(this)
        authService = AuthorizationService(this)
        MobileAds.initialize(this)
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setTestDeviceIds(
                    listOf(
                        "59822EDA71A89C033EEBD914F011B2EA",
                        "FE83E16FC98F8E48043F402CC6C4800F"
                    )
                ).build()
        )


        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Add menu items here
                menuInflater.inflate(R.menu.menu_main, menu)
            }

            override fun onMenuItemSelected(item: MenuItem): Boolean {
                // Handle the menu selection
                return when (item.itemId) {
                    R.id.account -> {
                        val navController = findNavController(R.id.nav_host_fragment_content_main)
                        navController.navigate(R.id.action_global_profileActivity)
                        true
                    }
                    R.id.settings -> {
                        val navController = findNavController(R.id.nav_host_fragment_content_main)
                        navController.navigate(R.id.action_global_settingsActivity)
                        true
                    }
                    else -> false
                }
            }
        })
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            // network is available for use
            override fun onAvailable(network: Network) {
                runOnUiThread {
                    binding.networkStatus.visibility = View.GONE
                }
                super.onAvailable(network)
            }

            // lost network connection
            override fun onLost(network: Network) {
                runOnUiThread {
                    binding.networkStatus.visibility = View.VISIBLE
                }
                super.onLost(network)
            }
        }
        val connectivityManager =
            getSystemService(ConnectivityManager::class.java) as ConnectivityManager
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
        if (!isOnline(this)) {
            binding.networkStatus.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        if (navController.currentDestination?.id == R.id.navigation_your_exams && !mAuthStateManager.current.isAuthorized) {
            navController.navigate(R.id.action_FirstFragment_to_LoginFragment)
        } else if (navController.currentDestination?.id == R.id.LoginFragment && mAuthStateManager.current.isAuthorized) {
            navController.navigate(R.id.action_LoginFragment_to_FirstFragment)
        }
    }


    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }


    fun isOnline(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        if (capabilities != null) {
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                return true
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return true
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                return true
            }
        }
        return false
    }

    fun startAuth() {
        val redirectUri = Uri.parse("com.czaplicki.eproba://oauth2redirect")
        val clientId = "57wXiwkX1865qziVedFEXXum01m9QHJ6MDMVD03i"
        val baseUrl = PreferenceManager.getDefaultSharedPreferences(this)
            .getString("server", "https://dev.eproba.pl")
        val builder = AuthorizationRequest.Builder(
            AuthorizationServiceConfiguration(
                Uri.parse("$baseUrl/oauth2/authorize/"), // authorization endpoint
                Uri.parse("$baseUrl/oauth2/token/") // token endpoint
            ),
            clientId,
            ResponseTypeValues.CODE,
            redirectUri
        )
        builder.setScopes("read write")

        val authRequest = builder.build()
        authService = AuthorizationService(this)
        val authIntent = authService.getAuthorizationRequestIntent(authRequest)
        getAuthorizationCode.launch(authIntent)
    }

    private val getAuthorizationCode =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val resp = AuthorizationResponse.fromIntent(it.data!!)
                val ex = AuthorizationException.fromIntent(it.data)
                if (resp != null) {
                    mAuthStateManager.updateAfterAuthorization(resp, ex)
                    exchangeCodeForToken()
                }
            }
        }

    // exchange authorization code for access token
    private fun exchangeCodeForToken() {
        if (mAuthStateManager.current.lastAuthorizationResponse == null) {
            Toast.makeText(this, "Authorization response is null", Toast.LENGTH_LONG)
                .show()
            return
        }
        authService.performTokenRequest(
            mAuthStateManager.current.lastAuthorizationResponse!!.createTokenExchangeRequest()
        ) { resp, ex ->
            if (resp != null) {
                mAuthStateManager.updateAfterTokenResponse(resp, ex)
                val apiService: EprobaService =
                    EprobaApi().create(this, mAuthStateManager.current.accessToken!!)!!
                        .create(EprobaService::class.java)
                apiService.getUserInfo().enqueue(object : Callback<User> {
                    override fun onFailure(call: Call<User>, t: Throwable) {
                        Toast.makeText(this@MainActivity, "Error: ${t.message}", Toast.LENGTH_LONG)
                            .show()
                        val errorScreen = ErrorScreen()
                        errorScreen.isCancelable = false
                        errorScreen.show(supportFragmentManager, "error")
                    }

                    override fun onResponse(
                        call: Call<User>,
                        response: Response<User>
                    ) {
                        if (response.body() != null) {
                            PreferenceManager.getDefaultSharedPreferences(this@MainActivity)
                                .edit()
                                .putString("user", Gson().toJson(response.body()))
                                .apply()
                            this@MainActivity.recreate()
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                "Error: ${response.message()}",
                                Toast.LENGTH_LONG
                            ).show()
                            val errorScreen = ErrorScreen()
                            errorScreen.isCancelable = false
                            errorScreen.show(supportFragmentManager, "error")
                        }
                    }
                })
            } else {
                if (ex != null) {
                    Toast.makeText(this, "Error: ${ex.error}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }


}