package com.andorid.crackmaster

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.andorid.crackmaster.ui.screens.DashboardScreen
import com.andorid.crackmaster.ui.theme.BackgroundDark
import com.andorid.crackmaster.ui.theme.CrackMasterTheme
import com.andorid.crackmaster.ui.viewmodel.CrackMasterViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: CrackMasterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CrackMasterTheme {
                DashboardScreen(
                    viewModel = viewModel,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BackgroundDark)
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshSystemAndGameData()
    }
}
