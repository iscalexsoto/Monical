package com.devsoto.monical.data.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

/**
 * Thin wrapper around ML Kit's on-device Latin text recognizer.
 *
 * Runs fully offline; produces the raw, unstructured text of a receipt image which is then
 * handed to a [com.devsoto.monical.data.parse.ReceiptParser] for structuring.
 */
class MlKitTextRecognizer(private val context: Context) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Extracts the raw text from the image at [uri].
     *
     * @throws java.io.IOException if the image cannot be read.
     */
    suspend fun recognize(uri: Uri): String {
        val image = InputImage.fromFilePath(context, uri)
        val result = recognizer.process(image).await()
        return result.text
    }
}
