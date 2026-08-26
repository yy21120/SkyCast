package com.yy21120.skycast

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yy21120.skycast.ui.OpportunityViewModel
import com.yy21120.skycast.ui.SkyCastApp
import com.yy21120.skycast.ui.theme.SkyCastTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SkyCastTheme {
                val opportunityViewModel: OpportunityViewModel = viewModel(
                    factory = OpportunityViewModel.factory(
                        applicationContext = applicationContext,
                        baseUrl = BuildConfig.API_BASE_URL,
                    ),
                )
                SkyCastApp(viewModel = opportunityViewModel)
            }
        }
    }
}
