package com.hermesrelay.agent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hermesrelay.agent.data.PreferencesManager
import com.hermesrelay.agent.ui.home.HomeScreen
import com.hermesrelay.agent.ui.login.LoginScreen
import com.hermesrelay.agent.ui.theme.HermesRelayTheme
import com.hermesrelay.agent.viewmodel.HomeViewModel
import com.hermesrelay.agent.viewmodel.LoginViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                            homeViewModel.logout()
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
}
