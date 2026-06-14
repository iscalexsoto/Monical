package com.devsoto.monical.ui.capture

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Creates a temporary file under `cacheDir/images` (declared in `res/xml/file_paths.xml`) and
 * returns a content [Uri] for it via the app's [FileProvider]. The camera writes the captured
 * photo to this Uri.
 */
fun createImageUri(context: Context): Uri {
    val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
    val file = File.createTempFile("receipt_", ".jpg", imagesDir)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
