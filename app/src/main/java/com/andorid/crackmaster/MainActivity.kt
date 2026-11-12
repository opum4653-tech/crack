package com.andorid.crackmaster

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.andorid.crackmaster.ui.screens.DashboardScreen
import com.andorid.crackmaster.ui.screens.LoginScreen
import com.andorid.crackmaster.ui.theme.BackgroundDark
import com.andorid.crackmaster.ui.theme.CrackMasterTheme
import com.andorid.crackmaster.ui.viewmodel.CrackMasterViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: CrackMasterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CrackMasterTheme {
                val state by viewModel.uiState.collectAsState()

                if (!state.isLoggedIn) {
                    // Stage 1 — License key gate before dashboard
                    LoginScreen(
                        viewModel = viewModel,
                        modifier  = Modifier
                            .fillMaxSize()
                            .background(BackgroundDark)
                    )
                } else {
                    // Stage 2 — Dashboard (START triggers InteractiveProcessBridge)
                    DashboardScreen(
                        viewModel = viewModel,
                        modifier  = Modifier
                            .fillMaxSize()
                            .background(BackgroundDark)
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshSystemAndGameData()
    }
}
