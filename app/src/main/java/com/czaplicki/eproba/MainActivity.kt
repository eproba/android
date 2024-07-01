package com.czaplicki.eproba

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.czaplicki.eproba.api.APIState
import com.czaplicki.eproba.databinding.ActivityMainBinding
import com.czaplicki.eproba.db.User
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import kotlinx.coroutines.launch
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    lateinit var fab: ExtendedFloatingActionButton
    lateinit var bottomNavigation: BottomNavigationView
    val navController by lazy { findNavController(R.id.nav_host_fragment_content_main) }
    private lateinit var authService: AuthorizationService
    private lateinit var mAuthStateManager: AuthStateManager
    private val service = EprobaApplication.instance.service
    var user: User? = null
    private lateinit var appUpdateActivityResultLauncher: ActivityResultLauncher<IntentSenderRequest>
    private val appUpdateManager by lazy { AppUpdateManagerFactory.create(this) }


    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        user = Gson().fromJson(
            PreferenceManager.getDefaultSharedPreferences(this).getString("user", null),
            User::class.java
        )
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
            if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("theEnd", false)) {
                val endScreen = EndScreen()
                endScreen.show(supportFragmentManager, "end")
            }
        }

        // Create channel to show notifications.
        val channelId = "general"
        val channelName = getString(R.string.general_notification_channel_name)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.createNotificationChannel(
            NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
        lifecycleScope.launch {
            when (EprobaApplication.instance.apiHelper.getAndProcessAppConfig()) {

                APIState.END_OF_LIFE -> {
                    val endScreen = EndScreen()
                    endScreen.show(supportFragmentManager, "end")
                }


                APIState.MAINTENANCE -> {
                    val maintenanceScreen = MaintenanceScreen()
                    maintenanceScreen.show(supportFragmentManager, "maintenance")
                }

                APIState.UPDATE_REQUIRED -> {
                    val appUpdateInfoTask = appUpdateManager.appUpdateInfo
                    appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
                        if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                            appUpdateManager.startUpdateFlowForResult(
                                appUpdateInfo,
                                appUpdateActivityResultLauncher,
                                AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                            )
                        } else {
                            val errorScreen =
                                ErrorScreen("Update is required, but no update is available")
                            errorScreen.show(supportFragmentManager, "error")
                        }
                    }
                }


                else -> {
                    // Do nothing
                }
            }
        }

        appUpdateActivityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result: ActivityResult ->
            // handle callback
            if (result.resultCode != RESULT_OK) {
                val errorScreen = ErrorScreen("Update failed")
                errorScreen.show(supportFragmentManager, "error")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!mAuthStateManager.current.isAuthorized) {
            navController.navigate(R.id.action_global_loginFragment)
        } else if (navController.currentDestination?.id == R.id.LoginFragment && mAuthStateManager.current.isAuthorized) {
            Log.d("Login", "Logged in")
            navController.navigate(R.id.action_LoginFragment_to_navigation_your_exams)
        }
        user = Gson().fromJson(
            PreferenceManager.getDefaultSharedPreferences(this).getString("user", null),
            User::class.java
        )
        if (user == null || !mAuthStateManager.current.isAuthorized) {
            bottomNavigation.visibility = View.GONE
        } else if (user!!.scout.function < 2) {
            bottomNavigation.visibility = View.GONE
            if (navController.currentDestination?.id == R.id.navigation_manage_exams || navController.currentDestination?.id == R.id.navigation_accept_tasks) {
                navController.navigate(R.id.navigation_your_exams)
            }
        } else if (navController.currentDestination?.id == R.id.navigation_edit_exam) {
            bottomNavigation.visibility = View.GONE
        } else {
            bottomNavigation.visibility = View.VISIBLE
        }
        if (PreferenceManager.getDefaultSharedPreferences(this)
                .getString("server", "https://eproba.pl") != "https://eproba.pl"
        ) {
            binding.devServerStatus.visibility = View.VISIBLE
        } else {
            binding.devServerStatus.visibility = View.GONE
        }


        appUpdateManager
            .appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability()
                    == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                ) {
                    // If an in-app update is already running, resume the update.
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        appUpdateActivityResultLauncher,
                        AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build())
                }
            }
    }


    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }


    private fun isOnline(context: Context): Boolean {
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
            .getString("server", "https://eproba.pl")
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
                fetchUser()
                FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                        return@OnCompleteListener
                    }

                    // Get new FCM registration token
                    val token = task.result
                    lifecycleScope.launch {
                        EprobaApplication.instance.apiHelper.registerFCMToken(token)
                    }
                    Log.d(TAG, "FCM token: $token")
                })
            } else {
                if (ex != null) {
                    Toast.makeText(this, "Error: ${ex.error}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }


    private fun fetchUser() {
        service.getUserCall().enqueue(object : Callback<User> {
            override fun onFailure(call: Call<User>, t: Throwable) {
                val errorScreen = ErrorScreen(t.message)
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
                    if (response.body()!!.scout.function < 2) {
                        bottomNavigation.visibility = View.GONE
                    } else {
                        bottomNavigation.visibility = View.VISIBLE
                    }
                    user = response.body()!!
                    EprobaApplication.instance.apiHelper.user = user
                    navController.navigate(R.id.navigation_your_exams)
                    if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        NotificationsRequestScreen().show(supportFragmentManager, "notifications")
                    }
                } else {
                    val errorScreen = ErrorScreen("Error: ${response.message()}")
                    errorScreen.show(supportFragmentManager, "error")
                }
            }
        })
    }

}