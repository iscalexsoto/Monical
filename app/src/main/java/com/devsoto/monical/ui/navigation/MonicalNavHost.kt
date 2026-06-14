package com.devsoto.monical.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.devsoto.monical.ui.capture.CaptureScreen
import com.devsoto.monical.ui.review.ReviewScreen
import com.devsoto.monical.ui.scan.ScanPhase
import com.devsoto.monical.ui.scan.ScanViewModel

object Routes {
    const val CAPTURE = "capture"
    const val REVIEW = "review"
    const val SAVED = "saved"
}

/**
 * Single-activity nav graph for the scan flow. Navigation is driven by the shared
 * [ScanViewModel]'s [ScanPhase] so the capture and review screens stay in sync with the
 * underlying flow state.
 */
@Composable
fun MonicalNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    viewModel: ScanViewModel = viewModel(factory = ScanViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Translate flow phase -> destination, keeping a single source of truth.
    LaunchedEffect(uiState.phase) {
        val target = when (uiState.phase) {
            ScanPhase.CAPTURE, ScanPhase.PROCESSING -> Routes.CAPTURE
            ScanPhase.REVIEW -> Routes.REVIEW
            ScanPhase.SAVED -> Routes.SAVED
        }
        if (navController.currentDestination?.route != target) {
            navController.navigate(target) {
                popUpTo(Routes.CAPTURE) { inclusive = target == Routes.CAPTURE }
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.CAPTURE,
        modifier = modifier,
    ) {
        composable(Routes.CAPTURE) { CaptureScreen(viewModel = viewModel) }
        composable(Routes.REVIEW) { ReviewScreen(viewModel = viewModel) }
        composable(Routes.SAVED) { SavedScreen(onScanAnother = viewModel::reset) }
    }
}

@Composable
private fun SavedScreen(onScanAnother: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Text(
            text = "¡Recibo guardado!",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onScanAnother) { Text("Escanear otro") }
    }
}
