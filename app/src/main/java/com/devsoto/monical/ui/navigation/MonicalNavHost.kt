package com.devsoto.monical.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.devsoto.monical.ui.home.HomeScreen
import com.devsoto.monical.ui.home.HomeViewModel
import com.devsoto.monical.ui.review.ReviewScreen
import com.devsoto.monical.ui.scan.ScanPhase
import com.devsoto.monical.ui.scan.ScanScreen
import com.devsoto.monical.ui.scan.ScanViewModel
import com.devsoto.monical.ui.settings.SettingsScreen
import com.devsoto.monical.ui.settings.SettingsViewModel

object Routes {
    const val HOME = "home"
    const val CAPTURE = "capture"
    const val REVIEW = "review"
    const val SETTINGS = "settings"
}

/**
 * Single-activity nav graph. Home (the receipt list) is the start; the scan/review sub-flow is
 * entered from the FAB and driven by the shared [ScanViewModel]'s [ScanPhase].
 */
@Composable
fun MonicalNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    scanViewModel: ScanViewModel = viewModel(factory = ScanViewModel.Factory),
) {
    val uiState by scanViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.phase) {
        val target = when (uiState.phase) {
            ScanPhase.IDLE -> Routes.HOME
            ScanPhase.CAPTURE, ScanPhase.PROCESSING -> Routes.CAPTURE
            ScanPhase.REVIEW -> Routes.REVIEW
        }
        if (navController.currentDestination?.route != target) {
            navController.navigate(target) {
                popUpTo(Routes.HOME) { inclusive = target == Routes.HOME }
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier,
    ) {
        composable(Routes.HOME) {
            val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
            HomeScreen(
                viewModel = homeViewModel,
                onImageCaptured = scanViewModel::processImage,
                onManual = scanViewModel::startManual,
                onOpenReceipt = { id, archived -> scanViewModel.editReceipt(id, archived) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.CAPTURE) { ScanScreen(viewModel = scanViewModel) }
        composable(Routes.REVIEW) { ReviewScreen(viewModel = scanViewModel) }
        composable(Routes.SETTINGS) {
            val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
            SettingsScreen(viewModel = settingsViewModel, onBack = { navController.popBackStack() })
        }
    }
}
