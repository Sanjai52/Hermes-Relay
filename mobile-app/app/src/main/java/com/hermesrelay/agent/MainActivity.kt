package com.hermesrelay.agent

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hermesrelay.agent.data.PreferencesManager
import com.hermesrelay.agent.ui.home.HomeScreen
import com.hermesrelay.agent.ui.login.LoginScreen
import com.hermesrelay.agent.ui.theme.HermesRelayTheme
import com.hermesrelay.agent.viewmodel.HomeViewModel
import com.hermesrelay.agent.viewmodel.LoginViewModel

class MainActivity : ComponentActivity() {
    private var permissionsGranted = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionsGranted = permissions.values.all { it }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissionsIfNeeded()

        setContent {
            HermesRelayTheme {
                val loginViewModel: LoginViewModel = viewModel()
                val homeViewModel: HomeViewModel = viewModel()

                val email by loginViewModel.email.collectAsState()
                val password by loginViewModel.password.collectAsState()
                val isLoading by loginViewModel.isLoading.collectAsState()
                val error by loginViewModel.error.collectAsState()
                val isLoggedIn by loginViewModel.isLoggedIn.collectAsState()

                val connectionStatus by homeViewModel.connectionStatus.collectAsState()
                val smsLog by homeViewModel.smsLog.collectAsState()
                val isServiceRunning by homeViewModel.isServiceRunning.collectAsState()

                if (isLoggedIn) {
                    HomeScreen(
                        connectionStatus = connectionStatus,
                        smsLog = smsLog,
                        isServiceRunning = isServiceRunning,
                        onStartService = { homeViewModel.startService() },
                        onStopService = { homeViewModel.stopService() },
                        onLogout = {
                            runCatching { homeViewModel.logout() }
                            loginViewModel.logout()
                        }
                    )
                } else {
                    LoginScreen(
                        email = email,
                        password = password,
                        isLoading = isLoading,
                        error = error,
                        onEmailChange = loginViewModel::updateEmail,
                        onPasswordChange = loginViewModel::updatePassword,
                        onLoginClick = {
                            loginViewModel.login(PreferencesManager.DEFAULT_HTTP_URL)
                        }
                    )
                }
            }
        }
    }

    private fun requestPermissionsIfNeeded() {
        val permissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.SEND_SMS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }
}
