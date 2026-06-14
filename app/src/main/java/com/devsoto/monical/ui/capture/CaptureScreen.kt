package com.devsoto.monical.ui.capture

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devsoto.monical.ui.scan.ScanPhase
import com.devsoto.monical.ui.scan.ScanViewModel

/**
 * Entry screen: lets the user capture a receipt with the camera or pick one from the gallery,
 * then hands the image to [ScanViewModel.processImage] for OCR + parsing.
 */
@Composable
fun CaptureScreen(
    viewModel: ScanViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isProcessing = uiState.phase == ScanPhase.PROCESSING

    // Holds the FileProvider Uri the camera will write to, set just before launching.
    val pendingCameraUri = remember { arrayOfNulls<Uri>(1) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri[0]
        if (success && uri != null) viewModel.processImage(uri)
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.processImage(uri)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Escanea un recibo",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Toma una foto del ticket o elige una imagen de tu galería.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))

        if (isProcessing) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text("Analizando recibo…", style = MaterialTheme.typography.bodyMedium)
        } else {
            Button(
                onClick = {
                    val uri = createImageUri(context)
                    pendingCameraUri[0] = uri
                    cameraLauncher.launch(uri)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Tomar foto")
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Elegir de la galería")
            }
        }

        uiState.error?.let { error ->
            Spacer(Modifier.height(24.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}
