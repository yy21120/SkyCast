package com.yy21120.skycast.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.yy21120.skycast.data.OpportunityResult
import kotlinx.serialization.Serializable

@Serializable
internal data object OpportunityListRoute

@Serializable
internal data class OpportunityDetailRoute(val sceneId: String)

@Composable
fun SkyCastApp(viewModel: OpportunityViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val context = LocalContext.current

    Surface(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            OpportunityUiState.Loading -> LoadingContent()
            is OpportunityUiState.Error -> ErrorContent(
                message = state.message,
                onRetry = viewModel::refresh,
            )
            is OpportunityUiState.Success -> SkyCastNavHost(
                result = state.result,
                onRetry = viewModel::refresh,
                onOpenSource = { url -> openSourceUrl(context, url) },
                navController = navController,
            )
        }
    }
}

@Composable
internal fun SkyCastNavHost(
    result: OpportunityResult,
    onRetry: () -> Unit,
    onOpenSource: (String) -> Unit,
    navController: NavHostController = rememberNavController(),
) {
    fun returnToList() {
        if (!navController.popBackStack()) {
            navController.navigate(OpportunityListRoute) {
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = OpportunityListRoute,
    ) {
        composable<OpportunityListRoute> {
            OpportunityListScreen(
                result = result,
                onRetry = onRetry,
                onOpportunityClick = { sceneId ->
                    navController.navigate(OpportunityDetailRoute(sceneId))
                },
            )
        }
        composable<OpportunityDetailRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<OpportunityDetailRoute>()
            OpportunityDetailScreen(
                result = result,
                opportunity = findOpportunity(result, route.sceneId),
                onBack = ::returnToList,
                onRetry = onRetry,
                onOpenSource = onOpenSource,
            )
        }
    }
}

internal fun findOpportunity(
    result: OpportunityResult,
    sceneId: String,
) = result.response.opportunities.firstOrNull { it.sceneId == sceneId }

internal fun openSourceUrl(context: Context, value: String): Boolean {
    val url = validHttpUrl(value) ?: return false
    return runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        true
    }.getOrDefault(false)
}
