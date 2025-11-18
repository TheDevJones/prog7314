package com.st10028374.vitality_vault.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.spotify.sdk.android.auth.AuthorizationClient
import com.st10028374.vitality_vault.R
import com.st10028374.vitality_vault.databinding.ActivityMainBinding
import com.st10028374.vitality_vault.routes.RoutesFragment
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("MainActivity", "Notification permission granted")
        } else {
            Log.d("MainActivity", "Notification permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestNotificationPermission()

        setupBottomNavigation()

        if (savedInstanceState == null) {
            selectNavItem(R.id.navDashboard)
            loadFragment(DashboardFragment())
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                    Log.d("MainActivity", "Notification permission already granted")
                }
                else -> {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }}


    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1337) {
            val response = AuthorizationClient.getResponse(resultCode, data)
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
            if (currentFragment is MusicFragment) {
                currentFragment.onAuthorizationResponse(response)
            }
        }
    }

    private fun setupBottomNavigation() {
        binding.navDashboard.setOnClickListener {
            selectNavItem(R.id.navDashboard)
            loadFragment(DashboardFragment())
        }
        binding.navSocial.setOnClickListener {
            selectNavItem(R.id.navSocial)
            loadFragment(SocialFragment())
        }
        binding.navRoutes.setOnClickListener {
            selectNavItem(R.id.navRoutes)
            loadFragment(RoutesFragment())
        }
        binding.navMusic.setOnClickListener {
            selectNavItem(R.id.navMusic)
            loadFragment(MusicFragment())
        }
    }



    private fun selectNavItem(selectedId: Int) {
        resetNavItem(binding.dashboardBackground, binding.iconDashboard, binding.labelDashboard, R.drawable.ic_dashboard_white)
        resetNavItem(binding.socialBackground, binding.iconSocial, binding.labelSocial, R.drawable.ic_social_white)
        resetNavItem(binding.routesBackground, binding.iconRoutes, binding.labelRoutes, R.drawable.ic_navigation_white)
        resetNavItem(binding.musicBackground, binding.iconMusic, binding.labelMusic, R.drawable.ic_music_white)

        when (selectedId) {
            R.id.navDashboard -> activateNavItem(binding.dashboardBackground, binding.iconDashboard, binding.labelDashboard, R.drawable.ic_dashboard_black)
            R.id.navSocial -> activateNavItem(binding.socialBackground, binding.iconSocial, binding.labelSocial, R.drawable.ic_social_black)
            R.id.navRoutes -> activateNavItem(binding.routesBackground, binding.iconRoutes, binding.labelRoutes, R.drawable.ic_navigation_black)
            R.id.navMusic -> activateNavItem(binding.musicBackground, binding.iconMusic, binding.labelMusic, R.drawable.ic_music_black)
        }
    }

    private fun resetNavItem(
        background: View,
        icon: android.widget.ImageView,
        label: android.widget.TextView,
        whiteIconRes: Int
    ) {
        background.visibility = View.GONE
        icon.setImageResource(whiteIconRes)
        label.setTextColor(ContextCompat.getColor(this, R.color.white))
        label.typeface = android.graphics.Typeface.DEFAULT
    }

    private fun activateNavItem(
        background: View,
        icon: android.widget.ImageView,
        label: android.widget.TextView,
        blackIconRes: Int
    ) {
        background.visibility = View.VISIBLE
        icon.setImageResource(blackIconRes)
        label.setTextColor(ContextCompat.getColor(this, R.color.black))
        label.typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    override fun onDestroy() {
        super.onDestroy()
        SpotifyManager.stopPlaybackAndDisconnect()
    }



}
